package frc.robot.constants;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

/**
 * Field-specific constants for 2026 REBUILT game.
 *
 * <p>The main scoring location is the HUB, surrounded by 8 AprilTags per alliance.
 *
 * <p><b>Hub AprilTag IDs:</b>
 * <ul>
 *   <li>Red Hub: Tags 2, 3, 4, 5, 8, 9, 10, 11
 *   <li>Blue Hub: Tags 18, 19, 20, 21, 24, 25, 26, 27
 * </ul>
 */
public final class FieldConstants {

  // ═══════════════════════════════════════════════════════════════════════
  // FIELD DIMENSIONS (Standard for all FRC fields)
  // ═══════════════════════════════════════════════════════════════════════

  /** Field length (meters) - Blue alliance wall to Red alliance wall. */
  public static final double kFieldLength = 16.54175;

  /** Field width (meters) - Driver station wall to opposite wall. */
  public static final double kFieldWidth = 8.0137;

  // ═══════════════════════════════════════════════════════════════════════
  // HUB POSITIONS - Center of the 8 AprilTags surrounding each hub
  // ═══════════════════════════════════════════════════════════════════════
  //
  // Calculated from AprilTag positions (averaged from 8 tags per hub):
  //
  // RED HUB TAGS (IDs 2,3,4,5,8,9,10,11):
  //   Center X = avg(469.11, 445.35, 445.35, 469.11, 483.11, 492.88, 492.88, 483.11) = 472.61 inches
  //   Center Y = avg(182.6, 172.84, 158.84, 135.09, 135.09, 144.84, 158.84, 182.6) = 158.84 inches
  //
  // BLUE HUB TAGS (IDs 18,19,20,21,24,25,26,27):
  //   Center X = avg(182.11, 205.87, 205.87, 182.11, 168.11, 158.34, 158.34, 168.11) = 178.61 inches
  //   Center Y = avg(135.09, 144.84, 158.84, 182.6, 182.6, 172.84, 158.84, 135.09) = 158.84 inches
  //

  /** Red Hub center position (meters). Calculated from AprilTags 2,3,4,5,8,9,10,11. */
  public static final Translation2d kRedHubCenter = new Translation2d(
      Units.inchesToMeters(472.61),  // 12.004 meters
      Units.inchesToMeters(158.84)   // 4.035 meters
  );

  /** Blue Hub center position (meters). Calculated from AprilTags 18,19,20,21,24,25,26,27. */
  public static final Translation2d kBlueHubCenter = new Translation2d(
      Units.inchesToMeters(178.61),  // 4.537 meters
      Units.inchesToMeters(158.84)   // 4.035 meters
  );

  /** Red Hub as a Pose2d (facing the hub from the field). */
  public static final Pose2d kRedHubPose = new Pose2d(kRedHubCenter, Rotation2d.fromDegrees(180));

  /** Blue Hub as a Pose2d (facing the hub from the field). */
  public static final Pose2d kBlueHubPose = new Pose2d(kBlueHubCenter, Rotation2d.fromDegrees(0));

  /**
   * Gets the hub center for the current alliance.
   *
   * @return Hub center Translation2d, or Blue hub if alliance unknown
   */
  public static Translation2d getAllianceHubCenter() {
    var alliance = DriverStation.getAlliance();
    if (alliance.isPresent() && alliance.get() == Alliance.Red) {
      return kRedHubCenter;
    }
    return kBlueHubCenter;
  }

  /**
   * Gets the hub pose for the current alliance.
   *
   * @return Hub Pose2d, or Blue hub if alliance unknown
   */
  public static Pose2d getAllianceHubPose() {
    var alliance = DriverStation.getAlliance();
    if (alliance.isPresent() && alliance.get() == Alliance.Red) {
      return kRedHubPose;
    }
    return kBlueHubPose;
  }

  // ═══════════════════════════════════════════════════════════════════════
  // SHOOTING RANGE CONSTANTS
  // ═══════════════════════════════════════════════════════════════════════

  /** Minimum distance from hub for reliable shooting (meters). */
  public static final double kMinShootingDistance = 1.5;

  /** Maximum distance from hub for reliable shooting (meters). */
  public static final double kMaxShootingDistance = 5.0;

  /** Optimal shooting distance from hub (meters). */
  public static final double kOptimalShootingDistance = 3.0;

  // ═══════════════════════════════════════════════════════════════════════
  // BLUE ALLIANCE SCORING POSITIONS
  // ═══════════════════════════════════════════════════════════════════════
  // TODO: Replace with actual game scoring locations

  /** Example scoring position - Left. */
  public static final Pose2d kBlueScoringLeft = new Pose2d(2.0, 5.5, Rotation2d.fromDegrees(0));

  /** Example scoring position - Center. */
  public static final Pose2d kBlueScoringCenter = new Pose2d(2.0, 4.0, Rotation2d.fromDegrees(0));

  /** Example scoring position - Right. */
  public static final Pose2d kBlueScoringRight = new Pose2d(2.0, 2.5, Rotation2d.fromDegrees(0));

  /** Example secondary scoring location. */
  public static final Pose2d kBlueSecondaryScoring = new Pose2d(1.5, 7.0, Rotation2d.fromDegrees(-90));

  // ═══════════════════════════════════════════════════════════════════════
  // RED ALLIANCE SCORING POSITIONS (mirrored from blue)
  // ═══════════════════════════════════════════════════════════════════════
  // TODO: Update to match blue alliance positions (mirrored across field centerline)

  /** Example scoring position - Left (mirrored from blue). */
  public static final Pose2d kRedScoringLeft = new Pose2d(14.5, 5.5, Rotation2d.fromDegrees(180));

  /** Example scoring position - Center (mirrored from blue). */
  public static final Pose2d kRedScoringCenter = new Pose2d(14.5, 4.0, Rotation2d.fromDegrees(180));

  /** Example scoring position - Right (mirrored from blue). */
  public static final Pose2d kRedScoringRight = new Pose2d(14.5, 2.5, Rotation2d.fromDegrees(180));

  /** Example secondary scoring location (mirrored from blue). */
  public static final Pose2d kRedSecondaryScoring = new Pose2d(15.0, 7.0, Rotation2d.fromDegrees(90));

  // ═══════════════════════════════════════════════════════════════════════
  // SCORING DISTANCE OFFSETS
  // ═══════════════════════════════════════════════════════════════════════
  // TODO: Update based on robot dimensions and game requirements

  /**
   * Distance to stop from primary scoring target (meters).
   *
   * <p>Robot positions this distance away before deploying scoring mechanism.
   * Adjust based on robot bumper dimensions and game piece release point.
   */
  public static final double kPrimaryScoringDistance = 0.5;

  /**
   * Distance to stop from secondary scoring target (meters).
   *
   * <p>May be different than primary if mechanism geometry differs.
   */
  public static final double kSecondaryScoringDistance = 0.4;

  private FieldConstants() {
    throw new UnsupportedOperationException("This is a utility class!");
  }
}
