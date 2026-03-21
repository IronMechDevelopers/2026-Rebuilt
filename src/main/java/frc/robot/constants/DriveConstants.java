package frc.robot.constants;

import com.pathplanner.lib.config.PIDConstants;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;

/**
 * Drive system behavior constants: speeds, safety, control tuning.
 *
 * Change these when:
 * - Tuning drive feel (speeds, PID)
 * - Adjusting safety thresholds (brownout, vision)
 * - Modifying autonomous behavior
 */
public final class DriveConstants {

  // =====================================================================
  // DRIVE SPEEDS
  // =====================================================================

  /** Max drive speed in teleop (m/s). Tune for responsive but controllable driving. */
  public static final double kMaxSpeedMetersPerSecond = 4.8;

  /** Max rotation speed (rad/s). 2π = 1 full rotation per second. */
  public static final double kMaxAngularSpeed = 2 * Math.PI;

  /** Default speed multiplier at startup (0.0 to 1.0). Robot starts at full speed. */
  public static final double kDefaultSpeedMultiplier = 1.0;

  // =====================================================================══
  // KINEMATICS
  // =====================================================================══

  /** Swerve drive kinematics. Uses module positions from HardwareConstants. */
  public static final SwerveDriveKinematics kDriveKinematics =
      new SwerveDriveKinematics(HardwareConstants.kModuleTranslations);

  // =====================================================================══
  // ODOMETRY & VISION FUSION
  // =====================================================================══

  /** Odometry trust - X position (meters). Lower = trust encoders/gyro more. */
  public static final double kStateStdDevX = 0.1;

  /** Odometry trust - Y position (meters). Lower = trust encoders/gyro more. */
  public static final double kStateStdDevY = 0.1;

  /**
   * Odometry trust - Rotation (radians). Lower = trust encoders/gyro more.
   *
   * NavX AHRS gyro is VERY accurate for rotation (< 1° drift/min).
   * This should be significantly lower than vision theta StdDevs.
   * At 0.1 rad (~5.7°), gyro is trusted 5-10x more than vision for rotation.
   */
  public static final double kStateStdDevTheta = 0.1;

  /** Vision baseline trust - X position (meters). Overridden per-frame based on quality. */
  public static final double kVisionStdDevX = 1.5;

  /** Vision baseline trust - Y position (meters). Overridden per-frame based on quality. */
  public static final double kVisionStdDevY = 1.5;

  /** Vision baseline trust - Rotation (radians). Overridden per-frame based on quality. */
  public static final double kVisionStdDevTheta = 1.5;

  // =====================================================================══
  // SAFETY THRESHOLDS
  // =====================================================================══

  /** Brownout voltage threshold (volts). Logs warning when battery drops below this. */
  public static final double kBrownoutVoltageThreshold = 8.0;

  /** Max angular velocity to accept vision (deg/sec). Reject if spinning too fast (motion blur). */
  public static final double kMaxAngularVelocityForVisionDegPerSec = 720.0;

  /** Vision timeout (seconds). Warn if no AprilTags detected for this long. */
  public static final double kVisionTimeoutSeconds = 2.0;

  /** Max age for vision measurements (seconds). Reject frames older than this. */
  public static final double kMaxVisionMeasurementAgeSeconds = 1.5;

  // =====================================================================══
  // VISION CORRECTION THRESHOLDS - Competition Mode vs Classroom Mode
  // =====================================================================══

  /**
   * COMPETITION MODE: Large vision correction threshold (meters).
   * Trust vision highly when correction > this (multi-tag detections).
   */
  public static final double kVisionLargeCorrectionThreshold_Competition = 0.5;

  /**
   * COMPETITION MODE: Small vision correction threshold (meters).
   * Accept single-tag vision when correction < this.
   * CRITICAL: This is the main safety filter to prevent "teleporting"!
   */
  public static final double kVisionSmallCorrectionThreshold_Competition = 1.0;

  /**
   * CLASSROOM MODE: Large vision correction threshold (meters).
   * More permissive for testing with single AprilTag in small space.
   */
  public static final double kVisionLargeCorrectionThreshold_Classroom = 2.0;

  /**
   * CLASSROOM MODE: Small vision correction threshold (meters).
   * Relaxed to allow testing when robot is placed far from expected position.
   * ⚠️ WARNING: This allows large jumps - only use during controlled testing!
   */
  public static final double kVisionSmallCorrectionThreshold_Classroom = 5.0;

  /**
   * @deprecated Use kVisionLargeCorrectionThreshold_Competition or _Classroom instead
   */
  @Deprecated
  public static final double kVisionLargeCorrectionThreshold = kVisionLargeCorrectionThreshold_Competition;

  /**
   * @deprecated Use kVisionSmallCorrectionThreshold_Competition or _Classroom instead
   */
  @Deprecated
  public static final double kVisionSmallCorrectionThreshold = kVisionSmallCorrectionThreshold_Competition;

  /** High-trust vision std dev - X/Y (meters). Used when making large corrections. */
  public static final double kVisionHighTrustStdDevXY = 0.1;

  /**
   * High-trust vision std dev - Rotation (radians). Used when making large corrections.
   *
   * Even for "high trust" multi-tag corrections, we still trust gyro more for rotation.
   * This is ~30° - gyro (0.1 rad / 5.7°) is still trusted ~5x more.
   */
  public static final double kVisionHighTrustStdDevTheta = 0.5;

  /** No-trust vision std dev (meters/radians). So high that pose estimator ignores it. */
  public static final double kVisionNoTrustStdDev = 100.0;

  // =====================================================================══
  // TELEOP CONTROL TUNING
  // =====================================================================══

  /** Joystick deadband (0.0 to 1.0). Prevents drift when stick is released. */
  public static final double kJoystickDeadband = 0.15;

  /** Translation slew rate limiter (units/sec). Prevents tipping during acceleration. */
  public static final double kTranslationSlewRate = 3.0;

  /** Rotation slew rate limiter (units/sec). Prevents tipping during rotation. */
  public static final double kRotationSlewRate = 3.0;

  /**
   * PathPlanner translation PID for teleop commands (aim, positioning).
   *
   * Start at (5.0, 0, 0). Tune P if robot oscillates or doesn't reach target.
   */
  public static final PIDConstants kTranslationPID = new PIDConstants(5.0, 0.0, 0.0);

  /**
   * PathPlanner rotation PID for teleop commands (auto-aim, snap-to-angle).
   *
   * Start at (5.0, 0, 0). Tune P if robot oscillates or doesn't reach angle.
   */
  public static final PIDConstants kRotationPID = new PIDConstants(5.5, 0.0, 0.0);

  /** Max velocity for pathfinding (m/s). */
  public static final double kPathfindMaxVelMetersPerSecond = 3.0;

  /** Max acceleration for pathfinding (m/s²). */
  public static final double kPathfindMaxAccelMetersPerSecondSquared = 3.0;

  /** Max angular velocity for pathfinding (rad/s). */
  public static final double kPathfindMaxAngularVelRadiansPerSecond = Math.PI;

  /** Max angular acceleration for pathfinding (rad/s²). */
  public static final double kPathfindMaxAngularAccelRadiansPerSecondSquared = Math.PI;

  /** Auto-aim PID - P gain. Tune if robot oscillates or doesn't reach target. */
  public static final double kAutoAimP = 5.0;

  /** Auto-aim PID - I gain. Usually 0. */
  public static final double kAutoAimI = 0.0;

  /** Auto-aim PID - D gain. Usually 0. */
  public static final double kAutoAimD = 0.0;

  /** Auto-aim tolerance (degrees). On-target if within this angle. */
  public static final double kAutoAimToleranceDegrees = 2.0;

  /** Heading lock PID - P gain. Tune if robot oscillates or doesn't hold heading. */
  public static final double kHeadingLockP = 5.0;

  /** Heading lock PID - I gain. Usually 0. */
  public static final double kHeadingLockI = 0.0;

  /** Heading lock PID - D gain. Usually 0. */
  public static final double kHeadingLockD = 0.0;

  // =====================================================================══
  // TELEOP PRESETS
  // =====================================================================══

  /** Full speed multiplier (100%). */
  public static final double kFullSpeedMultiplier = 1.0;

  /** Three-quarter speed multiplier (75%). */
  public static final double kThreeQuarterSpeedMultiplier = 0.75;

  /** Half speed multiplier (50%). Good for precision near game pieces. */
  public static final double kHalfSpeedMultiplier = 0.5;

  /** Quarter speed multiplier (25%). Very slow for fine positioning. */
  public static final double kQuarterSpeedMultiplier = 0.25;

  /** Speed toggle threshold. Toggle switches to half if above, full if below. */
  public static final double kSpeedToggleThreshold = 0.6;

  /** X-stance angle (degrees). Modules form an "X" for stability during defense. */
  public static final double kXStanceAngleDegrees = 45.0;

  /** Cardinal snap increment (degrees). Snap to 0/90/180/270. */
  public static final double kCardinalSnapIncrementDegrees = 90.0;

  /** Cardinal snap offset (degrees). 0° for N/E/S/W directions. */
  public static final double kCardinalSnapOffsetDegrees = 0.0;

  /** Diamond snap offset (degrees). 45° for NE/SE/SW/NW directions. */
  public static final double kDiamondSnapOffsetDegrees = 45.0;

  // =====================================================================══
  // AUTONOMOUS
  // =====================================================================══

  /** Pathfinding goal end velocity (m/s). 0 = stop at destination. */
  public static final double kPathfindGoalEndVelocity = 0.0;

  // =====================================================================══
  // TESTING
  // =====================================================================══

  /** Default test target X position (meters). Used for dashboard vision testing. */
  public static final double kDefaultTestTargetX = 2.0;

  /** Default test target Y position (meters). Used for dashboard vision testing. */
  public static final double kDefaultTestTargetY = 2.0;

  /** Default test target heading (degrees). 0° = facing red alliance. */
  public static final double kDefaultTestTargetHeadingDegrees = 0.0;

  /** Default distance from AprilTag during testing (meters). */
  public static final double kDefaultDistanceFromTag = 1.0;

  private DriveConstants() {
    throw new UnsupportedOperationException("This is a utility class!");
  }
}
