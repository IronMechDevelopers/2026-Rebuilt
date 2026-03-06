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
import frc.robot.subsystems.CANFuelSubsystem;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.MatchStateTracker;
import frc.robot.subsystems.MatchStateTracker.HubStatus;
import frc.robot.subsystems.MatchStateTracker.WarningLevel;

/**
 * Configures Shuffleboard tabs and widgets for competition and pit use.
 *
 * <h2>TABS</h2>
 * <ul>
 *   <li><b>Match</b> - Drive team during competition (shift awareness, field position, warnings)</li>
 *   <li><b>Pit</b> - Pit crew between matches (system checks, subsystem testing, setup controls)</li>
 * </ul>
 */
public class DashboardSetup {

  private final DriveSubsystem driveSubsystem;
  private final MatchStateTracker matchStateTracker;
  private final CANFuelSubsystem fuelSubsystem;
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
  // PIT TAB - For pit crew between matches
  // =====================================================================══════
  private GenericEntry pitBattery;
  private GenericEntry pitBatteryStatus;
  private GenericEntry pitVisionHealthy;
  private GenericEntry pitCameraCount;
  private GenericEntry pitCanUtil;

  // Drive system controls
  private GenericEntry pitSetWheelsStraight;
  private GenericEntry pitZeroHeading;
  private GenericEntry pitCoastMode;
  private GenericEntry pitBrakeMode;
  private GenericEntry pitVisionReset;

  // Fuel subsystem test controls
  private GenericEntry pitTestIntake;
  private GenericEntry pitTestEject;
  private GenericEntry pitTestLaunch;
  private GenericEntry pitStopFuel;

  // =====================================================================══════
  // RATE LIMITING - Reduce network load by updating less frequently
  // =====================================================================══════
  private static final int DASHBOARD_UPDATE_INTERVAL_MS = 100; // Update every 100ms (5x slower)
  private long lastDashboardUpdateTime = 0;

  public DashboardSetup(
      DriveSubsystem driveSubsystem,
      MatchStateTracker matchStateTracker,
      CANFuelSubsystem fuelSubsystem,
      SendableChooser<Command> autoChooser) {
    this.driveSubsystem = driveSubsystem;
    this.matchStateTracker = matchStateTracker;
    this.fuelSubsystem = fuelSubsystem;
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

    // Initialize defaults
    SmartDashboard.putBoolean("Vision/Enabled", true);
    SmartDashboard.putBoolean("Vision/ClassroomMode", false);
    SmartDashboard.putBoolean("Practice/Enabled", false);

    // Set Match tab as the default view
    Shuffleboard.selectTab("Match");

    System.out.println("DashboardSetup: All tabs configured successfully!");
    System.out.println("DashboardSetup: Match tab set as default");
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

    // ========== ROW 0: PRE-MATCH CHECKLIST ==========
    pitBattery = tab.add("Battery V", 0.0).withPosition(0, 0).withSize(1, 1).getEntry();
    pitBatteryStatus = tab.add("Battery Status", "---").withPosition(1, 0).withSize(2, 1).getEntry();
    pitVisionHealthy = tab.add("Vision Healthy", false).withPosition(3, 0).withSize(1, 1).getEntry();
    pitCameraCount = tab.add("Cameras", 0).withPosition(4, 0).withSize(1, 1).getEntry();
    pitCanUtil = tab.add("CAN Bus %", 0.0).withPosition(5, 0).withSize(1, 1).getEntry();

    // ========== ROW 1: DRIVE SYSTEM SETUP ==========
    pitSetWheelsStraight = tab.add("Set Wheels Straight", false).withPosition(0, 1).withSize(2, 1).getEntry();
    pitZeroHeading = tab.add("Zero Heading", false).withPosition(2, 1).withSize(2, 1).getEntry();
    pitCoastMode = tab.add("Coast Mode", false).withPosition(4, 1).withSize(1, 1).getEntry();
    pitBrakeMode = tab.add("Brake Mode", false).withPosition(5, 1).withSize(1, 1).getEntry();

    // ========== ROW 2: VISION SYSTEM ==========
    pitVisionReset = tab.add("Reset Vision", false).withPosition(0, 2).withSize(2, 1).getEntry();

    // ========== ROWS 3-4: FUEL SUBSYSTEM TESTING ==========
    pitTestIntake = tab.add("Test Intake", false).withPosition(0, 3).withSize(2, 1).getEntry();
    pitTestEject = tab.add("Test Eject", false).withPosition(2, 3).withSize(2, 1).getEntry();
    pitTestLaunch = tab.add("Test Launch", false).withPosition(4, 3).withSize(2, 1).getEntry();
    pitStopFuel = tab.add("STOP Fuel", false).withPosition(0, 4).withSize(2, 2).getEntry();
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

    // Update field
    matchField.setRobotPose(driveSubsystem.getCurrentPose());
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

    // Battery status with clear visual feedback
    if (voltage >= 12.0) {
      pitBatteryStatus.setString("GOOD - Ready for match");
    } else if (voltage >= 11.5) {
      pitBatteryStatus.setString("OK - Use for practice");
    } else if (voltage >= 11.0) {
      pitBatteryStatus.setString("LOW - Charge soon");
    } else {
      pitBatteryStatus.setString("CRITICAL - Charge now!");
    }

    pitVisionHealthy.setBoolean(driveSubsystem.isVisionHealthy());
    pitCameraCount.setInteger((int) SmartDashboard.getNumber("Vision/CameraCount", 0));
    pitCanUtil.setDouble(round1(RobotController.getCANStatus().percentBusUtilization));
  }

  private void checkPitCommands() {
    CommandScheduler scheduler = CommandScheduler.getInstance();

    // ========== DRIVE SYSTEM COMMANDS ==========
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

    // ========== VISION SYSTEM COMMANDS ==========
    if (pitVisionReset.getBoolean(false)) {
      pitVisionReset.setBoolean(false);
      scheduler.schedule(UtilityCommands.forceVisionReset(driveSubsystem));
    }

    // ========== FUEL SUBSYSTEM TEST COMMANDS ==========
    if (pitTestIntake.getBoolean(false)) {
      pitTestIntake.setBoolean(false);
      scheduler.schedule(fuelSubsystem.runOnce(() -> fuelSubsystem.intake())
          .withTimeout(2.0)
          .andThen(() -> fuelSubsystem.stop()));
      System.out.println("Pit: Testing intake (2 seconds)");
    }
    if (pitTestEject.getBoolean(false)) {
      pitTestEject.setBoolean(false);
      scheduler.schedule(fuelSubsystem.runOnce(() -> fuelSubsystem.eject())
          .withTimeout(2.0)
          .andThen(() -> fuelSubsystem.stop()));
      System.out.println("Pit: Testing eject (2 seconds)");
    }
    if (pitTestLaunch.getBoolean(false)) {
      pitTestLaunch.setBoolean(false);
      scheduler.schedule(fuelSubsystem.runOnce(() -> fuelSubsystem.launch())
          .withTimeout(2.0)
          .andThen(() -> fuelSubsystem.stop()));
      System.out.println("Pit: Testing launch (2 seconds)");
    }
    if (pitStopFuel.getBoolean(false)) {
      pitStopFuel.setBoolean(false);
      fuelSubsystem.stop();
      System.out.println("Pit: Stopped fuel subsystem");
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
