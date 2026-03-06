package frc.robot.constants;

import edu.wpi.first.math.util.Units;

/**
 * Vision system constants: camera configuration and trust parameters.
 *
 * OPTIMIZED FOR: Raspberry Pi 5 + OV9281 Global Shutter Cameras (720p)
 */
public final class VisionConstants {

  // =====================================================================══
  // CAMERA IDENTIFICATION
  // =====================================================================══

  public static final String kFrontCameraName = "front";
  public static final String kBackCameraName = "back";

  /** @deprecated Kept for backwards compatibility. */
  @Deprecated
  public static final String kCameraName = kFrontCameraName;

  // =====================================================================══
  // FRONT CAMERA POSITION (Robot-to-Camera Transform)
  // =====================================================================══
  // TODO: YOU MUST MEASURE THESE ON THE REAL ROBOT
  
  public static final double kFrontCameraToRobotX = 0.0; // Forward (+)
  public static final double kFrontCameraToRobotY = 0.0; // Left (+)
  public static final double kFrontCameraToRobotZ = 0.0; // Up (+)
  public static final double kFrontCameraPitchRadians = 0.0;
  public static final double kFrontCameraYawRadians = 0.0;
  public static final double kFrontCameraRollRadians = 0.0;

  // =====================================================================══
  // BACK CAMERA POSITION (Robot-to-Camera Transform)
  // =====================================================================══
  // TODO: YOU MUST MEASURE THESE ON THE REAL ROBOT

  public static final double kBackCameraToRobotX = 0.0; 
  public static final double kBackCameraToRobotY = 0.0;
  public static final double kBackCameraToRobotZ = 0.0;
  public static final double kBackCameraPitchRadians = 0.0;
  public static final double kBackCameraYawRadians = Math.PI; // Facing Backwards
  public static final double kBackCameraRollRadians = 0.0;

  // =====================================================================══
  // LEGACY POSITION (Deprecated)
  // =====================================================================══
  @Deprecated public static final double kCameraToRobotX = kFrontCameraToRobotX;
  @Deprecated public static final double kCameraToRobotY = kFrontCameraToRobotY;
  @Deprecated public static final double kCameraToRobotZ = kFrontCameraToRobotZ;
  @Deprecated public static final double kCameraPitchRadians = kFrontCameraPitchRadians;
  @Deprecated public static final double kCameraYawRadians = kFrontCameraYawRadians;
  @Deprecated public static final double kCameraRollRadians = kFrontCameraRollRadians;

  // =====================================================================══
  // VISION QUALITY FILTERS
  // =====================================================================══

  /**
   * Max distance for SINGLE-tag detections (meters).
   * 
   * 4.0 meters is approx 13 feet.
   * Past this range, single-tag depth data is too noisy for odometry.
   */
  public static final double kMaxVisionDistanceMeters = 4.0;

  /**
   * Max ambiguity for AprilTag detections (0.0 to 1.0).
   * 0.2 is the standard community recommendation.
   */
  public static final double kMaxAmbiguity = 0.2;

  /**
   * Max acceptable latency. Pi 5 + Mono Global Shutter should be ~25ms.
   * If it's over 50ms, check your settings (Resolution/Decimate).
   */
  public static final double kMaxLatencyMs = 50.0;

  // =====================================================================══
  // VISION HEALTH & SAFETY
  // =====================================================================══

  public static final int kMaxConsecutiveFailures = 10;
  public static final double kVisionStaleTimeoutSeconds = 2.0;
  public static final double kErrorLogIntervalSeconds = 5.0;
  public static final boolean kVerboseLogging = false;

  // =====================================================================══
  // VISION TRUST (Standard Deviations) - THE "SECRET SAUCE"
  // =====================================================================══

  /**
   * Multi-tag high trust std dev - X/Y position (meters).
   * 
   * With Pi 5 + Global Shutter, multi-tag is extremely accurate.
   * 0.05m = ~2 inches. We trust this very highly.
   */
  public static final double kMultiTagHighTrustStdDevXY = 0.10;

  /**
   * Multi-tag high trust std dev - Rotation (degrees).
   * 
   * Multi-tag solves the "PnP" problem perfectly, so we can trust rotation.
   * Previous value of 30.0 was too loose. 5.0 allows it to correct gyro drift.
   */
  public static final double kMultiTagHighTrustStdDevThetaDegrees = 5;

  /**
   * Single-tag base trust std dev - X/Y position (meters).
   * 
   * Base uncertainty at close range (1 meter).
   * 0.15m = ~6 inches. 
   * This scales UP as distance increases.
   */
  public static final double kSingleTagBaseTrustStdDevXY = 0.15;

  /**
   * Single-tag base trust std dev - Rotation (degrees).
   * 
   * Single-tag rotation is prone to "Ambiguity flipping", so we trust it less 
   * than multi-tag, but 5.0 is still useful for rough alignment.
   */
  public static final double kSingleTagBaseTrustStdDevThetaDegrees = 5.0;

  /**
   * Trust scale formula denominator.
   * 
   * Used in: trustScale = 1 + (distance² / denominator)
   * 
   * Value: 2.5
   * - 1m away: scale = 1.4 (Trust = ~0.21m) -> Precise
   * - 3m away: scale = 4.6 (Trust = ~0.70m) -> Loose (prevents jitter)
   * - 4m away: scale = 7.4 (Trust = ~1.10m) -> Very Loose (essentially ignored)
   * 
   * This creates a "trust curve" that allows long range detection (12ft)
   * without letting the noise ruin your autonomous driving.
   */
  public static final double kVisionTrustScaleDenominator = 2.5;

  private VisionConstants() {
    throw new UnsupportedOperationException("This is a utility class!");
  }
}