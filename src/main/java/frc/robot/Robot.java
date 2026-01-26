// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

/**
 * The methods in this class are called automatically corresponding to each mode, as described in
 * the TimedRobot documentation.
 *
 * This robot uses AdvantageKit for structured logging and replay capability.
 * All telemetry is logged via Logger.recordOutput() for AdvantageScope visualization.
 */
public class Robot extends LoggedRobot {
  private Command autonomousCommand;
  private final RobotContainer robotContainer;

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  public Robot() {
    // ═══════════════════════════════════════════════════════════════════════
    // ADVANTAGEKIT LOGGER SETUP
    // ═══════════════════════════════════════════════════════════════════════

    // Record metadata
    // TODO: Update these values when cloning template for new season
    Logger.recordMetadata("ProjectName", "FRC-Swerve-5684");
    Logger.recordMetadata("Team", "Team 5684");
    Logger.recordMetadata("Game", "2026 REBUILT");
    Logger.recordMetadata("RobotType", "Swerve Drive");

    if (isReal()) {
      // ═════════════════════════════════════════════════════════════════════
      // REAL ROBOT MODE - Log to USB stick and NetworkTables
      // ═════════════════════════════════════════════════════════════════════
      // Logs saved to: /U/logs/ (USB stick)
      // Also streams to AdvantageScope via NetworkTables for live viewing
      Logger.addDataReceiver(new WPILOGWriter()); // Log to USB
      Logger.addDataReceiver(new NT4Publisher()); // Stream to NetworkTables

    } else {
      // ═════════════════════════════════════════════════════════════════════
      // SIMULATION MODE - Just stream to NetworkTables (no blocking prompt)
      // ═════════════════════════════════════════════════════════════════════
      // Note: For replay, use AdvantageScope to open a log file directly
      // or set AKIT_LOG_PATH environment variable before launching
      Logger.addDataReceiver(new NT4Publisher());
    }

    // Start logging! Must be called after adding receivers
    Logger.start();

    // Instantiate our RobotContainer. This will perform all our button bindings, and put our
    // autonomous chooser on the dashboard.
    robotContainer = new RobotContainer();
  }

  /**
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before LiveWindow and
   * SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
    // Runs the Scheduler.  This is responsible for polling buttons, adding newly-scheduled
    // commands, running already-scheduled commands, removing finished or interrupted commands,
    // and running subsystem periodic() methods.  This must be called from the robot's periodic
    // block in order for anything in the Command-based framework to work.
    CommandScheduler.getInstance().run();

    // Update dashboard (syncs toggle switches to SmartDashboard)
    robotContainer.dashboardPeriodic();
  }

  /** This function is called once each time the robot enters Disabled mode. */
  @Override
  public void disabledInit() {}

  @Override
  public void disabledPeriodic() {}

  /** This autonomous runs the autonomous command selected by your {@link RobotContainer} class. */
  @Override
  public void autonomousInit() {
    autonomousCommand = robotContainer.getAutonomousCommand();

    // schedule the autonomous command (example)
    if (autonomousCommand != null) {
      CommandScheduler.getInstance().schedule(autonomousCommand);
    }
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {}

  @Override
  public void teleopInit() {
    // This makes sure that the autonomous stops running when
    // teleop starts running. If you want the autonomous to
    // continue until interrupted by another command, remove
    // this line or comment it out.
    if (autonomousCommand != null) {
      autonomousCommand.cancel();
    }
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {}

  @Override
  public void testInit() {
    // Cancels all running commands at the start of test mode.
    CommandScheduler.getInstance().cancelAll();
  }

  /** This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {}

  /** This function is called once when the robot is first started up. */
  @Override
  public void simulationInit() {}

  /** This function is called periodically whilst in simulation. */
  @Override
  public void simulationPeriodic() {}
}
