// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.config;

import java.security.SecurityPermission;
import java.util.Set;

import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.commands.PathPlannerAuto;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.constants.FuelConstants;
import frc.robot.subsystems.CANFuelSubsystem;

/**
 * Sets up autonomous routine selector for dashboard.
 *
 * PathPlanner paths go in deploy/pathplanner/
 */
public class AutoSelector {

  private final SendableChooser<Command> autoChooser;
  private final DriveSubsystem drive;
  private final CANFuelSubsystem fuel;

  /**
   * IMPORTANT: Call registerNamedCommands(drive, fuel) BEFORE creating this!
   */
  public AutoSelector(DriveSubsystem drive, CANFuelSubsystem fuel) {
    this.drive = drive;
    this.fuel = fuel;
    this.autoChooser = createAutoChooser();
  }

  public SendableChooser<Command> getChooser() {
    return autoChooser;
  }

  public Command getSelectedAuto() {
    return autoChooser.getSelected();
  }

  private SendableChooser<Command> createAutoChooser() {
    SendableChooser<Command> chooser = new SendableChooser<>();

  // Wrap PathPlannerAuto in Commands.defer() to create fresh command each time
  chooser.addOption("Shoot Gather Shoot",
      Commands.defer(() -> new PathPlannerAuto("Shoot Gather Shoot"), Set.of()));
  chooser.addOption("Right Side Spin",
      Commands.defer(() -> new PathPlannerAuto("Right-Side-Spin"), Set.of()));
  chooser.addOption("Right Hub Shoot",
      Commands.defer(() -> new PathPlannerAuto("Right Hub Shoot"), Set.of()));
  chooser.addOption("Center Shoot",
      Commands.defer(() -> new PathPlannerAuto("Center Shoot"), Set.of()));
  chooser.addOption("Outpost",
      Commands.defer(() -> new PathPlannerAuto("Outpost"), Set.of()));


  chooser.setDefaultOption("Do Nothing", Commands.none());

    return chooser;
  }

  private Command waitForSeconds(int seconds)
  {
    return new WaitCommand(seconds);
  }

  private Command createDriveDistanceCommand(double xSpeed, double ySpeed, int feet, int inches) {
    double distanceMeters = Units.feetToMeters(feet) + Units.inchesToMeters(inches);

    return Commands.defer(() -> {
      Pose2d startPose = drive.getCurrentPose();

      return Commands.runEnd(
        () -> drive.drive(xSpeed, ySpeed, 0, false),
        () -> drive.drive(0, 0, 0, false),
        drive
      ).until(() -> {
        double distanceTraveled = drive.getCurrentPose().getTranslation()
            .getDistance(startPose.getTranslation());
        return distanceTraveled >= distanceMeters;
      });
    }, Set.of(drive)).withTimeout(5.0);
  }

  /**
   * Register named commands for PathPlanner paths.
   * Call this BEFORE creating AutoSelector.
   */
  public static void registerNamedCommands(CANFuelSubsystem fuel) {
    NamedCommands.registerCommand("Shoot", fuel.launchCommand());
    NamedCommands.registerCommand("Intake", fuel.intakeCommand());
    NamedCommands.registerCommand("Eject", fuel.ejectCommand());
    NamedCommands.registerCommand("Stop", Commands.runOnce(() -> fuel.stop(), fuel));
  }
}
