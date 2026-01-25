package frc.robot.constants;

/**
 * Vision system constants: camera configuration and trust parameters.
 *
 * Change these when:
 * - Installing/moving cameras (position, rotation, name)
 * - Tuning vision trust (distance limits, ambiguity, std devs)
 */
public final class VisionConstants {

  // ═══════════════════════════════════════════════════════════════════════
  // CAMERA IDENTIFICATION
  // ═══════════════════════════════════════════════════════════════════════

  /**
   * Front camera name in PhotonVision.
   *
   * Must match name in PhotonVision UI (http://photonvision.local:5800).
   * Used for forward-facing AprilTag detection.
   */
  public static final String kFrontCameraName = "front";

  /**
   * Back camera name in PhotonVision.
   *
   * Must match name in PhotonVision UI (http://photonvision.local:5800).
   * Used for rear-facing AprilTag detection.
   */
  public static final String kBackCameraName = "back";

  /**
   * Legacy single camera name (deprecated - use kFrontCameraName/kBackCameraName).
   *
   * Kept for backwards compatibility. Defaults to front camera.
   */
  @Deprecated
  public static final String kCameraName = kFrontCameraName;

  // ═══════════════════════════════════════════════════════════════════════
  // FRONT CAMERA POSITION (Robot-to-Camera Transform)
  // ═══════════════════════════════════════════════════════════════════════

  /**
   * Front camera X offset from robot center (meters).
   *
   * + = forward, - = backward
   * Measure from robot center (intersection of wheel diagonals).
   */
  public static final double kFrontCameraToRobotX = 0.0;

  /**
   * Front camera Y offset from robot center (meters).
   *
   * + = left, - = right
   * Measure from robot center.
   */
  public static final double kFrontCameraToRobotY = 0.0;

  /**
   * Front camera Z offset from robot center (meters).
   *
   * + = up, - = down
   * Measure from robot center.
   */
  public static final double kFrontCameraToRobotZ = 0.0;

  /**
   * Front camera pitch angle (radians).
   *
   * Tilt up/down:
   * - Negative = tilted down (common for AprilTags on floor)
   * - 0 = level with ground
   * - Positive = tilted up
   *
   * Example: -0.52 rad = -30° tilt down
   */
  public static final double kFrontCameraPitchRadians = 0.0;

  /**
   * Front camera yaw angle (radians).
   *
   * Pan left/right:
   * - 0 = facing forward (most common)
   * - 1.57 = facing left (90°)
   * - -1.57 = facing right (-90°)
   */
  public static final double kFrontCameraYawRadians = 0.0;

  /**
   * Front camera roll angle (radians).
   *
   * Usually 0.0 unless camera is mounted sideways.
   */
  public static final double kFrontCameraRollRadians = 0.0;

  // ═══════════════════════════════════════════════════════════════════════
  // BACK CAMERA POSITION (Robot-to-Camera Transform)
  // ═══════════════════════════════════════════════════════════════════════

  /**
   * Back camera X offset from robot center (meters).
   *
   * + = forward, - = backward
   * Measure from robot center (intersection of wheel diagonals).
   * Typically negative since it's mounted at the back.
   */
  public static final double kBackCameraToRobotX = 0.0;

  /**
   * Back camera Y offset from robot center (meters).
   *
   * + = left, - = right
   * Measure from robot center.
   */
  public static final double kBackCameraToRobotY = 0.0;

  /**
   * Back camera Z offset from robot center (meters).
   *
   * + = up, - = down
   * Measure from robot center.
   */
  public static final double kBackCameraToRobotZ = 0.0;

  /**
   * Back camera pitch angle (radians).
   *
   * Tilt up/down:
   * - Negative = tilted down (common for AprilTags on floor)
   * - 0 = level with ground
   * - Positive = tilted up
   *
   * Example: -0.52 rad = -30° tilt down
   */
  public static final double kBackCameraPitchRadians = 0.0;

  /**
   * Back camera yaw angle (radians).
   *
   * Pan left/right:
   * - Math.PI (3.14) = facing backward (180°)
   * - Other values if camera is angled
   */
  public static final double kBackCameraYawRadians = Math.PI;

  /**
   * Back camera roll angle (radians).
   *
   * Usually 0.0 unless camera is mounted sideways.
   */
  public static final double kBackCameraRollRadians = 0.0;

  // ═══════════════════════════════════════════════════════════════════════
  // LEGACY CAMERA POSITION (Deprecated - use Front/Back specific constants)
  // ═══════════════════════════════════════════════════════════════════════

  /** @deprecated Use kFrontCameraToRobotX instead */
  @Deprecated
  public static final double kCameraToRobotX = kFrontCameraToRobotX;

  /** @deprecated Use kFrontCameraToRobotY instead */
  @Deprecated
  public static final double kCameraToRobotY = kFrontCameraToRobotY;

  /** @deprecated Use kFrontCameraToRobotZ instead */
  @Deprecated
  public static final double kCameraToRobotZ = kFrontCameraToRobotZ;

  /** @deprecated Use kFrontCameraPitchRadians instead */
  @Deprecated
  public static final double kCameraPitchRadians = kFrontCameraPitchRadians;

  /** @deprecated Use kFrontCameraYawRadians instead */
  @Deprecated
  public static final double kCameraYawRadians = kFrontCameraYawRadians;

  /** @deprecated Use kFrontCameraRollRadians instead */
  @Deprecated
  public static final double kCameraRollRadians = kFrontCameraRollRadians;

  // ═══════════════════════════════════════════════════════════════════════
  // VISION QUALITY FILTERS
  // ═══════════════════════════════════════════════════════════════════════

  /**
   * Max distance for single-tag detections (meters).
   *
   * Reject single tags farther than this. Multi-tag always accepted.
   * Increase if field is large, decrease if getting bad detections.
   */
  public static final double kMaxVisionDistanceMeters = 4.0;

  /**
   * Max ambiguity for AprilTag detections (0.0 to 1.0).
   *
   * Ambiguity measures detection quality:
   * - 0.0 = perfect, unambiguous
   * - 0.2 = good (our threshold)
   * - 0.5+ = questionable, likely bad
   *
   * Lower = stricter filtering, fewer but better detections.
   */
  public static final double kMaxAmbiguity = 0.2;

  /**
   * Max acceptable latency for vision measurements (milliseconds).
   *
   * Latency is the delay between when the camera captures an image
   * and when we receive the processed result:
   * - < 50ms = excellent (real-time)
   * - 50-100ms = acceptable (our threshold)
   * - > 100ms = too slow, may cause overshoot
   *
   * If latency exceeds this, the dashboard will show a warning.
   */
  public static final double kMaxLatencyMs = 100.0;

  // ═══════════════════════════════════════════════════════════════════════
  // VISION HEALTH & SAFETY
  // ═══════════════════════════════════════════════════════════════════════

  /**
   * Number of consecutive failures before marking vision as unhealthy.
   *
   * If vision fails this many times in a row, we stop trusting it
   * and commands that require vision will gracefully disable.
   */
  public static final int kMaxConsecutiveFailures = 10;

  /**
   * Seconds without a successful pose estimate before marking vision stale.
   *
   * If we haven't gotten a good pose in this long, something is wrong.
   * Dashboard will show a warning.
   */
  public static final double kVisionStaleTimeoutSeconds = 2.0;

  /**
   * Minimum interval between error log messages (seconds).
   *
   * Prevents console spam when vision is having issues.
   * Errors are still counted, just not all printed.
   */
  public static final double kErrorLogIntervalSeconds = 5.0;

  /**
   * Enable verbose vision logging for debugging.
   *
   * When true, logs detailed per-frame data to SmartDashboard.
   * When false, only logs summary data to reduce network traffic.
   *
   * Set to false for competition to reduce bandwidth usage.
   */
  public static final boolean kVerboseLogging = false;

  // ═══════════════════════════════════════════════════════════════════════
  // VISION TRUST (Standard Deviations)
  // ═══════════════════════════════════════════════════════════════════════

  /**
   * Multi-tag high trust std dev - X/Y position (meters).
   *
   * When 2+ tags detected, trust vision highly.
   * Lower = higher trust.
   */
  public static final double kMultiTagHighTrustStdDevXY = 0.5;

  /**
   * Multi-tag high trust std dev - Rotation (degrees).
   *
   * Converted to radians in code via Math.toRadians().
   *
   * NOTE: Gyro is much more accurate than vision for rotation.
   * We trust gyro ~5x more than multi-tag vision for theta.
   * Vision primarily corrects X/Y drift, not rotation.
   */
  public static final double kMultiTagHighTrustStdDevThetaDegrees = 30.0;

  /**
   * Single-tag base trust std dev - X/Y position (meters).
   *
   * Base uncertainty before distance scaling.
   * Multiplied by distance-based trust scale in VisionSubsystem.
   */
  public static final double kSingleTagBaseTrustStdDevXY = 0.9;

  /**
   * Single-tag base trust std dev - Rotation (degrees).
   *
   * Converted to radians in code via Math.toRadians().
   *
   * NOTE: Single-tag rotation is very unreliable due to ambiguity.
   * We barely trust it for rotation - gyro handles this.
   * This value gets multiplied by distance scale, making it even higher.
   */
  public static final double kSingleTagBaseTrustStdDevThetaDegrees = 60.0;

  /**
   * Trust scale formula denominator.
   *
   * Used in: trustScale = 1 + (distance² / denominator)
   *
   * Current value: 30
   * - 1m away: scale = 1.03 (very trustworthy)
   * - 2m away: scale = 1.13 (pretty good)
   * - 3m away: scale = 1.30 (okay)
   */
  public static final double kVisionTrustScaleDenominator = 30.0;

  private VisionConstants() {
    throw new UnsupportedOperationException("This is a utility class!");
  }
}
