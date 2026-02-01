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
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.DriveConstants;
import frc.robot.constants.FieldConstants;
import frc.robot.constants.HardwareConstants;
import edu.wpi.first.math.Matrix;

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
        // ADVANTAGEKIT LOGGING - Primary telemetry for AdvantageScope replay
        // ═══════════════════════════════════════════════════════════════════════
        //
        // All data logged here is available in AdvantageScope for post-match analysis.
        // NT4Publisher streams this to NetworkTables for live viewing.
        //
        // ADVANTAGESCOPE POSE SETUP:
        //   1. Open AdvantageScope, connect to robot or load log file
        //   2. Add a "2D Field" or "3D Field" view
        //   3. Drag these onto the field:
        //      - Odometry/Robot (GOLD) - Fused pose (what robot believes)
        //      - Odometry/OdometryOnly (BLUE) - Encoder-only, drifts over time
        //      - Vision/RobotPose (GREEN) - Latest vision estimate
        //
        // ═══════════════════════════════════════════════════════════════════════

        // --- POSE DATA (Primary for AdvantageScope 2D/3D Field) ---
        Logger.recordOutput("Odometry/Robot", getCurrentPose());
        Logger.recordOutput("Odometry/OdometryOnly", odometry.getPoseMeters());
        Logger.recordOutput("Odometry/VisionPose", latestVisionPose);

        // --- APRILTAG TRACKING (3D poses for field visualization) ---
        // AcceptedTags: Tags that passed filtering and were used for localization
        // RejectedTags: Tags detected but filtered out (ambiguity, distance, etc.)
        Logger.recordOutput("Odometry/AcceptedTags", vision.getAcceptedTagPoses());
        Logger.recordOutput("Odometry/RejectedTags", vision.getRejectedTagPoses());

        // --- MODULE STATES (For swerve visualization) ---
        Logger.recordOutput("Drive/MeasuredStates", new SwerveModuleState[] {
            frontLeft.getState(), frontRight.getState(), rearLeft.getState(), rearRight.getState()
        });

        // --- CHASSIS SPEEDS (For tuning/analysis) ---
        ChassisSpeeds speeds = getRobotRelativeSpeeds();
        Logger.recordOutput("Drive/MeasuredSpeeds", speeds);

        // --- GYRO (For odometry debugging) ---
        Logger.recordOutput("Gyro/Connected", gyro.isConnected());
        Logger.recordOutput("Gyro/YawDeg", gyro.getYaw());
        Logger.recordOutput("Gyro/RateDegPerSec", Math.toDegrees(gyro.getRate()));

        // --- HUB DISTANCE (For shooter ranging) ---
        var hubCenter = FieldConstants.getAllianceHubCenter();
        double distanceToHubMeters = getCurrentPose().getTranslation().getDistance(hubCenter);
        boolean inShootingRange = distanceToHubMeters >= FieldConstants.kMinShootingDistance
                               && distanceToHubMeters <= FieldConstants.kMaxShootingDistance;

        Logger.recordOutput("Hub/DistanceMeters", distanceToHubMeters);
        Logger.recordOutput("Hub/InRange", inShootingRange);

        // --- DRIVER SETTINGS ---
        Logger.recordOutput("Drive/FieldRelative", fieldRelative);
        Logger.recordOutput("Drive/SpeedMultiplier", speedMultiplier);
        Logger.recordOutput("Drive/BatteryVoltage", RobotController.getBatteryVoltage());
    }

    /**
     * Retrieves poses from the Vision subsystem and fuses them if they pass validity checks.
     */
    private void processVisionMeasurements() {
        // Rejection 1: Spinning too fast causes camera motion blur
        // (Even with global shutter, processing latency while spinning can cause lag)
        if (Math.abs(gyro.getRate()) > DriveConstants.kMaxAngularVelocityForVisionDegPerSec) {
            return;
        }

        Pose2d currentEstimate = poseEstimator.getEstimatedPosition();
        List<EstimatedRobotPose> visionEstimates = vision.getEstimatedGlobalPoses(currentEstimate);
        double currentTime = Timer.getFPGATimestamp();

        if (!visionEstimates.isEmpty()) {
            lastVisionUpdateTime = currentTime;
            latestVisionPose = visionEstimates.get(0).estimatedPose.toPose2d();
        }

        boolean classroomMode = SmartDashboard.getBoolean("Vision/ClassroomMode", false);
        double smallThreshold = classroomMode
            ? DriveConstants.kVisionSmallCorrectionThreshold_Classroom
            : DriveConstants.kVisionSmallCorrectionThreshold_Competition;

        for (EstimatedRobotPose est : visionEstimates) {
            // Rejection 2: Stale measurement - image is too old to be useful
            double measurementAge = currentTime - est.timestampSeconds;
            if (measurementAge > DriveConstants.kMaxVisionMeasurementAgeSeconds) {
                continue; // Skip this stale frame
            }

            Pose2d estPose = est.estimatedPose.toPose2d();

            // Calculate distance between current odometry and vision estimate
            double distanceDiff = currentEstimate.getTranslation().getDistance(estPose.getTranslation());
            boolean multiTag = est.targetsUsed.size() >= 2;

            // Get the calculated trust from VisionSubsystem (Don't overwrite this!)
            Matrix<N3, N1> stdDevs = vision.getEstimationStdDevs(est);

            if (multiTag) {
                // Multi-tag: extremely reliable, even at range.
                // However, if the pose jumps wildly (e.g. 2 meters), we still treat it with caution
                // unless we are in the "Large Threshold" logic (like a reset).
                poseEstimator.addVisionMeasurement(estPose, est.timestampSeconds, stdDevs);

            } else {
                // Single Tag Logic:
                // Only accept single tag updates if they are relatively close to where we think we are.
                // This prevents "teleporting" to the other side of the field due to a false ID detection.
                if (distanceDiff < smallThreshold) {
                    poseEstimator.addVisionMeasurement(estPose, est.timestampSeconds, stdDevs);
                }
                // If distanceDiff is large > smallThreshold, we ignore single-tag data.
                // It's too risky to let a single tag reset our field position by a large amount.
            }
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

    // ═══════════════════════════════════════════════════════════════════════════
    // POSE RESET METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // All resets sync the three pose trackers (fused, odometry, vision) so they
    // agree in AdvantageScope. Use the appropriate method for your situation:
    //
    //   zeroHeading()      - Robot is facing "forward" (away from alliance wall)
    //   setHeading(angle)  - Robot is facing a known angle on the field
    //   forceVisionReset() - Trust AprilTags completely, snap to vision
    //   resetPose(pose)    - Robot is at a known field position
    //   resetToOrigin()    - Testing/calibration at (0,0,0)
    //
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Resets all pose tracking to a known field position.
     * Does NOT reset the gyro - use this when you know where the robot is
     * but don't want to change the gyro reference.
     *
     * @param pose The known field position
     */
    public void resetPose(Pose2d pose) {
        Rotation2d gyroAngle = getRotation2d();
        poseEstimator.resetPosition(gyroAngle, getModulePositions(), pose);
        odometry.resetPosition(gyroAngle, getModulePositions(), pose);
        latestVisionPose = pose;

        Logger.recordOutput("Drive/Reset/Pose", pose);
        Logger.recordOutput("Drive/Reset/Type", "POSE");
        Logger.recordOutput("Drive/Reset/Timestamp", Timer.getFPGATimestamp());
    }

    /**
     * Snaps all pose tracking to the current vision estimate.
     * Use this when you trust AprilTags more than odometry (e.g., after driving blind).
     * Does NOT reset the gyro.
     *
     * @return true if successful, false if no AprilTags visible
     */
    public boolean forceVisionReset() {
        var visionEstimates = vision.getEstimatedGlobalPoses(getCurrentPose());
        if (visionEstimates.isEmpty()) {
            Logger.recordOutput("Drive/Reset/Type", "VISION_FAILED");
            Logger.recordOutput("Drive/Reset/Timestamp", Timer.getFPGATimestamp());
            DriverStation.reportWarning("Vision reset failed - no AprilTags visible!", false);
            return false;
        }

        Pose2d visionPose = visionEstimates.get(0).estimatedPose.toPose2d();
        int tagCount = visionEstimates.get(0).targetsUsed.size();

        Rotation2d gyroAngle = getRotation2d();
        poseEstimator.resetPosition(gyroAngle, getModulePositions(), visionPose);
        odometry.resetPosition(gyroAngle, getModulePositions(), visionPose);
        latestVisionPose = visionPose;

        Logger.recordOutput("Drive/Reset/Pose", visionPose);
        Logger.recordOutput("Drive/Reset/Type", "VISION");
        Logger.recordOutput("Drive/Reset/TagCount", tagCount);
        Logger.recordOutput("Drive/Reset/Timestamp", Timer.getFPGATimestamp());

        System.out.println(String.format("Vision reset: (%.2f, %.2f, %.1f°) using %d tag(s)",
            visionPose.getX(), visionPose.getY(), visionPose.getRotation().getDegrees(), tagCount));
        return true;
    }

    /**
     * Zeros the gyro and sets heading to 0 degrees.
     * Call this when the robot is facing "forward" (away from your alliance wall).
     * Preserves the current X/Y position.
     */
    public void zeroHeading() {
        setHeading(new Rotation2d()); // 0 degrees
    }

    /**
     * Sets the robot heading to a specific angle.
     * Call this when the robot is facing a known direction on the field.
     * Preserves the current X/Y position.
     *
     * <p>Note: This resets the gyro and uses the pose estimator's internal
     * offset tracking to handle the heading. The gyro will read 0 after this,
     * but the pose will have the correct heading.
     *
     * @param heading The field-relative heading (0 = away from alliance wall)
     */
    public void setHeading(Rotation2d heading) {
        gyro.reset(); // Gyro now reads 0

        // Update all poses with new heading, keep X/Y
        Pose2d currentPose = getCurrentPose();
        Pose2d newPose = new Pose2d(currentPose.getTranslation(), heading);

        // Tell the estimators: "gyro reads 0, but we want heading to be X"
        // They will internally track the offset
        Rotation2d gyroAngle = new Rotation2d(); // 0 after reset
        poseEstimator.resetPosition(gyroAngle, getModulePositions(), newPose);
        odometry.resetPosition(gyroAngle, getModulePositions(), newPose);
        latestVisionPose = new Pose2d(latestVisionPose.getTranslation(), heading);

        Logger.recordOutput("Drive/Reset/Pose", newPose);
        Logger.recordOutput("Drive/Reset/Type", "HEADING");
        Logger.recordOutput("Drive/Reset/Heading", heading.getDegrees());
        Logger.recordOutput("Drive/Reset/Timestamp", Timer.getFPGATimestamp());

        System.out.println(String.format("Heading reset to %.1f°", heading.getDegrees()));
    }

    /**
     * Resets everything to origin (0, 0) with 0 heading.
     * Also zeros the gyro. Use for testing/calibration.
     */
    public void resetToOrigin() {
        gyro.reset();
        Pose2d origin = new Pose2d();

        poseEstimator.resetPosition(new Rotation2d(), getModulePositions(), origin);
        odometry.resetPosition(new Rotation2d(), getModulePositions(), origin);
        latestVisionPose = origin;

        Logger.recordOutput("Drive/Reset/Pose", origin);
        Logger.recordOutput("Drive/Reset/Type", "ORIGIN");
        Logger.recordOutput("Drive/Reset/Timestamp", Timer.getFPGATimestamp());

        System.out.println("Reset to origin (0, 0, 0°)");
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
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setFieldRelative(boolean fieldRelative) {
        this.fieldRelative = fieldRelative;
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