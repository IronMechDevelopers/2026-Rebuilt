// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.commands.DriveCommands;
import frc.robot.config.AutoSelector;
import frc.robot.config.ButtonBindings;
import frc.robot.config.DashboardSetup;
import frc.robot.config.SubsystemSetup;
import frc.robot.config.VisionSetup;
import frc.robot.constants.DriveConstants;
import frc.robot.constants.HardwareConstants;
import frc.robot.subsystems.CANFuelSubsystem;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.MatchStateTracker;
import frc.robot.subsystems.VisionProvider;

/**
 * =====================================================================══════
 *                           ROBOT CONTAINER
 * =====================================================================══════
 *
 * This file wires everything together. It's the "main" setup for the robot.
 *
 * WHAT THIS FILE DOES:
 *   - Creates subsystems (drive, vision, etc.)
 *   - Creates controllers (joysticks)
 *   - Calls the setup files to configure everything
 *
 * WHERE TO FIND THINGS:
 *   - Button mappings: config/ButtonBindings.java
 *   - Auto routines: config/AutoSelector.java
 *   - Dashboard setup: config/DashboardSetup.java
 *   - Vision cameras: config/VisionSetup.java
 *   - New subsystems: config/SubsystemSetup.java
 *
 * Controller Ports:
 *   - 0: Driver controller (Xbox)
 *   - 1: Co-driver controller (Xbox)
 */
public class RobotContainer {

  // SUBSYSTEMS

  /** Vision provider - Created by VisionSetup (uses SimVisionProvider in simulation) */
  private final VisionProvider vision = VisionSetup.createVisionProvider();

  /** Drive subsystem - Created by SubsystemSetup */
  private final DriveSubsystem drive = SubsystemSetup.createDriveSubsystem(vision);

  private final CANFuelSubsystem ballSubsystem = new CANFuelSubsystem();

  /** Match state tracker - Tracks hub status and shift timing for 2026 REBUILT */
  private final MatchStateTracker matchState = new MatchStateTracker();


  // CONTROLLERS

  /** Driver Xbox controller */
  private final CommandXboxController driver = new CommandXboxController(HardwareConstants.kDriverControllerPort);

  /** Co-driver Xbox controller */
  private final CommandXboxController coDriver = new CommandXboxController(HardwareConstants.kCoDriverControllerPort);

  // =========================================================================
  // CONFIGURATION OBJECTS
  // =========================================================================

  /** Auto selector - Manages autonomous routines */
  private final AutoSelector autoSelector;

  /** Dashboard setup - Manages Shuffleboard tabs */
  private DashboardSetup dashboardSetup;

  // =========================================================================
  // TEST TARGET (for vision testing)
  // =========================================================================

  /** Test target position - Updated via dashboard */
  private Pose2d testTarget = new Pose2d(
    DriveConstants.kDefaultTestTargetX,
    DriveConstants.kDefaultTestTargetY,
    Rotation2d.fromDegrees(DriveConstants.kDefaultTestTargetHeadingDegrees)
  );

  // =========================================================================
  // CONSTRUCTOR
  // =========================================================================

  /**
   * Creates robot container and configures everything.
   */
  public RobotContainer() {
    // =====================================================================
    // STEP 1: Register PathPlanner named commands (MUST be first!)
    // =====================================================================
    AutoSelector.registerNamedCommands();

    // =====================================================================
    // STEP 2: Create auto selector
    // =====================================================================
    autoSelector = new AutoSelector();

    // =====================================================================
    // STEP 3: Set default drive command
    // =====================================================================
    drive.setDefaultCommand(
      DriveCommands.joystickDrive(
        drive,
        () -> -driver.getLeftY(),   // Forward/backward
        () -> -driver.getLeftX(),   // Left/right
        () -> -driver.getRightX()   // Rotation
      )
    );

    // =====================================================================
    // STEP 4: Configure button bindings (see config/ButtonBindings.java)
    // =====================================================================
    ButtonBindings buttonBindings = new ButtonBindings(
        drive,
        ballSubsystem,
        driver,
        coDriver,
        this::getTestTarget,
        this::setTestTarget
    );
    buttonBindings.configureAll();

    // =====================================================================
    // STEP 5: Configure dashboard (see config/DashboardSetup.java)
    // =====================================================================
    dashboardSetup = new DashboardSetup(drive, matchState, autoSelector.getChooser());
    dashboardSetup.configureAll();

    // =====================================================================
    // STEP 6: Publish test target to dashboard
    // =====================================================================
    SmartDashboard.putNumber("Test/TargetX", testTarget.getX());
    SmartDashboard.putNumber("Test/TargetY", testTarget.getY());
    SmartDashboard.putNumber("Test/TargetHeading", testTarget.getRotation().getDegrees());
    SmartDashboard.putNumber("Test/DistanceFromTag", DriveConstants.kDefaultDistanceFromTag);

    // =====================================================================
    // STEP 7: Initialize vision mode
    // =====================================================================
    SmartDashboard.setDefaultBoolean("Vision/ClassroomMode", false);
  }

  // =========================================================================
  // ACCESSORS
  // =========================================================================

  /**
   * Gets selected autonomous command (called by Robot.java).
   *
   * @return Selected auto command or null
   */
  public Command getAutonomousCommand() {
    return autoSelector.getSelectedAuto();
  }

  /**
   * Gets drive subsystem instance.
   *
   * @return Drive subsystem
   */
  public DriveSubsystem getDriveSubsystem() {
    return drive;
  }

  /**
   * Gets match state tracker instance.
   *
   * @return Match state tracker
   */
  public MatchStateTracker getMatchStateTracker() {
    return matchState;
  }

  /**
   * Updates dashboard periodic tasks.
   * Call this from Robot.robotPeriodic().
   */
  public void dashboardPeriodic() {
    if (dashboardSetup != null) {
      dashboardSetup.periodic();
    }

    // Log inputs and match context for AdvantageScope replay
    logInputsAndContext();
  }

  /**
   * Updates simulation physics.
   * Call this from Robot.simulationPeriodic().
   */
  public void simulationPeriodic() {
    drive.simulationPeriodic();
  }

  // =========================================================================
  // ADVANTAGEKIT LOGGING - For replay and debugging
  // =========================================================================
  //
  // These inputs are logged every cycle so AdvantageScope can replay
  // exactly what the driver did during a match.
  //
  // ADVANTAGESCOPE USAGE:
  //   - Inputs/Driver* shows raw joystick values
  //   - Match/* shows match state and timing
  //   - Commands/* shows what code was running
  //   - CAN/* shows hardware bus health
  //
  private void logInputsAndContext() {
    // =====================================================================══
    // DRIVER INPUTS - Critical for replay!
    // =====================================================================══
    // These are the raw joystick values BEFORE deadband/processing
    // Essential for replaying exactly what the driver did


    // =====================================================================══
    // MATCH CONTEXT - Know when things happened
    // =====================================================================══

    Logger.recordOutput("Match/TimeRemaining", DriverStation.getMatchTime());
    Logger.recordOutput("Match/IsEnabled", DriverStation.isEnabled());
    Logger.recordOutput("Match/IsAutonomous", DriverStation.isAutonomousEnabled());
    Logger.recordOutput("Match/IsTeleop", DriverStation.isTeleopEnabled());
    Logger.recordOutput("Match/IsTest", DriverStation.isTestEnabled());
    Logger.recordOutput("Match/IsDSAttached", DriverStation.isDSAttached());
    Logger.recordOutput("Match/IsFMSAttached", DriverStation.isFMSAttached());

    var alliance = DriverStation.getAlliance();
    Logger.recordOutput("Match/Alliance", alliance.isPresent() ? alliance.get().name() : "Unknown");
    Logger.recordOutput("Match/MatchNumber", DriverStation.getMatchNumber());
    Logger.recordOutput("Match/EventName", DriverStation.getEventName());

    // =====================================================================══
    // COMMAND STATE - Know what code was running
    // =====================================================================══

    // Log the current command running on the drive subsystem
    Command currentDriveCommand = drive.getCurrentCommand();
    Logger.recordOutput("Commands/DriveCommand",
        currentDriveCommand != null ? currentDriveCommand.getName() : "None");
    Logger.recordOutput("Commands/IsDefaultCommand",
        currentDriveCommand != null &&
        currentDriveCommand.equals(drive.getDefaultCommand()));

    // =====================================================================══
    // CAN BUS HEALTH - Catch hardware issues
    // =====================================================================══

    var canStatus = RobotController.getCANStatus();
    Logger.recordOutput("CAN/PercentUtilization", canStatus.percentBusUtilization);
    Logger.recordOutput("CAN/BusOffCount", canStatus.busOffCount);
    Logger.recordOutput("CAN/TxFullCount", canStatus.txFullCount);
    Logger.recordOutput("CAN/ReceiveErrorCount", canStatus.receiveErrorCount);
    Logger.recordOutput("CAN/TransmitErrorCount", canStatus.transmitErrorCount);
  }

  /**
   * Gets current test target position.
   *
   * @return Test target pose
   */
  private Pose2d getTestTarget() {
    return testTarget;
  }

  /**
   * Sets test target position.
   *
   * @param target New test target
   */
  private void setTestTarget(Pose2d target) {
    this.testTarget = target;
  }
}
