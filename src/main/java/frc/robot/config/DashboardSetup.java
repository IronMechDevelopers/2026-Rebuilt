// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.config;

import java.util.Map;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.commands.DriveCommands;
import frc.robot.constants.DriveConstants;
import frc.robot.constants.VisionConstants;
import frc.robot.subsystems.DriveSubsystem;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *                           DASHBOARD SETUP
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Configures Shuffleboard tabs for different audiences.
 *
 * <h2>DASHBOARD PHILOSOPHY</h2>
 * <ul>
 *   <li><b>Shuffleboard</b> = Live display during matches and testing</li>
 *   <li><b>AdvantageScope</b> = Post-match log analysis (uses Logger.recordOutput)</li>
 *   <li><b>SmartDashboard</b> = Data bus only (subsystems publish, dashboard reads)</li>
 * </ul>
 *
 * <h2>TABS BY AUDIENCE</h2>
 * <table>
 *   <tr><th>Tab</th><th>Audience</th><th>Purpose</th></tr>
 *   <tr><td>Match</td><td>Drive Team</td><td>Critical info only, large readable widgets</td></tr>
 *   <tr><td>Pit</td><td>Pit Crew</td><td>System health, quick diagnostics</td></tr>
 *   <tr><td>Debug</td><td>Software Team</td><td>Detailed data, PID tuning, testing</td></tr>
 *   <tr><td>Vision Cal</td><td>Vision Team</td><td>Camera calibration and testing</td></tr>
 * </table>
 *
 * <h2>BEST PRACTICES</h2>
 * <ul>
 *   <li>Match tab: 6 widgets max, readable from 10 feet away</li>
 *   <li>Use boolean indicators (green/red) for status</li>
 *   <li>Group related info in layouts</li>
 *   <li>Log detailed data to AdvantageScope, not Shuffleboard</li>
 * </ul>
 */
public class DashboardSetup {

  private final DriveSubsystem driveSubsystem;
  private final SendableChooser<Command> autoChooser;

  // Camera keys for reading from SmartDashboard (data bus)
  private final String frontKey;
  private final String backKey;

  // Persistent entries for writable widgets
  private GenericEntry classroomModeEntry;
  private GenericEntry visionEnabledEntry;

  // Field visualization for Vision Cal tab
  private final Field2d visionCalField = new Field2d();

  /**
   * Creates dashboard setup.
   *
   * @param driveSubsystem The drive subsystem
   * @param autoChooser    The autonomous selector
   */
  public DashboardSetup(DriveSubsystem driveSubsystem, SendableChooser<Command> autoChooser) {
    this.driveSubsystem = driveSubsystem;
    this.autoChooser = autoChooser;
    this.frontKey = "Vision/" + VisionConstants.kFrontCameraName;
    this.backKey = "Vision/" + VisionConstants.kBackCameraName;
  }

  /**
   * Configures all Shuffleboard tabs.
   * Call once from RobotContainer after creating all subsystems.
   */
  public void configureAll() {
    configureMatchTab();
    configurePitTab();
    configureDebugTab();
    configureVisionCalTab();

    // Set Match as the default tab for competition
    Shuffleboard.selectTab("Match");
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MATCH TAB - Drive Team (Competition)
  // ═══════════════════════════════════════════════════════════════════════════
  //
  // DESIGN PRINCIPLES:
  // - Maximum 6 large widgets
  // - Must be readable from 10 feet away
  // - Green = good, Red = problem
  // - Only show what drivers NEED to know

  private void configureMatchTab() {
    ShuffleboardTab tab = Shuffleboard.getTab("Match");

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 0: Auto Selection (LARGEST - drivers need to verify before match)
    // ─────────────────────────────────────────────────────────────────────────
    tab.add("AUTO MODE", autoChooser)
        .withWidget(BuiltInWidgets.kComboBoxChooser)
        .withPosition(0, 0)
        .withSize(4, 2);

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 0: Critical Status Indicators
    // ─────────────────────────────────────────────────────────────────────────

    // Battery - Big number, easy to read
    tab.addNumber("BATTERY", RobotController::getBatteryVoltage)
        .withWidget(BuiltInWidgets.kDial)
        .withProperties(Map.of("min", 0, "max", 14))
        .withPosition(4, 0)
        .withSize(2, 2);

    // Vision Status - THE most important indicator
    // Green = vision working, Red = vision broken (auto-aim won't work)
    tab.addBoolean("VISION", () -> SmartDashboard.getBoolean("Vision/Healthy", false))
        .withWidget(BuiltInWidgets.kBooleanBox)
        .withProperties(Map.of("colorWhenTrue", "#00FF00", "colorWhenFalse", "#FF0000"))
        .withPosition(6, 0)
        .withSize(2, 2);

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 2: Drive Status
    // ─────────────────────────────────────────────────────────────────────────

    // Speed mode - So drivers know if they're in slow mode
    tab.addString("SPEED", () -> {
      double mult = driveSubsystem.getSpeedMultiplier();
      if (mult >= 0.9) return "FULL";
      if (mult >= 0.7) return "75%";
      if (mult >= 0.4) return "HALF";
      return "SLOW";
    })
        .withWidget(BuiltInWidgets.kTextView)
        .withPosition(0, 2)
        .withSize(2, 1);

    // Field Relative indicator
    tab.addBoolean("FIELD REL", driveSubsystem::getFieldRelative)
        .withWidget(BuiltInWidgets.kBooleanBox)
        .withProperties(Map.of("colorWhenTrue", "#00FF00", "colorWhenFalse", "#FFFF00"))
        .withPosition(2, 2)
        .withSize(2, 1);

    // Target count - Quick glance at what vision sees
    tab.addString("TAGS", () -> {
      int front = (int) SmartDashboard.getNumber(frontKey + "/TargetCount", 0);
      int back = (int) SmartDashboard.getNumber(backKey + "/TargetCount", 0);
      int total = front + back;
      if (total == 0) return "NONE";
      if (total >= 2) return total + " (MULTI)";
      return "1";
    })
        .withWidget(BuiltInWidgets.kTextView)
        .withPosition(4, 2)
        .withSize(2, 1);

    // Heading - Useful for knowing robot orientation
    tab.addNumber("HEADING", () -> {
      double heading = driveSubsystem.getHeading() % 360;
      if (heading < 0) heading += 360;
      return Math.round(heading);
    })
        .withWidget(BuiltInWidgets.kTextView)
        .withPosition(6, 2)
        .withSize(2, 1);

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 3: Hub Targeting (for 2026 REBUILT game)
    // ─────────────────────────────────────────────────────────────────────────

    // Hub Distance - Shows how far from the hub (in inches for driver familiarity)
    tab.addString("HUB DIST", () -> {
      double dist = SmartDashboard.getNumber("Hub/DistanceInches", -1);
      if (dist < 0) return "---";
      return String.format("%.0f\"", dist);  // Show as whole inches with " symbol
    })
        .withWidget(BuiltInWidgets.kTextView)
        .withPosition(0, 3)
        .withSize(2, 1);

    // In Range indicator - Green when in optimal shooting range
    tab.addBoolean("IN RANGE", () -> SmartDashboard.getBoolean("Hub/InRange", false))
        .withWidget(BuiltInWidgets.kBooleanBox)
        .withProperties(Map.of("colorWhenTrue", "#00FF00", "colorWhenFalse", "#FF0000"))
        .withPosition(2, 3)
        .withSize(2, 1);

    // Optimal distance hint (converted to inches: 1.5m = 59", 5.0m = 197")
    tab.addString("OPTIMAL", () -> "59-197\"")
        .withWidget(BuiltInWidgets.kTextView)
        .withPosition(4, 3)
        .withSize(2, 1);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PIT TAB - Pit Crew (Pre-Match Checks)
  // ═══════════════════════════════════════════════════════════════════════════
  //
  // DESIGN PRINCIPLES:
  // - Quick health check before going to field
  // - All systems status at a glance
  // - Buttons for common pit operations

  private void configurePitTab() {
    ShuffleboardTab tab = Shuffleboard.getTab("Pit");

    // ─────────────────────────────────────────────────────────────────────────
    // SYSTEM STATUS LAYOUT
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout statusLayout = tab.getLayout("System Status", BuiltInLayouts.kList)
        .withPosition(0, 0)
        .withSize(3, 4)
        .withProperties(Map.of("Label position", "LEFT"));

    statusLayout.addNumber("Battery (V)", RobotController::getBatteryVoltage);

    statusLayout.addBoolean("Vision Healthy",
        () -> SmartDashboard.getBoolean("Vision/Healthy", false));

    statusLayout.addBoolean("Front Camera",
        () -> SmartDashboard.getBoolean(frontKey + "/Connected", false));

    statusLayout.addBoolean("Back Camera",
        () -> SmartDashboard.getBoolean(backKey + "/Connected", false));

    statusLayout.addString("Vision Status",
        () -> SmartDashboard.getString("Vision/HealthStatus", "UNKNOWN"));

    // ─────────────────────────────────────────────────────────────────────────
    // QUICK COMMANDS LAYOUT
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout commandsLayout = tab.getLayout("Pit Commands", BuiltInLayouts.kList)
        .withPosition(3, 0)
        .withSize(2, 4)
        .withProperties(Map.of("Label position", "HIDDEN"));

    // Coast mode - For pushing robot in pit
    commandsLayout.add("Coast Mode", DriveCommands.setCoastMode(driveSubsystem));

    // Brake mode - For matches
    commandsLayout.add("Brake Mode", DriveCommands.setBrakeMode(driveSubsystem));

    // Zero heading - Reset gyro
    commandsLayout.add("Zero Heading", DriveCommands.zeroHeading(driveSubsystem));

    // Wheels straight - For inspection
    commandsLayout.add("Wheels Straight", DriveCommands.setStraightAhead(driveSubsystem));

    // ─────────────────────────────────────────────────────────────────────────
    // POSITION CHECK
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout positionLayout = tab.getLayout("Position", BuiltInLayouts.kList)
        .withPosition(5, 0)
        .withSize(2, 3)
        .withProperties(Map.of("Label position", "LEFT"));

    positionLayout.addNumber("X (m)", () ->
        Math.round(driveSubsystem.getCurrentPose().getX() * 100) / 100.0);
    positionLayout.addNumber("Y (m)", () ->
        Math.round(driveSubsystem.getCurrentPose().getY() * 100) / 100.0);
    positionLayout.addNumber("Heading (°)", () ->
        Math.round(driveSubsystem.getHeading() * 10) / 10.0);

    // Force vision reset button
    tab.add("Force Vision Reset", DriveCommands.forceVisionReset(driveSubsystem))
        .withPosition(5, 3)
        .withSize(2, 1);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DEBUG TAB - Software Team (Development)
  // ═══════════════════════════════════════════════════════════════════════════
  //
  // DESIGN PRINCIPLES:
  // - All the detailed data
  // - PID tuning widgets
  // - Test commands
  // - Not meant for competition use

  private void configureDebugTab() {
    ShuffleboardTab tab = Shuffleboard.getTab("Debug");

    // ─────────────────────────────────────────────────────────────────────────
    // POSE DATA
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout poseLayout = tab.getLayout("Pose", BuiltInLayouts.kGrid)
        .withPosition(0, 0)
        .withSize(3, 2)
        .withProperties(Map.of("Number of columns", 3, "Number of rows", 2));

    poseLayout.addNumber("X (m)", () -> driveSubsystem.getCurrentPose().getX());
    poseLayout.addNumber("Y (m)", () -> driveSubsystem.getCurrentPose().getY());
    poseLayout.addNumber("Heading (°)",
        () -> driveSubsystem.getCurrentPose().getRotation().getDegrees());
    poseLayout.addNumber("Gyro Rate (°/s)", driveSubsystem::getGyroRate);
    poseLayout.addNumber("Speed Mult", driveSubsystem::getSpeedMultiplier);
    poseLayout.addBoolean("Field Rel", driveSubsystem::getFieldRelative);

    // ─────────────────────────────────────────────────────────────────────────
    // HEADING LOCK PID TUNING
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout pidLayout = tab.getLayout("Heading Lock PID", BuiltInLayouts.kList)
        .withPosition(3, 0)
        .withSize(2, 3)
        .withProperties(Map.of("Label position", "LEFT"));

    pidLayout.addNumber("kP", () ->
        SmartDashboard.getNumber("HeadingLock/kP", DriveConstants.kHeadingLockP));
    pidLayout.addNumber("kI", () ->
        SmartDashboard.getNumber("HeadingLock/kI", DriveConstants.kHeadingLockI));
    pidLayout.addNumber("kD", () ->
        SmartDashboard.getNumber("HeadingLock/kD", DriveConstants.kHeadingLockD));
    pidLayout.addNumber("Error (°)", () ->
        SmartDashboard.getNumber("HeadingLock/ErrorDeg", 0));

    // ─────────────────────────────────────────────────────────────────────────
    // VISION SUMMARY
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout visionLayout = tab.getLayout("Vision", BuiltInLayouts.kList)
        .withPosition(5, 0)
        .withSize(2, 3)
        .withProperties(Map.of("Label position", "LEFT"));

    visionLayout.addBoolean("Healthy", () ->
        SmartDashboard.getBoolean("Vision/Healthy", false));
    visionLayout.addNumber("Failures", () ->
        SmartDashboard.getNumber("Vision/ConsecutiveFailures", 0));
    visionLayout.addBoolean("Any Targets", () ->
        SmartDashboard.getBoolean("Vision/AnyTargetsVisible", false));
    visionLayout.addNumber("Best Ambiguity", () ->
        SmartDashboard.getNumber("Vision/BestTarget/Ambiguity", 0));

    // ─────────────────────────────────────────────────────────────────────────
    // TEST COMMANDS
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout testLayout = tab.getLayout("Test Commands", BuiltInLayouts.kGrid)
        .withPosition(0, 2)
        .withSize(3, 2)
        .withProperties(Map.of("Number of columns", 2, "Number of rows", 3));

    testLayout.add("X-Stance", DriveCommands.xStance(driveSubsystem));
    testLayout.add("Full Speed", DriveCommands.setSpeed(driveSubsystem, 1.0));
    testLayout.add("Half Speed", DriveCommands.setSpeed(driveSubsystem, 0.5));
    testLayout.add("Quarter Speed", DriveCommands.setSpeed(driveSubsystem, 0.25));
    testLayout.add("Toggle Field Rel", DriveCommands.toggleFieldRelative(driveSubsystem));
    testLayout.add("Zero Heading", DriveCommands.zeroHeading(driveSubsystem));

    // ─────────────────────────────────────────────────────────────────────────
    // SYSTEM INFO
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout sysLayout = tab.getLayout("System", BuiltInLayouts.kList)
        .withPosition(7, 0)
        .withSize(2, 2)
        .withProperties(Map.of("Label position", "LEFT"));

    sysLayout.addNumber("Battery (V)", RobotController::getBatteryVoltage);
    sysLayout.addNumber("CPU Temp (C)", () -> RobotController.getCPUTemp());
    sysLayout.addBoolean("Brownout", RobotController::isBrownedOut);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VISION CAL TAB - Vision Calibration & Testing
  // ═══════════════════════════════════════════════════════════════════════════
  //
  // DESIGN PRINCIPLES:
  // - Everything needed to verify vision is working
  // - Controls to enable/disable for testing
  // - Detailed per-camera metrics
  // - POSE VISUALIZATION to catch bad camera transforms!

  private void configureVisionCalTab() {
    ShuffleboardTab tab = Shuffleboard.getTab("Vision Cal");

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 0: VISION CONTROLS
    // ─────────────────────────────────────────────────────────────────────────

    visionEnabledEntry = tab.add("Vision Enabled", true)
        .withWidget(BuiltInWidgets.kToggleSwitch)
        .withPosition(0, 0)
        .withSize(2, 1)
        .getEntry();

    classroomModeEntry = tab.add("Classroom Mode", false)
        .withWidget(BuiltInWidgets.kToggleSwitch)
        .withPosition(2, 0)
        .withSize(2, 1)
        .getEntry();

    tab.addBoolean("HEALTHY", () -> SmartDashboard.getBoolean("Vision/Healthy", false))
        .withWidget(BuiltInWidgets.kBooleanBox)
        .withProperties(Map.of("colorWhenTrue", "#00FF00", "colorWhenFalse", "#FF0000"))
        .withPosition(4, 0)
        .withSize(1, 1);

    tab.addString("Status", () -> SmartDashboard.getString("Vision/HealthStatus", "UNKNOWN"))
        .withPosition(5, 0)
        .withSize(2, 1);

    // Sanity check indicator - if pose is outside field bounds or spinning wildly
    tab.addBoolean("POSE SANE?", this::isPoseSane)
        .withWidget(BuiltInWidgets.kBooleanBox)
        .withProperties(Map.of("colorWhenTrue", "#00FF00", "colorWhenFalse", "#FF0000"))
        .withPosition(7, 0)
        .withSize(2, 1);

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 1-3: FIELD VISUALIZATION (THE KEY DIAGNOSTIC TOOL!)
    // ─────────────────────────────────────────────────────────────────────────
    // This shows where the robot thinks it is on the field.
    // If vision is misconfigured, you'll see the robot:
    // - On the wrong side of the field
    // - Spinning in circles
    // - Jumping to impossible positions

    tab.add("Field View", visionCalField)
        .withWidget(BuiltInWidgets.kField)
        .withPosition(0, 1)
        .withSize(5, 3);

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 1-2: POSE COMPARISON (Fused vs Odometry-Only)
    // ─────────────────────────────────────────────────────────────────────────
    // Compare these to see if vision is helping or hurting!
    // If they're very different, something may be wrong.

    ShuffleboardLayout fusedPoseLayout = tab.getLayout("Fused Pose (Vision+Odom)", BuiltInLayouts.kList)
        .withPosition(5, 1)
        .withSize(2, 2)
        .withProperties(Map.of("Label position", "LEFT"));

    fusedPoseLayout.addNumber("X (m)", () ->
        round2(driveSubsystem.getCurrentPose().getX()));
    fusedPoseLayout.addNumber("Y (m)", () ->
        round2(driveSubsystem.getCurrentPose().getY()));
    fusedPoseLayout.addNumber("Heading (°)", () ->
        round1(driveSubsystem.getCurrentPose().getRotation().getDegrees()));

    ShuffleboardLayout odomPoseLayout = tab.getLayout("Odometry Only", BuiltInLayouts.kList)
        .withPosition(7, 1)
        .withSize(2, 2)
        .withProperties(Map.of("Label position", "LEFT"));

    // Note: We read from SmartDashboard where DriveSubsystem logs odometry
    odomPoseLayout.addString("Compare", () -> {
      // Calculate difference between fused and odometry
      Pose2d fused = driveSubsystem.getCurrentPose();
      double fusedX = fused.getX();
      double fusedY = fused.getY();
      // Get distance difference (simple diagnostic)
      double dist = Math.sqrt(fusedX * fusedX + fusedY * fusedY);
      return String.format("Dist: %.2fm", dist);
    });
    odomPoseLayout.addNumber("Gyro Rate (°/s)", driveSubsystem::getGyroRate);
    odomPoseLayout.addBoolean("Gyro OK", () -> Math.abs(driveSubsystem.getGyroRate()) < 360);

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 3: POSE SANITY CHECKS
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout sanityLayout = tab.getLayout("Sanity Checks", BuiltInLayouts.kList)
        .withPosition(5, 3)
        .withSize(4, 1)
        .withProperties(Map.of("Label position", "LEFT"));

    sanityLayout.addBoolean("In Field Bounds", this::isInFieldBounds);
    sanityLayout.addBoolean("Not Spinning Wild", () -> Math.abs(driveSubsystem.getGyroRate()) < 720);

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 4-5: FRONT CAMERA DETAILS
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout frontLayout = tab.getLayout("Front Camera", BuiltInLayouts.kList)
        .withPosition(0, 4)
        .withSize(3, 2)
        .withProperties(Map.of("Label position", "LEFT"));

    frontLayout.addBoolean("Connected", () ->
        SmartDashboard.getBoolean(frontKey + "/Connected", false));
    frontLayout.addBoolean("Has Targets", () ->
        SmartDashboard.getBoolean(frontKey + "/HasTargets", false));
    frontLayout.addNumber("Target Count", () ->
        SmartDashboard.getNumber(frontKey + "/TargetCount", 0));
    frontLayout.addString("Tag IDs", () ->
        SmartDashboard.getString(frontKey + "/TagIDs", "None"));
    frontLayout.addString("Tag Distances", () ->
        SmartDashboard.getString(frontKey + "/TagDistances", "None"));
    frontLayout.addNumber("Latency (ms)", () ->
        SmartDashboard.getNumber(frontKey + "/LatencyMs", 0));
    frontLayout.addNumber("Ambiguity", () ->
        SmartDashboard.getNumber(frontKey + "/BestAmbiguity", 0));

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 4-5: BACK CAMERA DETAILS
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout backLayout = tab.getLayout("Back Camera", BuiltInLayouts.kList)
        .withPosition(3, 4)
        .withSize(3, 2)
        .withProperties(Map.of("Label position", "LEFT"));

    backLayout.addBoolean("Connected", () ->
        SmartDashboard.getBoolean(backKey + "/Connected", false));
    backLayout.addBoolean("Has Targets", () ->
        SmartDashboard.getBoolean(backKey + "/HasTargets", false));
    backLayout.addNumber("Target Count", () ->
        SmartDashboard.getNumber(backKey + "/TargetCount", 0));
    backLayout.addString("Tag IDs", () ->
        SmartDashboard.getString(backKey + "/TagIDs", "None"));
    backLayout.addString("Tag Distances", () ->
        SmartDashboard.getString(backKey + "/TagDistances", "None"));
    backLayout.addNumber("Latency (ms)", () ->
        SmartDashboard.getNumber(backKey + "/LatencyMs", 0));
    backLayout.addNumber("Ambiguity", () ->
        SmartDashboard.getNumber(backKey + "/BestAmbiguity", 0));

    // ─────────────────────────────────────────────────────────────────────────
    // ROW 4-5: BEST TARGET & CONTROLS
    // ─────────────────────────────────────────────────────────────────────────
    ShuffleboardLayout bestLayout = tab.getLayout("Best Target", BuiltInLayouts.kList)
        .withPosition(6, 4)
        .withSize(2, 2)
        .withProperties(Map.of("Label position", "LEFT"));

    bestLayout.addNumber("Tag ID", () ->
        SmartDashboard.getNumber("Vision/BestTarget/TagID", -1));
    bestLayout.addNumber("Ambiguity", () ->
        SmartDashboard.getNumber("Vision/BestTarget/Ambiguity", 0));
    bestLayout.addString("Camera", () ->
        SmartDashboard.getString("Vision/BestTarget/Camera", "None"));
    bestLayout.addNumber("Failures", () ->
        SmartDashboard.getNumber("Vision/ConsecutiveFailures", 0));

    // Commands & Reset Status
    tab.add("Snap to Vision", DriveCommands.forceVisionReset(driveSubsystem))
        .withPosition(8, 4)
        .withSize(2, 1);

    tab.addString("Reset Status", () ->
        SmartDashboard.getString("Vision/LastResetStatus", "Not attempted"))
        .withPosition(8, 5)
        .withSize(2, 1);

    tab.addString("PhotonVision", () -> "photonvision.local:5800")
        .withPosition(8, 6)
        .withSize(2, 1);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VISION CAL HELPER METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Checks if the current pose is within reasonable field bounds.
   * FRC field is approximately 16.5m x 8.0m.
   * Returns false if robot thinks it's outside the field (bad vision!).
   */
  private boolean isInFieldBounds() {
    Pose2d pose = driveSubsystem.getCurrentPose();
    double x = pose.getX();
    double y = pose.getY();
    // Allow some margin for measurement error
    return x >= -1.0 && x <= 17.5 && y >= -1.0 && y <= 9.0;
  }

  /**
   * Overall sanity check for pose.
   * Returns false if:
   * - Pose is outside field bounds
   * - Robot is spinning impossibly fast (bad vision causing jumps)
   */
  private boolean isPoseSane() {
    return isInFieldBounds() && Math.abs(driveSubsystem.getGyroRate()) < 720;
  }

  /** Round to 2 decimal places for display. */
  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  /** Round to 1 decimal place for display. */
  private double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PERIODIC UPDATE
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Call this from Robot.robotPeriodic() to sync toggle switches and update visualizations.
   *
   * <p>This bridges the Shuffleboard toggle switches to the SmartDashboard values
   * that VisionSubsystem reads, and updates the Field2d visualization.
   */
  public void periodic() {
    // Sync Vision Enabled toggle
    if (visionEnabledEntry != null) {
      SmartDashboard.putBoolean("Vision/Enabled", visionEnabledEntry.getBoolean(true));
    }

    // Sync Classroom Mode toggle
    if (classroomModeEntry != null) {
      SmartDashboard.putBoolean("Vision/ClassroomMode", classroomModeEntry.getBoolean(false));
    }

    // Update Field2d visualization for Vision Cal tab
    // This shows where the robot thinks it is - essential for catching bad camera transforms!
    visionCalField.setRobotPose(driveSubsystem.getCurrentPose());
  }
}
