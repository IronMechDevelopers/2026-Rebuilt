package frc.robot.subsystems;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.Timer;

/**
 * Simulates drivetrain physics for testing without hardware.
 *
 * <p>This class handles all simulation-specific state and logic, keeping
 * the DriveSubsystem clean and focused on real robot behavior.
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li>True pose tracking (what vision sees)</li>
 *   <li>Simulated odometry drift (accumulating error over time)</li>
 *   <li>Allows observing vision corrections in AdvantageScope</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 * // In DriveSubsystem constructor:
 * if (!RobotBase.isReal()) {
 *     simDrivetrain = new SimDrivetrain();
 * }
 *
 * // In drive():
 * if (simDrivetrain != null) {
 *     simDrivetrain.setInputs(xSpeed, ySpeed, rot, fieldRelative);
 * }
 *
 * // In simulationPeriodic():
 * if (simDrivetrain != null) {
 *     simDrivetrain.update();
 * }
 *
 * // In getCurrentPose() (fused with vision):
 * if (simDrivetrain != null) {
 *     return simDrivetrain.getPose();
 * }
 *
 * // In getOdometryOnlyPose() (for comparison):
 * if (simDrivetrain != null) {
 *     return simDrivetrain.getOdometryPose();
 * }
 * </pre>
 */
public class SimDrivetrain {

    // =====================================================================══
    // ODOMETRY DRIFT CONFIGURATION - Simulates real-world encoder/gyro error
    // =====================================================================══
    //
    // Real odometry errors come from:
    //   1. Wheel diameter calibration error (consistent scale factor)
    //   2. Wheel slip on carpet/hard floor (loses distance)
    //   3. Gyro drift (slow accumulating heading error)
    //
    // This creates realistic "falling behind" behavior instead of jitter.
    // =====================================================================══

    /**
     * Scale factor for odometry distance (1.0 = perfect, 0.97 = 3% under-reporting).
     * Simulates wheel diameter calibration error - very common in real robots.
     * Odometry will consistently fall behind the true pose.
     */
    private static final double ODOMETRY_SCALE_FACTOR = 0.97;  // 3% under-reports distance

    /**
     * Gyro drift rate in degrees per second.
     * Real gyros drift slowly over time. NavX is good (~0.5°/min) but not perfect.
     */
    private static final double GYRO_DRIFT_DEG_PER_SEC = 0.1;  // 6°/minute drift

    /**
     * Probability of a "slip" event per cycle when moving.
     * Slip causes odometry to miss some distance (like wheels spinning on carpet).
     */
    private static final double SLIP_PROBABILITY = 0.02;  // 2% chance per cycle

    /**
     * How much distance is lost during a slip (0.0 to 1.0).
     * 0.3 = loses 30% of that cycle's movement.
     */
    private static final double SLIP_LOSS_FACTOR = 0.5;  // Loses 50% during slip

    // =====================================================================══
    // SIMULATED STATE
    // =====================================================================══

    // True pose (what vision sees, "ground truth")
    private Pose2d truePose = new Pose2d();
    private double trueGyroAngleDeg = 0;

    // Odometry pose (accumulates drift over time, like real encoders)
    private Pose2d odometryPose = new Pose2d();
    private double odometryGyroAngleDeg = 0;

    // Fused pose (odometry corrected by vision - what robot "believes")
    private Pose2d fusedPose = new Pose2d();
    private double fusedGyroAngleDeg = 0;

    private double lastUpdateTime = 0;

    // Drive inputs (set each cycle by drive())
    private double inputVx = 0;        // X velocity input (m/s)
    private double inputVy = 0;        // Y velocity input (m/s)
    private double inputOmega = 0;     // Angular velocity input (rad/s)
    private boolean inputFieldRelative = true;

    /**
     * Creates a new SimDrivetrain starting at the origin.
     */
    public SimDrivetrain() {
        this(new Pose2d());
    }

    /**
     * Creates a new SimDrivetrain at a specific starting pose.
     *
     * @param initialPose The starting pose
     */
    public SimDrivetrain(Pose2d initialPose) {
        truePose = initialPose;
        trueGyroAngleDeg = initialPose.getRotation().getDegrees();
        odometryPose = initialPose;
        odometryGyroAngleDeg = initialPose.getRotation().getDegrees();
        fusedPose = initialPose;
        fusedGyroAngleDeg = initialPose.getRotation().getDegrees();
        lastUpdateTime = Timer.getFPGATimestamp();
    }

    /**
     * Sets the drive inputs for this cycle. Call this from DriveSubsystem.drive().
     *
     * @param vx X velocity (m/s). Field-relative if fieldRelative=true.
     * @param vy Y velocity (m/s). Field-relative if fieldRelative=true.
     * @param omega Angular velocity (rad/s)
     * @param fieldRelative Whether vx/vy are field-relative
     */
    public void setInputs(double vx, double vy, double omega, boolean fieldRelative) {
        inputVx = vx;
        inputVy = vy;
        inputOmega = omega;
        inputFieldRelative = fieldRelative;
    }

    /**
     * Updates the simulation. Call this from Robot.simulationPeriodic().
     */
    public void update() {
        double currentTime = Timer.getFPGATimestamp();
        double dt = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        // Clamp dt to avoid huge jumps
        if (dt > 0.1 || dt <= 0) {
            dt = 0.02;
        }

        // Calculate field-relative velocities using TRUE heading
        double fieldVx, fieldVy;

        if (inputFieldRelative) {
            // Inputs are already field-relative
            fieldVx = inputVx;
            fieldVy = inputVy;
        } else {
            // Convert robot-relative to field-relative using true heading
            double headingRad = Math.toRadians(trueGyroAngleDeg);
            fieldVx = inputVx * Math.cos(headingRad) - inputVy * Math.sin(headingRad);
            fieldVy = inputVx * Math.sin(headingRad) + inputVy * Math.cos(headingRad);
        }

        // =====================================================================══
        // UPDATE TRUE POSE (Ground truth - what vision sees)
        // =====================================================================══
        double trueNewX = truePose.getX() + fieldVx * dt;
        double trueNewY = truePose.getY() + fieldVy * dt;
        double trueNewAngleDeg = trueGyroAngleDeg + Math.toDegrees(inputOmega * dt);

        truePose = new Pose2d(trueNewX, trueNewY, Rotation2d.fromDegrees(trueNewAngleDeg));
        trueGyroAngleDeg = trueNewAngleDeg;

        // =====================================================================══
        // UPDATE ODOMETRY POSE (Simulates realistic encoder/gyro errors)
        // =====================================================================══

        // 1. Apply scale factor error (wheel diameter calibration)
        //    Odometry consistently under-reports distance traveled
        double odomVx = fieldVx * ODOMETRY_SCALE_FACTOR;
        double odomVy = fieldVy * ODOMETRY_SCALE_FACTOR;

        // 2. Check for slip event (wheels spin but don't grip)
        //    Only happens when actually moving
        double speed = Math.sqrt(fieldVx * fieldVx + fieldVy * fieldVy);
        if (speed > 0.1 && Math.random() < SLIP_PROBABILITY) {
            // Slip! Lose some of this cycle's movement
            odomVx *= (1.0 - SLIP_LOSS_FACTOR);
            odomVy *= (1.0 - SLIP_LOSS_FACTOR);
            Logger.recordOutput("Sim/SlipEvent", true);
        } else {
            Logger.recordOutput("Sim/SlipEvent", false);
        }

        // 3. Apply gyro drift (slow, consistent heading error)
        //    This causes odometry to gradually rotate off course
        double gyroDriftThisCycle = GYRO_DRIFT_DEG_PER_SEC * dt;

        // Update odometry pose
        double odomNewX = odometryPose.getX() + odomVx * dt;
        double odomNewY = odometryPose.getY() + odomVy * dt;
        double odomNewAngleDeg = odometryGyroAngleDeg + Math.toDegrees(inputOmega * dt) + gyroDriftThisCycle;

        odometryPose = new Pose2d(odomNewX, odomNewY, Rotation2d.fromDegrees(odomNewAngleDeg));
        odometryGyroAngleDeg = odomNewAngleDeg;

        // =====================================================================══
        // UPDATE FUSED POSE (Drifts like odometry, but vision can correct it)
        // =====================================================================══
        double fusedNewX = fusedPose.getX() + odomVx * dt;
        double fusedNewY = fusedPose.getY() + odomVy * dt;
        double fusedNewAngleDeg = fusedGyroAngleDeg + Math.toDegrees(inputOmega * dt) + gyroDriftThisCycle;

        fusedPose = new Pose2d(fusedNewX, fusedNewY, Rotation2d.fromDegrees(fusedNewAngleDeg));
        fusedGyroAngleDeg = fusedNewAngleDeg;

        // =====================================================================══
        // LOGGING
        // =====================================================================══
        Logger.recordOutput("Sim/TruePose", truePose);
        Logger.recordOutput("Sim/OdometryPose", odometryPose);
        Logger.recordOutput("Sim/FusedPose", fusedPose);
        Logger.recordOutput("Sim/GyroAngleDeg", trueGyroAngleDeg);

        // Drift metrics - watch odometry drift grow, fused should stay closer to true!
        double odomDrift = truePose.getTranslation().getDistance(odometryPose.getTranslation());
        double fusedDrift = truePose.getTranslation().getDistance(fusedPose.getTranslation());
        double headingDrift = trueGyroAngleDeg - odometryGyroAngleDeg;
        Logger.recordOutput("Sim/Drift/OdometryMeters", odomDrift);
        Logger.recordOutput("Sim/Drift/FusedMeters", fusedDrift);
        Logger.recordOutput("Sim/Drift/HeadingDegrees", headingDrift);

        // Raw inputs
        Logger.recordOutput("Sim/FieldVx", fieldVx);
        Logger.recordOutput("Sim/FieldVy", fieldVy);
        Logger.recordOutput("Sim/Omega", inputOmega);
        Logger.recordOutput("Sim/IsFieldRelative", inputFieldRelative);
    }

    /**
     * Gets the current fused pose (odometry corrected by vision).
     * This is what the robot "believes" its position is.
     * Drifts like odometry, but gets corrected when vision sees tags.
     *
     * @return The fused pose (what DriveSubsystem uses)
     */
    public Pose2d getPose() {
        return fusedPose;
    }

    /**
     * Gets the true pose (ground truth).
     * This is where the robot ACTUALLY is - what vision should report.
     * Use this for SimVisionProvider to generate accurate tag-based estimates.
     *
     * @return The true robot pose (ground truth)
     */
    public Pose2d getTruePose() {
        return truePose;
    }

    /**
     * Gets the current odometry pose (with accumulated drift).
     * This simulates what real wheel encoders would report - it drifts over time.
     * Use this for the "Odometry Only" display to show vision corrections.
     *
     * @return The drifted odometry pose
     */
    public Pose2d getOdometryPose() {
        return odometryPose;
    }

    /**
     * Applies a vision correction to the fused pose.
     * Blends the current fused pose toward the vision estimate based on trust.
     *
     * @param visionPose The pose estimate from vision (should be close to truePose)
     * @param trust How much to trust vision (0.0 = ignore, 1.0 = snap to vision)
     */
    public void applyVisionCorrection(Pose2d visionPose, double trust) {
        trust = Math.max(0.0, Math.min(1.0, trust)); // Clamp to [0, 1]

        // Blend fused pose toward vision pose
        double newX = fusedPose.getX() + (visionPose.getX() - fusedPose.getX()) * trust;
        double newY = fusedPose.getY() + (visionPose.getY() - fusedPose.getY()) * trust;

        // Blend heading (be careful with angle wrapping)
        double fusedRad = fusedPose.getRotation().getRadians();
        double visionRad = visionPose.getRotation().getRadians();
        double angleDiff = visionRad - fusedRad;
        // Normalize to [-PI, PI]
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;
        double newAngleRad = fusedRad + angleDiff * trust;

        fusedPose = new Pose2d(newX, newY, new Rotation2d(newAngleRad));
        fusedGyroAngleDeg = Math.toDegrees(newAngleRad);

        Logger.recordOutput("Sim/VisionCorrection/Applied", true);
        Logger.recordOutput("Sim/VisionCorrection/Trust", trust);
    }

    /**
     * Gets the current simulated gyro angle (fused heading).
     *
     * @return The simulated gyro angle in degrees
     */
    public double getGyroAngleDeg() {
        return fusedGyroAngleDeg;
    }

    /**
     * Gets the current simulated heading as a Rotation2d (fused heading).
     *
     * @return The simulated heading
     */
    public Rotation2d getRotation2d() {
        return Rotation2d.fromDegrees(fusedGyroAngleDeg);
    }

    /**
     * Resets the simulation to a specific pose.
     * Resets all poses (true, odometry, and fused) - clears accumulated drift.
     *
     * @param newPose The new pose
     */
    public void resetPose(Pose2d newPose) {
        truePose = newPose;
        trueGyroAngleDeg = newPose.getRotation().getDegrees();
        odometryPose = newPose;
        odometryGyroAngleDeg = newPose.getRotation().getDegrees();
        fusedPose = newPose;
        fusedGyroAngleDeg = newPose.getRotation().getDegrees();
        inputVx = 0;
        inputVy = 0;
        inputOmega = 0;
        lastUpdateTime = Timer.getFPGATimestamp();

        Logger.recordOutput("Sim/Reset", newPose);
    }

    /**
     * Resets the simulation to the origin.
     */
    public void resetToOrigin() {
        resetPose(new Pose2d());
    }
}
