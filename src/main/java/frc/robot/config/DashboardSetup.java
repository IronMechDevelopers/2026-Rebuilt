// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.config;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.commands.UtilityCommands;
import frc.robot.constants.VisionConstants;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.MatchStateTracker;
import frc.robot.subsystems.MatchStateTracker.HubStatus;
import frc.robot.subsystems.MatchStateTracker.WarningLevel;

/**
 * Configures Shuffleboard tabs and widgets following the 4-dashboard structure.
 *
 * <h2>TABS</h2>
 * <ul>
 *   <li><b>Match</b> - Drive team during competition</li>
 *   <li><b>Pit</b> - Pit crew between matches</li>
 *   <li><b>Vision</b> - Vision tuning</li>
 *   <li><b>Software</b> - Programmers</li>
 * </ul>
 */
public class DashboardSetup {

  private final DriveSubsystem driveSubsystem;
  private final MatchStateTracker matchStateTracker;
  private final SendableChooser<Command> autoChooser;

  // Camera keys
  private final String frontCameraKey;
  private final String backCameraKey;

  // Field visualizations (one per tab - can't share Sendables)
  private final Field2d matchField = new Field2d();

  // =====================================================================══════
  // MATCH TAB
  // =====================================================================══════
  private GenericEntry matchHubActive;
  private GenericEntry matchHubStatus;
  private GenericEntry matchNextStatus;
  private GenericEntry matchShiftCountdown;
  private GenericEntry matchPhase;
  private GenericEntry matchWarning;
  private GenericEntry matchShiftPattern;
  private GenericEntry matchBattery;
  private GenericEntry matchVisionHealthy;
  private GenericEntry matchDistanceToHub;
  private GenericEntry matchInRange;

  // =====================================================================══════
  // PIT TAB
  // =====================================================================══════
  private GenericEntry pitBattery;
  private GenericEntry pitBatteryStatus;
  private GenericEntry pitVisionHealthy;
  private GenericEntry pitCameraCount;
  private GenericEntry pitCanUtil;
  private GenericEntry pitSetWheelsStraight;
  private GenericEntry pitZeroHeading;
  private GenericEntry pitCoastMode;
  private GenericEntry pitBrakeMode;
  private GenericEntry pitVisionReset;

  // =====================================================================══════
  // VISION TAB
  // =====================================================================══════
  private GenericEntry visionHealthy;
  private GenericEntry visionEnabled;
  private GenericEntry visionTotalTags;
  private GenericEntry visionAccepted;
  private GenericEntry visionRejected;
  private GenericEntry visionFrontConnected;
  private GenericEntry visionFrontTargets;
  private GenericEntry visionBackConnected;
  private GenericEntry visionBackTargets;

  // =====================================================================══════
  // SOFTWARE TAB
  // =====================================================================══════
  private GenericEntry softwareBattery;
  private GenericEntry softwareBrownout;
  private GenericEntry softwareCanUtil;
  private GenericEntry softwarePracticeEnabled;
  private GenericEntry softwareClassroomMode;

  // =====================================================================══════
  // RATE LIMITING - Reduce network load by updating less frequently
  // =====================================================================══════
  private static final int DASHBOARD_UPDATE_INTERVAL_MS = 100; // Update every 100ms (5x slower)
  private long lastDashboardUpdateTime = 0;

  public DashboardSetup(
      DriveSubsystem driveSubsystem,
      MatchStateTracker matchStateTracker,
      SendableChooser<Command> autoChooser) {
    this.driveSubsystem = driveSubsystem;
    this.matchStateTracker = matchStateTracker;
    this.autoChooser = autoChooser;
    this.frontCameraKey = "Vision/" + VisionConstants.kFrontCameraName;
    this.backCameraKey = "Vision/" + VisionConstants.kBackCameraName;
  }

  /**
   * Configures all dashboard tabs.
   */
  public void configureAll() {
    System.out.println("DashboardSetup: Starting configuration...");

    try {
      configureMatchTab();
      System.out.println("DashboardSetup: Match tab configured");
    } catch (Exception e) {
      DriverStation.reportError("DashboardSetup: Match tab failed: " + e.getMessage(), e.getStackTrace());
    }

    try {
      configurePitTab();
      System.out.println("DashboardSetup: Pit tab configured");
    } catch (Exception e) {
      DriverStation.reportError("DashboardSetup: Pit tab failed: " + e.getMessage(), e.getStackTrace());
    }

    try {
      configureVisionTab();
      System.out.println("DashboardSetup: Vision tab configured");
    } catch (Exception e) {
      DriverStation.reportError("DashboardSetup: Vision tab failed: " + e.getMessage(), e.getStackTrace());
    }

    try {
      configureSoftwareTab();
      System.out.println("DashboardSetup: Software tab configured");
    } catch (Exception e) {
      DriverStation.reportError("DashboardSetup: Software tab failed: " + e.getMessage(), e.getStackTrace());
    }

    // Initialize defaults
    SmartDashboard.putBoolean("Vision/Enabled", true);
    SmartDashboard.putBoolean("Vision/ClassroomMode", false);

    System.out.println("DashboardSetup: All tabs configured successfully!");
  }

  private void configureMatchTab() {
    ShuffleboardTab tab = Shuffleboard.getTab("Match");

    // Row 0: Hub status and countdown (primary focus)
    matchHubActive = tab.add("Hub Active", false).withPosition(0, 0).withSize(2, 2).getEntry();
    matchHubStatus = tab.add("Hub Status", "---").withPosition(2, 0).withSize(2, 1).getEntry();
    matchNextStatus = tab.add("Next Status", "---").withPosition(4, 0).withSize(2, 1).getEntry();
    matchShiftCountdown = tab.add("Shift In (sec)", 0).withPosition(6, 0).withSize(2, 2).getEntry();

    // Row 1: Phase and status indicators
    matchPhase = tab.add("Phase", "---").withPosition(2, 1).withSize(2, 1).getEntry();
    matchBattery = tab.add("Battery V", 0.0).withPosition(4, 1).withSize(1, 1).getEntry();
    matchVisionHealthy = tab.add("Vision OK", false).withPosition(5, 1).withSize(1, 1).getEntry();
    matchDistanceToHub = tab.add("Distance (m)", 0.0).withPosition(6, 1).withSize(1, 1).getEntry();
    matchInRange = tab.add("In Range", false).withPosition(7, 1).withSize(1, 1).getEntry();

    // Row 2: WARNING BANNER - Full width for maximum visibility
    matchWarning = tab.add("WARNING", "").withPosition(0, 2).withSize(8, 1).getEntry();

    // Row 3: Shift pattern timeline and auto selector
    matchShiftPattern = tab.add("Shift Pattern", "").withPosition(0, 3).withSize(5, 1).getEntry();
    tab.add("Auto Selector", autoChooser).withPosition(5, 3).withSize(3, 1);

    // Rows 4-6: Field visualization (larger for better visibility)
    tab.add("Field", matchField).withPosition(0, 4).withSize(8, 3);
  }

  private void configurePitTab() {
    ShuffleboardTab tab = Shuffleboard.getTab("Pit");

    // System status row
    pitBattery = tab.add("Battery V", 0.0).withPosition(0, 0).withSize(1, 1).getEntry();
    pitBatteryStatus = tab.add("Battery", "---").withPosition(1, 0).withSize(1, 1).getEntry();
    pitVisionHealthy = tab.add("Vision OK", false).withPosition(2, 0).withSize(1, 1).getEntry();
    pitCameraCount = tab.add("Cameras", 0).withPosition(3, 0).withSize(1, 1).getEntry();
    pitCanUtil = tab.add("CAN %", 0.0).withPosition(4, 0).withSize(1, 1).getEntry();

    // Command buttons
    pitSetWheelsStraight = tab.add("Wheels Straight", false).withPosition(0, 1).withSize(2, 1).getEntry();
    pitZeroHeading = tab.add("Zero Heading", false).withPosition(2, 1).withSize(2, 1).getEntry();
    pitCoastMode = tab.add("Coast Mode", false).withPosition(0, 2).withSize(2, 1).getEntry();
    pitBrakeMode = tab.add("Brake Mode", false).withPosition(2, 2).withSize(2, 1).getEntry();
    pitVisionReset = tab.add("Vision Reset", false).withPosition(4, 1).withSize(2, 1).getEntry();
  }

  private void configureVisionTab() {
    ShuffleboardTab tab = Shuffleboard.getTab("Vision");

    // Overall status
    visionHealthy = tab.add("Healthy", false).withPosition(0, 0).withSize(1, 1).getEntry();
    visionEnabled = tab.add("Enabled", true).withPosition(1, 0).withSize(1, 1).getEntry();
    visionTotalTags = tab.add("Total Tags", 0).withPosition(2, 0).withSize(1, 1).getEntry();
    visionAccepted = tab.add("Accepted", 0).withPosition(3, 0).withSize(1, 1).getEntry();
    visionRejected = tab.add("Rejected", 0).withPosition(4, 0).withSize(1, 1).getEntry();

    // Per-camera status
    visionFrontConnected = tab.add("Front Connected", false).withPosition(0, 1).withSize(2, 1).getEntry();
    visionFrontTargets = tab.add("Front Targets", 0).withPosition(2, 1).withSize(1, 1).getEntry();
    visionBackConnected = tab.add("Back Connected", false).withPosition(0, 2).withSize(2, 1).getEntry();
    visionBackTargets = tab.add("Back Targets", 0).withPosition(2, 2).withSize(1, 1).getEntry();
  }

  private void configureSoftwareTab() {
    ShuffleboardTab tab = Shuffleboard.getTab("Software");

    // System info
    softwareBattery = tab.add("Battery V", 0.0).withPosition(0, 0).withSize(1, 1).getEntry();
    softwareBrownout = tab.add("Brownout", false).withPosition(1, 0).withSize(1, 1).getEntry();
    softwareCanUtil = tab.add("CAN %", 0.0).withPosition(2, 0).withSize(1, 1).getEntry();

    // Mode toggles
    softwarePracticeEnabled = tab.add("Practice Mode", false).withPosition(0, 1).withSize(2, 1).getEntry();
    softwareClassroomMode = tab.add("Classroom Mode", false).withPosition(2, 1).withSize(2, 1).getEntry();
  }

  /**
   * Updates all dashboard values. Call from Robot.robotPeriodic().
   * Rate-limited to reduce network load and prevent loop overruns.
   */
  public void periodic() {
    // Check pit commands every cycle (responsive to button presses)
    checkPitCommands();

    // Rate-limit dashboard updates to reduce network load
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastDashboardUpdateTime < DASHBOARD_UPDATE_INTERVAL_MS) {
      return; // Skip this cycle
    }
    lastDashboardUpdateTime = currentTime;

    // Update all tabs (rate-limited)
    updateMatchTab();
    updatePitTab();
    updateVisionTab();
    updateSoftwareTab();

    // Update field
    matchField.setRobotPose(driveSubsystem.getCurrentPose());

    // Sync toggles to SmartDashboard for other systems
    SmartDashboard.putBoolean("Vision/Enabled", visionEnabled.getBoolean(true));
    SmartDashboard.putBoolean("Vision/ClassroomMode", softwareClassroomMode.getBoolean(false));
    SmartDashboard.putBoolean("Practice/Enabled", softwarePracticeEnabled.getBoolean(false));
  }

  private void updateMatchTab() {
    HubStatus hubStatus = matchStateTracker.getOurHubStatus();
    matchHubActive.setBoolean(hubStatus == HubStatus.ACTIVE);
    matchHubStatus.setString(hubStatus.getDisplayName());
    matchNextStatus.setString(matchStateTracker.getNextHubStatus().getDisplayName());
    matchShiftCountdown.setDouble(Math.ceil(matchStateTracker.getTimeUntilNextShift()));
    matchPhase.setString(matchStateTracker.getCurrentPhase().getDisplayName());

    WarningLevel warning = matchStateTracker.getCurrentWarning();
    matchWarning.setString(warning.getMessage());

    matchShiftPattern.setString(generateShiftPatternString());

    matchBattery.setDouble(round1(RobotController.getBatteryVoltage()));
    matchVisionHealthy.setBoolean(driveSubsystem.isVisionHealthy());
    matchDistanceToHub.setDouble(round1(driveSubsystem.getDistanceToHub()));
    matchInRange.setBoolean(driveSubsystem.isInHubRange());
  }

  private void updatePitTab() {
    double voltage = RobotController.getBatteryVoltage();
    pitBattery.setDouble(round1(voltage));
    pitBatteryStatus.setString(voltage >= 12.0 ? "GOOD" : voltage >= 11.0 ? "OK" : "LOW");
    pitVisionHealthy.setBoolean(driveSubsystem.isVisionHealthy());
    pitCameraCount.setInteger((int) SmartDashboard.getNumber("Vision/CameraCount", 0));
    pitCanUtil.setDouble(round1(RobotController.getCANStatus().percentBusUtilization));
  }

  private void updateVisionTab() {
    visionHealthy.setBoolean(driveSubsystem.isVisionHealthy());
    visionTotalTags.setInteger((int) SmartDashboard.getNumber("Vision/TotalTagCount", 0));
    visionAccepted.setInteger((int) SmartDashboard.getNumber("Vision/PosesAccepted", 0));
    visionRejected.setInteger((int) SmartDashboard.getNumber("Vision/PosesRejected", 0));

    visionFrontConnected.setBoolean(SmartDashboard.getBoolean(frontCameraKey + "/Connected", false));
    visionFrontTargets.setInteger((int) SmartDashboard.getNumber(frontCameraKey + "/TargetCount", 0));
    visionBackConnected.setBoolean(SmartDashboard.getBoolean(backCameraKey + "/Connected", false));
    visionBackTargets.setInteger((int) SmartDashboard.getNumber(backCameraKey + "/TargetCount", 0));
  }

  private void updateSoftwareTab() {
    softwareBattery.setDouble(round1(RobotController.getBatteryVoltage()));
    softwareBrownout.setBoolean(RobotController.isBrownedOut());
    softwareCanUtil.setDouble(round1(RobotController.getCANStatus().percentBusUtilization));
  }

  private void checkPitCommands() {
    CommandScheduler scheduler = CommandScheduler.getInstance();
    if (pitSetWheelsStraight.getBoolean(false)) {
      pitSetWheelsStraight.setBoolean(false);
      scheduler.schedule(UtilityCommands.setStraightAhead(driveSubsystem));
    }
    if (pitZeroHeading.getBoolean(false)) {
      pitZeroHeading.setBoolean(false);
      scheduler.schedule(UtilityCommands.zeroHeading(driveSubsystem));
    }
    if (pitCoastMode.getBoolean(false)) {
      pitCoastMode.setBoolean(false);
      scheduler.schedule(UtilityCommands.setCoastMode(driveSubsystem));
    }
    if (pitBrakeMode.getBoolean(false)) {
      pitBrakeMode.setBoolean(false);
      scheduler.schedule(UtilityCommands.setBrakeMode(driveSubsystem));
    }
    if (pitVisionReset.getBoolean(false)) {
      pitVisionReset.setBoolean(false);
      scheduler.schedule(UtilityCommands.forceVisionReset(driveSubsystem));
    }
  }

  /**
   * Generates a compact shift pattern string for the drive team.
   * Format: "S1:ACTIVE → S2:INACTIVE → S3:ACTIVE → S4:INACTIVE"
   */
  private String generateShiftPatternString() {
    String[] pattern = matchStateTracker.getShiftPatternArray();

    // Extract just the shifts (indices 2-5) and format compactly
    StringBuilder sb = new StringBuilder();
    for (int i = 2; i < 6; i++) {
      if (i > 2) {
        sb.append(" → ");
      }
      String shift = pattern[i];
      // Convert "SHIFT 1: ACTIVE (25s)" to "S1:ACTIVE"
      if (shift.contains("SHIFT")) {
        String shiftNum = shift.substring(6, 7); // Extract "1" from "SHIFT 1"
        String status = shift.contains("ACTIVE") ? "ACTIVE" :
                       shift.contains("INACTIVE") ? "INACTIVE" : "UNKNOWN";
        sb.append("S").append(shiftNum).append(":").append(status);
      }
    }
    return sb.toString();
  }

  private double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}
