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
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.RobotBase;
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
    private double distanceToHub = 0.0;              // Distance to scoring hub (meters)
    private boolean inHubRange = false;              // Within scoring range

    // =====================================================================══
    // VISION REJECTION TRACKING - Debug why poses are filtered in DriveSubsystem
    // =====================================================================══
    private int rejectedGyroRateCount = 0;        // Spinning too fast
    private int rejectedStaleCount = 0;           // Measurement too old
    private int rejectedSingleTagDistCount = 0;   // Single-tag too far from odometry
    private int acceptedMultiTagCount = 0;        // Multi-tag accepted
    private int acceptedSingleTagCount = 0;       // Single-tag accepted

    // =====================================================================══
    // PPS (Poses Per Second) TRACKING - Measure vision throughput
    // Uses exponential moving average for smooth display (no sawtooth!)
    // =====================================================================══
    private static final double PPS_SMOOTHING_FACTOR = 0.1; // Lower = smoother, higher = more responsive
    private double smoothedReceivedPPS = 0;       // Exponential moving average of received PPS
    private double smoothedAcceptedPPS = 0;       // Exponential moving average of accepted PPS
    private double lastPPSUpdateTime = 0;         // Last time we updated PPS calculation
    private int posesReceivedSinceLastUpdate = 0; // Accumulator between updates
    private int posesAcceptedSinceLastUpdate = 0; // Accumulator between updates

    // =====================================================================══
    // SIMULATION SUPPORT - Encapsulated in SimDrivetrain for clean separation
    // =====================================================================══
    private SimDrivetrain simDrivetrain = null;   // Only created in simulation

    // =====================================================================══
    // PRE-ALLOCATED ARRAYS - Prevents GC pressure from repeated allocations
    // =====================================================================══
    private final SwerveModuleState[] measuredStates = new SwerveModuleState[4];
    private final SwerveModulePosition[] modulePositionsReal = new SwerveModulePosition[4];
    private final SwerveModulePosition[] modulePositionsSim = new SwerveModulePosition[] {
        new SwerveModulePosition(0, new Rotation2d()),
        new SwerveModulePosition(0, new Rotation2d()),
        new SwerveModulePosition(0, new Rotation2d()),
        new SwerveModulePosition(0, new Rotation2d())
    };

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

        // Initialize simulation support if not on real robot
        if (!RobotBase.isReal()) {
            simDrivetrain = new SimDrivetrain();

            // Wire up SimVisionProvider to use SimDrivetrain's true pose
            if (vision instanceof SimVisionProvider) {
                ((SimVisionProvider) vision).setTruePoseSupplier(simDrivetrain::getTruePose);
            }
        }

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
        // =====================================================================══
        // ADVANTAGEKIT LOGGING - Primary telemetry for AdvantageScope replay
        // =====================================================================══
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
        // =====================================================================══

        // --- POSE DATA (Primary for AdvantageScope 2D/3D Field) ---
        // In simulation, we have 3 distinct poses to visualize:
        //   - Robot (fused): What the robot "believes" - drifts, then corrected by vision
        //   - OdometryOnly: Pure odometry - drifts continuously, never corrected
        //   - TruePose: Ground truth - where robot ACTUALLY is (vision reports this)
        //   - VisionPose: Latest vision estimate (should match TruePose when tags visible)
        Logger.recordOutput("Odometry/Robot", getCurrentPose());
        if (simDrivetrain != null) {
            Logger.recordOutput("Odometry/OdometryOnly", simDrivetrain.getOdometryPose());
            Logger.recordOutput("Odometry/TruePose", simDrivetrain.getTruePose());
        } else {
            Logger.recordOutput("Odometry/OdometryOnly", odometry.getPoseMeters());
        }
        Logger.recordOutput("Odometry/VisionPose", latestVisionPose);

        // --- APRILTAG TRACKING (3D poses for field visualization) ---
        // AcceptedTags: Tags that passed filtering and were used for localization
        // RejectedTags: Tags detected but filtered out (ambiguity, distance, etc.)
        Logger.recordOutput("Odometry/AcceptedTags", vision.getAcceptedTagPoses());
        Logger.recordOutput("Odometry/RejectedTags", vision.getRejectedTagPoses());

        // --- MODULE STATES (For swerve visualization) --- Reuse pre-allocated array
        measuredStates[0] = frontLeft.getState();
        measuredStates[1] = frontRight.getState();
        measuredStates[2] = rearLeft.getState();
        measuredStates[3] = rearRight.getState();
        Logger.recordOutput("Drive/MeasuredStates", measuredStates);

        // --- CHASSIS SPEEDS (For tuning/analysis) ---
        ChassisSpeeds speeds = getRobotRelativeSpeeds();
        Logger.recordOutput("Drive/MeasuredSpeeds", speeds);

        // --- GYRO (Essential for odometry replay) ---
        Logger.recordOutput("Gyro/YawDeg", gyro.getYaw());

        // --- DISTANCE TO HUB (For drive team scoring awareness) ---
        Translation2d currentTranslation = getCurrentPose().getTranslation();
        Translation2d hubCenter = FieldConstants.getAllianceHubCenter();
        distanceToHub = currentTranslation.getDistance(hubCenter);
        inHubRange = distanceToHub >= FieldConstants.kMinShootingDistance
                     && distanceToHub <= FieldConstants.kMaxShootingDistance;

        Logger.recordOutput("Drive/DistanceToHub", distanceToHub);
        Logger.recordOutput("Drive/InHubRange", inHubRange);
    }

    /**
     * Retrieves poses from the Vision subsystem and fuses them if they pass validity checks.
     */
    private void processVisionMeasurements() {
        // Rejection 1: Spinning too fast causes camera motion blur
        double gyroRate = Math.abs(gyro.getRate());
        if (gyroRate > DriveConstants.kMaxAngularVelocityForVisionDegPerSec) {
            rejectedGyroRateCount++;
            return;
        }

        // Use getCurrentPose() which returns simPose in simulation
        Pose2d currentEstimate = getCurrentPose();
        List<EstimatedRobotPose> visionEstimates = vision.getEstimatedGlobalPoses(currentEstimate);
        double currentTime = Timer.getFPGATimestamp();

        // Count poses RECEIVED from PhotonVision (before any DriveSubsystem filtering)
        posesReceivedSinceLastUpdate += visionEstimates.size();

        // PPS tracking: Update exponential moving average every 0.1 seconds (10 Hz update rate)
        double timeSinceLastUpdate = currentTime - lastPPSUpdateTime;
        if (timeSinceLastUpdate >= 0.1) {
            // Calculate instantaneous PPS based on counts since last update
            double instantReceivedPPS = posesReceivedSinceLastUpdate / timeSinceLastUpdate;
            double instantAcceptedPPS = posesAcceptedSinceLastUpdate / timeSinceLastUpdate;

            // Apply exponential moving average for smooth display
            smoothedReceivedPPS = PPS_SMOOTHING_FACTOR * instantReceivedPPS + (1.0 - PPS_SMOOTHING_FACTOR) * smoothedReceivedPPS;
            smoothedAcceptedPPS = PPS_SMOOTHING_FACTOR * instantAcceptedPPS + (1.0 - PPS_SMOOTHING_FACTOR) * smoothedAcceptedPPS;

            // Reset accumulators
            posesReceivedSinceLastUpdate = 0;
            posesAcceptedSinceLastUpdate = 0;
            lastPPSUpdateTime = currentTime;
        }

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
                rejectedStaleCount++;
                continue;
            }

            Pose2d estPose = est.estimatedPose.toPose2d();

            // Calculate distance between current odometry and vision estimate
            double distanceDiff = currentEstimate.getTranslation().getDistance(estPose.getTranslation());
            boolean multiTag = est.targetsUsed.size() >= 2;

            // Get the calculated trust from VisionSubsystem (Don't overwrite this!)
            Matrix<N3, N1> stdDevs = vision.getEstimationStdDevs(est);

            if (multiTag) {
                // Multi-tag: extremely reliable, even at range
                poseEstimator.addVisionMeasurement(estPose, est.timestampSeconds, stdDevs);
                acceptedMultiTagCount++;
                posesAcceptedSinceLastUpdate++;

                if (simDrivetrain != null) {
                    simDrivetrain.applyVisionCorrection(estPose, 0.8);
                }
            } else {
                // Single Tag: Only accept if close to current odometry estimate
                if (distanceDiff < smallThreshold) {
                    poseEstimator.addVisionMeasurement(estPose, est.timestampSeconds, stdDevs);
                    acceptedSingleTagCount++;
                    posesAcceptedSinceLastUpdate++;

                    if (simDrivetrain != null) {
                        simDrivetrain.applyVisionCorrection(estPose, 0.5);
                    }
                } else {
                    rejectedSingleTagDistCount++;
                }
            }
        }

        // Log summary stats every cycle
        logVisionDebugSummary();
    }

    /**
     * Logs PPS (Poses Per Second) - the key metric for vision throughput.
     */
    private void logVisionDebugSummary() {
        // PPS metrics only - essential for debugging throughput issues
        Logger.recordOutput("Vision/PPS/Received", smoothedReceivedPPS);
        Logger.recordOutput("Vision/PPS/Accepted", smoothedAcceptedPPS);
    }

    /**
     * Drives the robot.
     *
     * @param xSpeed Forward velocity (meters/second). Field-relative if fieldRelative=true.
     * @param ySpeed Sideways velocity (meters/second). Field-relative if fieldRelative=true.
     * @param rot Angular velocity (radians/second).
     * @param fieldRelative True for field-oriented control, false for robot-oriented.
     */
    public void drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative) {
        // Store RAW inputs for simulation (before any conversion)
        if (simDrivetrain != null) {
            simDrivetrain.setInputs(xSpeed, ySpeed, rot, fieldRelative);
        }

        ChassisSpeeds speeds = fieldRelative
            ? ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, getRotation2d())
            : new ChassisSpeeds(xSpeed, ySpeed, rot);

        SwerveModuleState[] states = DriveConstants.kDriveKinematics.toSwerveModuleStates(speeds);
        SwerveDriveKinematics.desaturateWheelSpeeds(states, DriveConstants.kMaxSpeedMetersPerSecond);
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

    /** Returns the fused pose (Odometry + Vision). In simulation, returns simulated pose. */
    public Pose2d getCurrentPose() {
        if (simDrivetrain != null) {
            return simDrivetrain.getPose();
        }
        return poseEstimator.getEstimatedPosition();
    }

    // =====================================================================══════
    // POSE RESET METHODS
    // =====================================================================══════
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
    // =====================================================================══════

    /**
     * Resets all pose tracking to a known field position.
     * Does NOT reset the gyro - use this when you know where the robot is
     * but don't want to change the gyro reference.
     *
     * @param pose The known field position
     */
    public void resetPose(Pose2d pose) {
        // In simulation, also reset the sim pose
        if (simDrivetrain != null) {
            simDrivetrain.resetPose(pose);
        }

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
    }

    /**
     * Resets everything to origin (0, 0) with 0 heading.
     * Also zeros the gyro. Use for testing/calibration.
     */
    public void resetToOrigin() {
        Pose2d origin = new Pose2d();

        // In simulation, reset the sim pose; on real robot, reset gyro
        if (simDrivetrain != null) {
            simDrivetrain.resetPose(origin);
        } else {
            gyro.reset();
        }

        poseEstimator.resetPosition(new Rotation2d(), getModulePositions(), origin);
        odometry.resetPosition(new Rotation2d(), getModulePositions(), origin);
        latestVisionPose = origin;

        Logger.recordOutput("Drive/Reset/Pose", origin);
        Logger.recordOutput("Drive/Reset/Type", "ORIGIN");
        Logger.recordOutput("Drive/Reset/Timestamp", Timer.getFPGATimestamp());
    }

    /**
     * Resets pose to starting position in front of alliance hub.
     * Call this at the start of autonomous to set the known starting position.
     *
     * Starting positions are at optimal shooting distance from the hub:
     * - Blue alliance: In front of blue hub, facing forward (0°)
     * - Red alliance: In front of red hub, facing backward (180°)
     */
    public void resetToAllianceHubStart() {
        Translation2d hubCenter = FieldConstants.getAllianceHubCenter();
        var alliance = DriverStation.getAlliance();

        Pose2d startPose;
        if (alliance.isPresent() && alliance.get() == Alliance.Red) {
            // Red alliance: Start in front of red hub (toward red alliance wall)
            double startX = hubCenter.getX() + FieldConstants.kOptimalShootingDistance;
            startPose = new Pose2d(startX, hubCenter.getY(), Rotation2d.fromDegrees(180));
        } else {
            // Blue alliance: Start in front of blue hub (toward blue alliance wall)
            double startX = hubCenter.getX() - FieldConstants.kOptimalShootingDistance;
            startPose = new Pose2d(startX, hubCenter.getY(), Rotation2d.fromDegrees(0));
        }

        resetPose(startPose);
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
     * Returns the distance from the robot to the alliance hub center.
     *
     * @return distance in meters
     */
    public double getDistanceToHub() {
        return distanceToHub;
    }

    /**
     * Returns true if the robot is within the valid scoring range of the hub.
     *
     * @return true if between min and max shooting distance
     */
    public boolean isInHubRange() {
        return inHubRange;
    }

    /**
     * Returns the robot's rotation.
     * Note: Negated because NavX is CW+ but WPILib requires CCW+.
     * In simulation, returns the simulated gyro angle.
     */
    public Rotation2d getRotation2d() {
        if (simDrivetrain != null) {
            return simDrivetrain.getRotation2d();
        }
        return gyro.isConnected() ? Rotation2d.fromDegrees(-1*gyro.getAngle()) : new Rotation2d();
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
        // In simulation, we use simDrivetrain.getPose() directly via getCurrentPose(),
        // so module positions are not critical. Return pre-allocated zeros to avoid GC.
        if (simDrivetrain != null) {
            return modulePositionsSim;
        }
        // Reuse pre-allocated array to avoid GC pressure
        modulePositionsReal[0] = frontLeft.getPosition();
        modulePositionsReal[1] = frontRight.getPosition();
        modulePositionsReal[2] = rearLeft.getPosition();
        modulePositionsReal[3] = rearRight.getPosition();
        return modulePositionsReal;
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

    // =====================================================================══
    // SIMULATION SUPPORT
    // =====================================================================══

    /**
     * Updates simulation physics. Call this from Robot.simulationPeriodic().
     *
     * <p>Delegates to SimDrivetrain which handles all physics simulation.
     * The SimDrivetrain uses raw drive inputs (stored via setInputs() in drive())
     * to update the simulated robot pose.
     */
    public void simulationPeriodic() {
        if (simDrivetrain != null) {
            simDrivetrain.update();
        }
    }

        public void moveOneWheel() {
        setModuleStates(new SwerveModuleState[] {
            new SwerveModuleState(5, Rotation2d.fromDegrees(0)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(0)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(0)),
            new SwerveModuleState(0, Rotation2d.fromDegrees(0))
        });
    }
}