package frc.robot.constants;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;

/**
 * Hardware-specific constants: CAN IDs, motor specs, physical dimensions.
 *
 * Change these when:
 * - Wiring changes (CAN IDs)
 * - Robot rebuild (dimensions, wheel size)
 * - Motor configuration (current limits, PID)
 */
public final class HardwareConstants {

  // ═══════════════════════════════════════════════════════════════════════
  // ROBOT PHYSICAL DIMENSIONS
  // ═══════════════════════════════════════════════════════════════════════

  /** Distance between left and right wheels (meters). Default: 29 inches for MAXSwerve. */
  public static final double kTrackWidth = Units.inchesToMeters(29);

  /** Distance between front and back wheels (meters). Default: 29 inches for MAXSwerve. */
  public static final double kWheelBase = Units.inchesToMeters(29);

  /** Swerve module positions relative to robot center. Order: FL, FR, RL, RR. */
  public static final Translation2d[] kModuleTranslations = new Translation2d[] {
      new Translation2d(kWheelBase / 2, kTrackWidth / 2),   // Front Left
      new Translation2d(kWheelBase / 2, -kTrackWidth / 2),  // Front Right
      new Translation2d(-kWheelBase / 2, kTrackWidth / 2),  // Rear Left
      new Translation2d(-kWheelBase / 2, -kTrackWidth / 2)  // Rear Right
  };

  // ═══════════════════════════════════════════════════════════════════════
  // CAN IDs
  // ═══════════════════════════════════════════════════════════════════════

  /** Front-left driving motor CAN ID. */
  public static final int kFrontLeftDrivingCanId = 10;

  /** Front-left turning motor CAN ID. */
  public static final int kFrontLeftTurningCanId = 11;

  /** Front-right driving motor CAN ID. */
  public static final int kFrontRightDrivingCanId = 20;

  /** Front-right turning motor CAN ID. */
  public static final int kFrontRightTurningCanId = 21;

  /** Rear-right driving motor CAN ID. */
  public static final int kRearRightDrivingCanId = 30;

  /** Rear-right turning motor CAN ID. */
  public static final int kRearRightTurningCanId = 31;

  /** Rear-left driving motor CAN ID. */
  public static final int kRearLeftDrivingCanId = 40;

  /** Rear-left turning motor CAN ID. */
  public static final int kRearLeftTurningCanId = 41;

  // ═══════════════════════════════════════════════════════════════════════
  // SWERVE MODULE ANGULAR OFFSETS
  // ═══════════════════════════════════════════════════════════════════════

  /** Front-left chassis angular offset (radians). Calibrate with bevel gear facing left. */
  public static final double kFrontLeftChassisAngularOffset = -Math.PI / 2;

  /** Front-right chassis angular offset (radians). Calibrate with bevel gear facing front. */
  public static final double kFrontRightChassisAngularOffset = 0;

  /** Rear-left chassis angular offset (radians). Calibrate with bevel gear facing back. */
  public static final double kBackLeftChassisAngularOffset = Math.PI;

  /** Rear-right chassis angular offset (radians). Calibrate with bevel gear facing right. */
  public static final double kBackRightChassisAngularOffset = Math.PI / 2;

  // ═══════════════════════════════════════════════════════════════════════
  // MOTOR SPECIFICATIONS
  // ═══════════════════════════════════════════════════════════════════════

  /** NEO motor free speed (RPM). */
  public static final double kNeoFreeSpeedRpm = 5676;

  /** Driving motor pinion teeth count. Options: 12T, 13T, 14T. Affects top speed. */
  public static final int kDrivingMotorPinionTeeth = 14;

  /** Wheel diameter (meters). Default: 3.0 inches. ⚠️ Update if wheels wear down! */
  public static final double kWheelDiameterMeters = Units.inchesToMeters(3.0);

  /** Driving motor gear reduction. Formula: (45 × 22) / (pinionTeeth × 15) */
  public static final double kDrivingMotorReduction = (45.0 * 22) / (kDrivingMotorPinionTeeth * 15);

  // ═══════════════════════════════════════════════════════════════════════
  // MOTOR CONFIGURATION
  // ═══════════════════════════════════════════════════════════════════════

  /** Driving motor current limit (amps). Prevents brownouts and motor overheating. */
  public static final int kDrivingMotorCurrentLimit = 50;

  /** Turning motor current limit (amps). Lower than driving since less continuous load. */
  public static final int kTurningMotorCurrentLimit = 20;

  /** Driving motor PID - Proportional gain. Tune if wheels oscillate or don't reach speed. */
  public static final double kDrivingP = 0.04;

  /** Driving motor PID - Integral gain. Usually 0 for velocity control. */
  public static final double kDrivingI = 0.0;

  /** Driving motor PID - Derivative gain. Usually 0 for velocity control. */
  public static final double kDrivingD = 0.0;

  /** Turning motor PID - Proportional gain. Tune if modules oscillate or turn slowly. */
  public static final double kTurningP = 1.0;

  /** Turning motor PID - Integral gain. Usually 0 for position control. */
  public static final double kTurningI = 0.0;

  /** Turning motor PID - Derivative gain. Usually 0 for position control. */
  public static final double kTurningD = 0.0;

  // ═══════════════════════════════════════════════════════════════════════
  // OPERATOR INTERFACE
  // ═══════════════════════════════════════════════════════════════════════

  /** Left joystick USB port (translation control). */
  public static final int kDriverLeftJoystickPort = 0;

  /** Right joystick USB port (rotation control). */
  public static final int kDriverRightJoystickPort = 1;

  /** Co-driver controller USB port. */
  public static final int kCoDriverControllerPort = 2;

  // ═══════════════════════════════════════════════════════════════════════
  // DERIVED CONSTANTS (Auto-calculated)
  // ═══════════════════════════════════════════════════════════════════════

  /** Wheel circumference (meters). Auto-calculated from diameter. */
  public static final double kWheelCircumferenceMeters = kWheelDiameterMeters * Math.PI;

  /** Motor free speed (rotations/second). Converted from RPM. */
  public static final double kDrivingMotorFreeSpeedRps = kNeoFreeSpeedRpm / 60;

  /** Theoretical max wheel speed (meters/second). Actual is lower due to voltage sag. */
  public static final double kDriveWheelFreeSpeedRps =
      (kDrivingMotorFreeSpeedRps * kWheelCircumferenceMeters) / kDrivingMotorReduction;

  private HardwareConstants() {
    throw new UnsupportedOperationException("This is a utility class!");
  }
}
