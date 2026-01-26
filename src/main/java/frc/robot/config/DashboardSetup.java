// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.config;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.commands.UtilityCommands;
import frc.robot.constants.VisionConstants;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.MatchStateTracker;
import frc.robot.subsystems.MatchStateTracker.HubStatus;
import frc.robot.subsystems.MatchStateTracker.WarningLevel;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *                           DASHBOARD SETUP
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Publishes robot data to NetworkTables for display in Elastic and AdvantageScope.
 *
 * <h2>DASHBOARD PHILOSOPHY (2026+)</h2>
 * <ul>
 *   <li><b>Elastic</b> = Live dashboard during matches (you design layout in Elastic)</li>
 *   <li><b>AdvantageScope</b> = Post-match log analysis and 3D visualization</li>
 *   <li><b>NetworkTables/SmartDashboard</b> = Data bus (code publishes, dashboards read)</li>
 * </ul>
 *
 * <h2>HOW IT WORKS</h2>
 * <ol>
 *   <li>This class publishes all robot data to SmartDashboard keys</li>
 *   <li>Elastic reads those keys and displays them (layout configured in Elastic)</li>
 *   <li>AdvantageKit logs data for AdvantageScope replay</li>
 * </ol>
 *
 * <h2>KEY NAMESPACES</h2>
 * <table>
 *   <tr><th>Namespace</th><th>Purpose</th></tr>
 *   <tr><td>Match/*</td><td>Hub status, phase, warnings (2026 REBUILT game)</td></tr>
 *   <tr><td>Drive/*</td><td>Speed, heading, field-relative status</td></tr>
 *   <tr><td>Vision/*</td><td>Camera status, targets, health</td></tr>
 *   <tr><td>Hub/*</td><td>Distance to hub, in-range status</td></tr>
 *   <tr><td>System/*</td><td>Battery, CPU, brownout</td></tr>
 * </table>
 *
 * <h2>ELASTIC SETUP</h2>
 * <p>In Elastic, create widgets that read these NetworkTables keys.
 * Recommended Match layout:
 * <ul>
 *   <li>Large boolean box for "Match/HubActive" (green/red)</li>
 *   <li>Text display for "Match/Warning" (warning messages)</li>
 *   <li>Number display for "Match/ShiftCountdown"</li>
 *   <li>Auto chooser for "Auto Selector"</li>
 * </ul>
 */
public class DashboardSetup {

  private final DriveSubsystem driveSubsystem;
  private final MatchStateTracker matchStateTracker;
  private final SendableChooser<Command> autoChooser;

  // Camera keys for reading from SmartDashboard
  private final String frontKey;
  private final String backKey;

  // Field visualization (works in both Elastic and AdvantageScope)
  private final Field2d field = new Field2d();

  /**
   * Creates dashboard setup.
   *
   * @param driveSubsystem    The drive subsystem
   * @param matchStateTracker The match state tracker (for hub status)
   * @param autoChooser       The autonomous selector
   */
  public DashboardSetup(
      DriveSubsystem driveSubsystem,
      MatchStateTracker matchStateTracker,
      SendableChooser<Command> autoChooser) {
    this.driveSubsystem = driveSubsystem;
    this.matchStateTracker = matchStateTracker;
    this.autoChooser = autoChooser;
    this.frontKey = "Vision/" + VisionConstants.kFrontCameraName;
    this.backKey = "Vision/" + VisionConstants.kBackCameraName;
  }

  /**
   * Initializes dashboard defaults.
   * Call once from RobotContainer after creating all subsystems.
   */
  public void configureAll() {
    // Publish auto chooser
    SmartDashboard.putData("Auto Selector", autoChooser);

    // Publish field visualization
    SmartDashboard.putData("Field", field);

    // Initialize default values
    SmartDashboard.putBoolean("Vision/Enabled", true);
    SmartDashboard.putBoolean("Vision/ClassroomMode", false);

    // Initialize Pit Mode command buttons (all false by default)
    SmartDashboard.putBoolean("Pit/SetWheelsStraight", false);
    SmartDashboard.putBoolean("Pit/ZeroHeading", false);
    SmartDashboard.putBoolean("Pit/CoastMode", false);
    SmartDashboard.putBoolean("Pit/BrakeMode", false);
    SmartDashboard.putBoolean("Pit/ForceVisionReset", false);
  }

  /**
   * Updates all dashboard values.
   * Call this from Robot.robotPeriodic().
   */
  public void periodic() {
    // ═══════════════════════════════════════════════════════════════════════
    // PIT MODE COMMANDS (check for button presses)
    // ═══════════════════════════════════════════════════════════════════════
    checkPitCommands();

    // ═══════════════════════════════════════════════════════════════════════
    // MATCH STATE (2026 REBUILT - Hub status and warnings)
    // ═══════════════════════════════════════════════════════════════════════
    updateMatchState();

    // ═══════════════════════════════════════════════════════════════════════
    // DRIVE STATUS
    // ═══════════════════════════════════════════════════════════════════════
    updateDriveStatus();

    // ═══════════════════════════════════════════════════════════════════════
    // SYSTEM STATUS
    // ═══════════════════════════════════════════════════════════════════════
    updateSystemStatus();

    // ═══════════════════════════════════════════════════════════════════════
    // FIELD VISUALIZATION
    // ═══════════════════════════════════════════════════════════════════════
    field.setRobotPose(driveSubsystem.getCurrentPose());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MATCH STATE (2026 REBUILT game-specific)
  // ═══════════════════════════════════════════════════════════════════════════

  private void updateMatchState() {
    // Hub status - THE most critical info
    HubStatus hubStatus = matchStateTracker.getOurHubStatus();
    SmartDashboard.putBoolean("Match/HubActive", hubStatus == HubStatus.ACTIVE);
    SmartDashboard.putString("Match/HubStatus", hubStatus.getDisplayName());

    // Next status
    HubStatus nextStatus = matchStateTracker.getNextHubStatus();
    String nextDisplay = switch (nextStatus) {
      case ACTIVE -> "→ ACTIVE";
      case INACTIVE -> "→ INACTIVE";
      default -> "→ ???";
    };
    SmartDashboard.putString("Match/NextStatus", nextDisplay);

    // Timing
    SmartDashboard.putNumber("Match/ShiftCountdown", Math.ceil(matchStateTracker.getTimeUntilNextShift()));
    SmartDashboard.putNumber("Match/TimeInPhase", matchStateTracker.getTimeInPhase());
    SmartDashboard.putString("Match/Phase", matchStateTracker.getCurrentPhase().getDisplayName());

    // Warnings - This is what drives team action
    WarningLevel warning = matchStateTracker.getCurrentWarning();
    SmartDashboard.putString("Match/Warning", warning.getMessage());
    SmartDashboard.putBoolean("Match/HasWarning", warning != WarningLevel.NONE);

    // Warning type for color coding in Elastic
    String warningType = switch (warning) {
      case HUB_CLOSING_SOON -> "CLEAR";
      case HUB_OPENING_SOON -> "READY";
      case ENDGAME_APPROACHING, ENDGAME_URGENT, ENDGAME_CRITICAL -> "CLIMB";
      default -> "NONE";
    };
    SmartDashboard.putString("Match/WarningType", warningType);

    // FMS data status
    SmartDashboard.putBoolean("Match/FmsDataReceived", matchStateTracker.isFmsDataReceived());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DRIVE STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  private void updateDriveStatus() {
    // Speed mode
    double mult = driveSubsystem.getSpeedMultiplier();
    String speedMode;
    if (mult >= 0.9) speedMode = "FULL";
    else if (mult >= 0.7) speedMode = "75%";
    else if (mult >= 0.4) speedMode = "HALF";
    else speedMode = "SLOW";

    SmartDashboard.putString("Drive/SpeedMode", speedMode);
    SmartDashboard.putNumber("Drive/SpeedMultiplier", mult);

    // Field relative
    SmartDashboard.putBoolean("Drive/FieldRelative", driveSubsystem.getFieldRelative());

    // Heading (normalized 0-360)
    double heading = driveSubsystem.getHeading() % 360;
    if (heading < 0) heading += 360;
    SmartDashboard.putNumber("Drive/Heading", Math.round(heading));

    // Pose
    Pose2d pose = driveSubsystem.getCurrentPose();
    SmartDashboard.putNumber("Drive/PoseX", round2(pose.getX()));
    SmartDashboard.putNumber("Drive/PoseY", round2(pose.getY()));
    SmartDashboard.putNumber("Drive/PoseRotation", round1(pose.getRotation().getDegrees()));

    // Tag count (aggregated from both cameras)
    int frontTags = (int) SmartDashboard.getNumber(frontKey + "/TargetCount", 0);
    int backTags = (int) SmartDashboard.getNumber(backKey + "/TargetCount", 0);
    int totalTags = frontTags + backTags;
    SmartDashboard.putNumber("Drive/TagCount", totalTags);
    SmartDashboard.putBoolean("Drive/HasMultiTag", totalTags >= 2);

    // Pose sanity checks
    SmartDashboard.putBoolean("Drive/PoseInBounds", isInFieldBounds());
    SmartDashboard.putBoolean("Drive/PoseSane", isPoseSane());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SYSTEM STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  private void updateSystemStatus() {
    SmartDashboard.putNumber("System/BatteryVoltage", round2(RobotController.getBatteryVoltage()));
    SmartDashboard.putNumber("System/CPUTemp", round1(RobotController.getCPUTemp()));
    SmartDashboard.putBoolean("System/Brownout", RobotController.isBrownedOut());

    // Battery status for easy display
    double voltage = RobotController.getBatteryVoltage();
    String batteryStatus;
    if (voltage >= 12.0) batteryStatus = "GOOD";
    else if (voltage >= 11.0) batteryStatus = "OK";
    else batteryStatus = "LOW";
    SmartDashboard.putString("System/BatteryStatus", batteryStatus);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PIT MODE COMMANDS
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Checks for Pit Mode button presses and triggers commands.
   * Buttons auto-reset after triggering.
   */
  private void checkPitCommands() {
    // Set Wheels Straight
    if (SmartDashboard.getBoolean("Pit/SetWheelsStraight", false)) {
      SmartDashboard.putBoolean("Pit/SetWheelsStraight", false);
      UtilityCommands.setStraightAhead(driveSubsystem).schedule();
      System.out.println("PIT: Setting wheels straight");
    }

    // Zero Heading
    if (SmartDashboard.getBoolean("Pit/ZeroHeading", false)) {
      SmartDashboard.putBoolean("Pit/ZeroHeading", false);
      UtilityCommands.zeroHeading(driveSubsystem).schedule();
      System.out.println("PIT: Zeroing heading");
    }

    // Coast Mode
    if (SmartDashboard.getBoolean("Pit/CoastMode", false)) {
      SmartDashboard.putBoolean("Pit/CoastMode", false);
      UtilityCommands.setCoastMode(driveSubsystem).schedule();
      System.out.println("PIT: Motors set to COAST mode");
    }

    // Brake Mode
    if (SmartDashboard.getBoolean("Pit/BrakeMode", false)) {
      SmartDashboard.putBoolean("Pit/BrakeMode", false);
      UtilityCommands.setBrakeMode(driveSubsystem).schedule();
      System.out.println("PIT: Motors set to BRAKE mode");
    }

    // Force Vision Reset
    if (SmartDashboard.getBoolean("Pit/ForceVisionReset", false)) {
      SmartDashboard.putBoolean("Pit/ForceVisionReset", false);
      UtilityCommands.forceVisionReset(driveSubsystem).schedule();
      System.out.println("PIT: Forcing vision reset");
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Checks if the current pose is within reasonable field bounds.
   * FRC field is approximately 16.5m x 8.0m.
   */
  private boolean isInFieldBounds() {
    Pose2d pose = driveSubsystem.getCurrentPose();
    double x = pose.getX();
    double y = pose.getY();
    return x >= -1.0 && x <= 17.5 && y >= -1.0 && y <= 9.0;
  }

  /**
   * Overall sanity check for pose.
   */
  private boolean isPoseSane() {
    return isInFieldBounds() && Math.abs(driveSubsystem.getGyroRate()) < 720;
  }

  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}
