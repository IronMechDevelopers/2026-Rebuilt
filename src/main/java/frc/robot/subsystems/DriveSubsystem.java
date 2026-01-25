package frc.robot.subsystems;

import java.util.List;

import org.littletonrobotics.junction.Logger;
import org.photonvision.EstimatedRobotPose;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.studica.frc.AHRS;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.DriveConstants;
import frc.robot.constants.FieldConstants;
import frc.robot.constants.HardwareConstants;

/**
 * Controls the swerve drivetrain and handles pose estimation (Odometry + Vision).
 */
public class DriveSubsystem extends SubsystemBase {

    // Hardware
    private final MAXSwerveModule frontLeft = new MAXSwerveModule(
        HardwareConstants.kFrontLeftDrivingCanId,
        HardwareConstants.kFrontLeftTurningCanId,
        HardwareConstants.kFrontLeftChassisAngularOffset);

    private final MAXSwerveModule frontRight = new MAXSwerveModule(
        HardwareConstants.kFrontRightDrivingCanId,
        HardwareConstants.kFrontRightTurningCanId,
        HardwareConstants.kFrontRightChassisAngularOffset);

    private final MAXSwerveModule rearLeft = new MAXSwerveModule(
        HardwareConstants.kRearLeftDrivingCanId,
        HardwareConstants.kRearLeftTurningCanId,
        HardwareConstants.kBackLeftChassisAngularOffset);

    private final MAXSwerveModule rearRight = new MAXSwerveModule(
        HardwareConstants.kRearRightDrivingCanId,
        HardwareConstants.kRearRightTurningCanId,
        HardwareConstants.kBackRightChassisAngularOffset);

    private final AHRS gyro = new AHRS(AHRS.NavXComType.kMXP_SPI);

    // Pose Tracking
    private final SwerveDrivePoseEstimator poseEstimator;
    private final SwerveDriveOdometry odometry;
    private final VisionProvider vision;

    // State
    private double speedMultiplier = DriveConstants.kDefaultSpeedMultiplier;
    private boolean fieldRelative = true;
    private boolean isBrownedOut = false;
    private double lastVisionUpdateTime = 0.0;
    private Pose2d latestVisionPose = new Pose2d();  // Latest raw vision estimate (for logging)

    /**
     * @param vision The vision provider implementation (Real or Simulation).
     */
    public DriveSubsystem(VisionProvider vision) {
        this.vision = vision;

        // Initialize Pose Estimator (Fusion)
        poseEstimator = new SwerveDrivePoseEstimator(
            DriveConstants.kDriveKinematics,
            getRotation2d(),
            getModulePositions(),
            new Pose2d(),
            // Standard Devs (State Trust): [x, y, theta]
            VecBuilder.fill(DriveConstants.kStateStdDevX, DriveConstants.kStateStdDevY, DriveConstants.kStateStdDevTheta),
            // Standard Devs (Vision Trust): overwritten per frame
            VecBuilder.fill(DriveConstants.kVisionStdDevX, DriveConstants.kVisionStdDevY, DriveConstants.kVisionStdDevTheta)
        );

        // Initialize Odometry (Backup/Pure Encoders)
        odometry = new SwerveDriveOdometry(
            DriveConstants.kDriveKinematics,
            getRotation2d(),
            getModulePositions()
        );

        configurePathPlanner();
    }

    @Override
    public void periodic() {
        checkBrownout();

        // 1. Update Physics (Encoders + Gyro)
        Rotation2d gyroAngle = getRotation2d();
        SwerveModulePosition[] modulePositions = getModulePositions();

        poseEstimator.update(gyroAngle, modulePositions);
        odometry.update(gyroAngle, modulePositions);

        // 2. Add Vision (if valid)
        processVisionMeasurements();

        // 3. Telemetry
        updateDashboard();
    }

    /**
     * Checks battery voltage and logs a warning if below critical threshold.
     */
    private void checkBrownout() {
        double voltage = RobotController.getBatteryVoltage();
        if (voltage < DriveConstants.kBrownoutVoltageThreshold) {
            if (!isBrownedOut) {
                DriverStation.reportWarning("Low voltage detected: " + String.format("%.1f", voltage) + "V", false);
                isBrownedOut = true;
            }
        } else {
            isBrownedOut = false;
        }
    }

    private void updateDashboard() {
        // ═══════════════════════════════════════════════════════════════════════
        // POSE COMPARISON LOGGING - For AdvantageScope calibration & debugging
        // ═══════════════════════════════════════════════════════════════════════
        //
        // ADVANTAGESCOPE SETUP:
        //   1. Open AdvantageScope, connect to robot or load log file
        //   2. Add a "2D Field" or "3D Field" view
        //   3. Drag these onto the field (all under "Poses/" folder):
        //      - Poses/Fused        (GOLD)   - What the robot "believes" (vision + odometry combined)
        //      - Poses/OdometryOnly (BLUE)   - Raw wheel encoder math, drifts over time
        //      - Poses/Vision       (GREEN)  - What AprilTags see, only updates when tags visible
        //      - Poses/VisibleTags  (RED)    - Field positions of AprilTags currently being detected
        //
        // VISION RAYS SETUP (shows line from robot to each detected tag):
        //   4. Drag Poses/VisionRays/0, /1, /2, /3 onto the field
        //   5. For each ray: click the colored icon -> change type to "Trajectory"
        //   6. You'll see lines from the robot to each AprilTag it's detecting!
        //
        // WHAT TO LOOK FOR:
        //   - All three robot poses should start at same position after "Snap to Vision"
        //   - OdometryOnly will drift as you drive (wheel slip, encoder error)
        //   - Fused should track closer to Vision when tags are visible
        //   - If Fused diverges wildly from Vision, check camera calibration
        //   - VisibleTags shows which tags the robot is detecting (verify correct tags!)
        //   - VisionRays show exactly which tags are being used for pose estimation
        //
        // CALIBRATION WORKFLOW:
        //   1. Point robot at AprilTag
        //   2. Press "Snap to Vision" on Vision Cal tab
        //   3. All three poses should align
        //   4. Drive around and watch them diverge/converge
        //   5. Check VisionRays to see which AprilTags are being tracked
        //
        Logger.recordOutput("Poses/Fused", getCurrentPose());
        Logger.recordOutput("Poses/OdometryOnly", odometry.getPoseMeters());
        Logger.recordOutput("Poses/Vision", latestVisionPose);
        Logger.recordOutput("Poses/VisibleTags", vision.getVisibleTagPoses());

        // Log vision rays (lines from robot to each visible tag)
        // In AdvantageScope, set these to "Trajectory" type to see the rays
        Pose2d[] visibleTags = vision.getVisibleTagPoses();
        Pose2d robotPose = getCurrentPose();
        for (int i = 0; i < visibleTags.length && i < 4; i++) {
            Logger.recordOutput("Poses/VisionRays/" + i, new Pose2d[] { robotPose, visibleTags[i] });
        }
        // Clear unused ray slots (so old rays don't persist)
        for (int i = visibleTags.length; i < 4; i++) {
            Logger.recordOutput("Poses/VisionRays/" + i, new Pose2d[] {});
        }

        // Legacy paths (for backwards compatibility with existing dashboards)
        Logger.recordOutput("Drive/Pose", getCurrentPose());
        Logger.recordOutput("Drive/OdometryPose", odometry.getPoseMeters());

        // ═══════════════════════════════════════════════════════════════════════
        // OTHER DRIVE TELEMETRY
        // ═══════════════════════════════════════════════════════════════════════

        // Log Module States (measured)
        Logger.recordOutput("Drive/MeasuredStates", new SwerveModuleState[] {
            frontLeft.getState(), frontRight.getState(), rearLeft.getState(), rearRight.getState()
        });

        // Log Velocities (measured)
        ChassisSpeeds speeds = getRobotRelativeSpeeds();
        Logger.recordOutput("Drive/MeasuredSpeeds/vx", speeds.vxMetersPerSecond);
        Logger.recordOutput("Drive/MeasuredSpeeds/vy", speeds.vyMetersPerSecond);
        Logger.recordOutput("Drive/MeasuredSpeeds/omega", speeds.omegaRadiansPerSecond);
        Logger.recordOutput("Drive/Heading", getHeading());

        // ═══════════════════════════════════════════════════════════════════════
        // GYRO DATA - Critical for odometry debugging
        // ═══════════════════════════════════════════════════════════════════════
        //
        // Use this to debug odometry drift:
        //   - If GyroConnected is false, odometry will be wrong
        //   - Compare GyroAngle to Poses/Fused heading to check calibration
        //   - High GyroRate during vision updates may cause rejection
        //
        Logger.recordOutput("Gyro/Connected", gyro.isConnected());
        Logger.recordOutput("Gyro/AngleDeg", gyro.getAngle());
        Logger.recordOutput("Gyro/YawDeg", gyro.getYaw());
        Logger.recordOutput("Gyro/PitchDeg", gyro.getPitch());
        Logger.recordOutput("Gyro/RollDeg", gyro.getRoll());
        Logger.recordOutput("Gyro/RateDegPerSec", Math.toDegrees(gyro.getRate()));

        // ═══════════════════════════════════════════════════════════════════════
        // HUB DISTANCE - Always logged so drivers know when to spin up shooter
        // ═══════════════════════════════════════════════════════════════════════
        var hubCenter = FieldConstants.getAllianceHubCenter();
        double distanceToHubMeters = getCurrentPose().getTranslation().getDistance(hubCenter);
        double distanceToHubInches = Units.metersToInches(distanceToHubMeters);

        // Check if in shooting range
        boolean inShootingRange = distanceToHubMeters >= FieldConstants.kMinShootingDistance
                               && distanceToHubMeters <= FieldConstants.kMaxShootingDistance;

        // Log to SmartDashboard (for Shuffleboard display)
        SmartDashboard.putNumber("Hub/DistanceInches", distanceToHubInches);
        SmartDashboard.putBoolean("Hub/InRange", inShootingRange);

        // Log to AdvantageKit (for replay/analysis)
        Logger.recordOutput("Hub/DistanceInches", distanceToHubInches);
        Logger.recordOutput("Hub/DistanceMeters", distanceToHubMeters);
        Logger.recordOutput("Hub/InRange", inShootingRange);

        // Driver Feedback
        SmartDashboard.putBoolean("Drive/FieldRelative", fieldRelative);
        SmartDashboard.putNumber("Drive/SpeedMultiplier", speedMultiplier);
        Logger.recordOutput("Drive/BatteryVoltage", RobotController.getBatteryVoltage());
        Logger.recordOutput("Drive/FieldRelative", fieldRelative);
        Logger.recordOutput("Drive/SpeedMultiplier", speedMultiplier);
    }

    /**
     * Retrieves poses from the Vision subsystem and fuses them if they pass validity checks.
     */
    private void processVisionMeasurements() {
        // Rejection 1: Spinning too fast causes camera motion blur
        if (Math.abs(gyro.getRate()) > DriveConstants.kMaxAngularVelocityForVisionDegPerSec) {
            return;
        }

        Pose2d currentEstimate = poseEstimator.getEstimatedPosition();
        List<EstimatedRobotPose> visionEstimates = vision.getEstimatedGlobalPoses(currentEstimate);

        if (!visionEstimates.isEmpty()) {
            lastVisionUpdateTime = edu.wpi.first.wpilibj.Timer.getFPGATimestamp();
            // Store latest vision pose for logging/comparison
            latestVisionPose = visionEstimates.get(0).estimatedPose.toPose2d();
        }

        boolean classroomMode = SmartDashboard.getBoolean("Vision/ClassroomMode", false);
        double largeThreshold = classroomMode 
            ? DriveConstants.kVisionLargeCorrectionThreshold_Classroom 
            : DriveConstants.kVisionLargeCorrectionThreshold_Competition;
        double smallThreshold = classroomMode 
            ? DriveConstants.kVisionSmallCorrectionThreshold_Classroom 
            : DriveConstants.kVisionSmallCorrectionThreshold_Competition;

        for (EstimatedRobotPose est : visionEstimates) {
            Pose2d estPose = est.estimatedPose.toPose2d();
            double distanceDiff = currentEstimate.getTranslation().getDistance(estPose.getTranslation());
            boolean multiTag = est.targetsUsed.size() >= 2;

            if (multiTag) {
                // Multi-tag is high confidence. If error is large, snap quickly (high trust/low stdDev).
                var stdDevs = vision.getEstimationStdDevs(est);
                if (distanceDiff > largeThreshold) {
                    stdDevs = VecBuilder.fill(
                        DriveConstants.kVisionHighTrustStdDevXY, 
                        DriveConstants.kVisionHighTrustStdDevXY, 
                        DriveConstants.kVisionHighTrustStdDevTheta);
                }
                poseEstimator.addVisionMeasurement(estPose, est.timestampSeconds, stdDevs);
                
            } else if (distanceDiff < smallThreshold) {
                // Single tag is ok if it aligns with current odometry.
                poseEstimator.addVisionMeasurement(estPose, est.timestampSeconds, vision.getEstimationStdDevs(est));
            }
            // else: Single tag with large error is likely a false positive/reflection. Ignore.
        }
    }

    /**
     * Drives the robot.
     *
     * @param xSpeed Forward velocity (meters/second).
     * @param ySpeed Sideways velocity (meters/second).
     * @param rot Angular velocity (radians/second).
     * @param fieldRelative True for field-oriented control, false for robot-oriented.
     */
    public void drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative) {
        ChassisSpeeds speeds = fieldRelative
            ? ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, getRotation2d())
            : new ChassisSpeeds(xSpeed, ySpeed, rot);

        SwerveModuleState[] states = DriveConstants.kDriveKinematics.toSwerveModuleStates(speeds);
        SwerveDriveKinematics.desaturateWheelSpeeds(states, DriveConstants.kMaxSpeedMetersPerSecond);

        // Log desired states for replay comparison (what we're commanding vs what happened)
        Logger.recordOutput("Drive/DesiredSpeeds/vx", speeds.vxMetersPerSecond);
        Logger.recordOutput("Drive/DesiredSpeeds/vy", speeds.vyMetersPerSecond);
        Logger.recordOutput("Drive/DesiredSpeeds/omega", speeds.omegaRadiansPerSecond);
        Logger.recordOutput("Drive/DesiredStates", states);

        setModuleStates(states);
    }

    /**
     * Applies states to modules after desaturating speeds.
     * @param desiredStates Array of 4 states [FL, FR, RL, RR].
     */
    public void setModuleStates(SwerveModuleState[] desiredStates) {
        SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, DriveConstants.kMaxSpeedMetersPerSecond);
        frontLeft.setDesiredState(desiredStates[0]);
        frontRight.setDesiredState(desiredStates[1]);
        rearLeft.setDesiredState(desiredStates[2]);
        rearRight.setDesiredState(desiredStates[3]);
    }

    /** Set to true for hold-position (matches), false for rolling (pit). */
    public void setMotorBrake(boolean shouldBrake) {
        frontLeft.setBrakeMode(shouldBrake);
        frontRight.setBrakeMode(shouldBrake);
        rearLeft.setBrakeMode(shouldBrake);
        rearRight.setBrakeMode(shouldBrake);
    }

    /** Returns the fused pose (Odometry + Vision). */
    public Pose2d getCurrentPose() {
        return poseEstimator.getEstimatedPosition();
    }

    /** Resets the pose estimator, odometry, and vision reference to a known location. */
    public void resetPose(Pose2d pose) {
        poseEstimator.resetPosition(getRotation2d(), getModulePositions(), pose);
        odometry.resetPosition(getRotation2d(), getModulePositions(), pose);
        latestVisionPose = pose;  // Sync all three poses for AdvantageScope comparison
    }

    /**
     * Attempts to snap the robot pose to the current vision estimate.
     * @return true if a tag was visible and reset occurred.
     */
    public boolean forceVisionReset() {
        var visionEstimates = vision.getEstimatedGlobalPoses(getCurrentPose());
        if (!visionEstimates.isEmpty()) {
            Pose2d visionPose = visionEstimates.get(0).estimatedPose.toPose2d();
            resetPose(visionPose);
            Logger.recordOutput("Drive/VisionResetPose", visionPose);
            Logger.recordOutput("Drive/VisionResetTimestamp", Timer.getFPGATimestamp());
            SmartDashboard.putBoolean("Vision/ResetSucceeded", true);
            SmartDashboard.putString("Vision/LastResetStatus",
                String.format("Reset to (%.2f, %.2f, %.1f°)",
                    visionPose.getX(), visionPose.getY(), visionPose.getRotation().getDegrees()));
            return true;
        }
        SmartDashboard.putBoolean("Vision/ResetSucceeded", false);
        SmartDashboard.putString("Vision/LastResetStatus", "FAILED - No tags visible!");
        return false;
    }

    /**
     * Resets odometry to the origin (0, 0) with 0 heading.
     * Use this for calibration or when starting from a known location.
     */
    public void resetToOrigin() {
        resetPose(new Pose2d());
    }

    /**
     * Returns true if vision is working and can be trusted.
     *
     * <p><b>Use this before running vision-dependent commands!</b>
     * If vision is unhealthy, commands should fall back to manual control.
     *
     * @return true if vision is healthy, false if disabled/broken/stale
     */
    public boolean isVisionHealthy() {
        return vision.isVisionHealthy();
    }

    /**
     * Returns the robot's rotation.
     * Note: Negated because NavX is CW+ but WPILib requires CCW+.
     */
    public Rotation2d getRotation2d() {
        return gyro.isConnected() ? Rotation2d.fromDegrees(-gyro.getAngle()) : new Rotation2d();
    }

    /** Returns current heading in degrees (-inf to +inf). */
    public double getHeading() {
        return getRotation2d().getDegrees();
    }
    
    /** Returns gyro rate in degrees per second. */
    public double getGyroRate() {
        return Math.toDegrees(gyro.getRate());
    }

    /** Resets the gyro to 0 heading. Preserves X/Y position. */
    public void zeroHeading() {
        gyro.reset();
        Pose2d current = getCurrentPose();
        resetPose(new Pose2d(current.getTranslation(), new Rotation2d()));
    }
    
    /** Calculates straight-line distance to a target pose. */
    public double getDistanceToTarget(Pose2d target) {
        return getCurrentPose().getTranslation().getDistance(target.getTranslation());
    }

    public SwerveModulePosition[] getModulePositions() {
        return new SwerveModulePosition[] {
            frontLeft.getPosition(), frontRight.getPosition(),
            rearLeft.getPosition(), rearRight.getPosition()
        };
    }

    /** Returns current speeds relative to the robot frame. */
    public ChassisSpeeds getRobotRelativeSpeeds() {
        return DriveConstants.kDriveKinematics.toChassisSpeeds(
            frontLeft.getState(), frontRight.getState(),
            rearLeft.getState(), rearRight.getState()
        );
    }

    /**
     * Sets the speed multiplier (0.0 to 1.0).
     */
    public void setSpeedMultiplier(double multiplier) {
        speedMultiplier = edu.wpi.first.math.MathUtil.clamp(multiplier, 0.0, 1.0);
        SmartDashboard.putNumber("Drive/SpeedMultiplier", speedMultiplier);
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setFieldRelative(boolean fieldRelative) {
        this.fieldRelative = fieldRelative;
        SmartDashboard.putBoolean("Drive/FieldRelative", this.fieldRelative);
    }

    public boolean getFieldRelative() {
        return fieldRelative;
    }

    /** Sets wheels to X formation to prevent pushing. */
    public void setX() {
        setModuleStates(new SwerveModuleState[] {
            new SwerveModuleState(0, Rotation2d.fromDegrees(DriveConstants.kXStanceAngleDegrees)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(-DriveConstants.kXStanceAngleDegrees)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(-DriveConstants.kXStanceAngleDegrees)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(DriveConstants.kXStanceAngleDegrees))
        });
    }

    /** Points all wheels forward (0 deg). Useful for alignment checks. */
    public void setStraightAhead() {
        setModuleStates(new SwerveModuleState[] {
            new SwerveModuleState(0, Rotation2d.fromDegrees(0)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(0)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(0)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(0))
        });
    }

    /** Configures AutoBuilder for PathPlanner. */
    private void configurePathPlanner() {
        RobotConfig config;
        try {
            config = RobotConfig.fromGUISettings();
        } catch (Exception e) {
            DriverStation.reportError("PathPlanner config failed: " + e.getMessage(), true);
            return;
        }

        AutoBuilder.configure(
            this::getCurrentPose,
            this::resetPose,
            this::getRobotRelativeSpeeds,
            // Output: PathPlanner speeds are robot-relative.
            (speeds, feedforwards) -> drive(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond, speeds.omegaRadiansPerSecond, false),
            new PPHolonomicDriveController(DriveConstants.kTranslationPID, DriveConstants.kRotationPID),
            config,
            // Flip path if on Red Alliance
            () -> {
                var alliance = DriverStation.getAlliance();
                return alliance.isPresent() && alliance.get() == Alliance.Red;
            },
            this
        );
    }
}