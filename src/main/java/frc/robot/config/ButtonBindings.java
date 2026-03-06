// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import frc.robot.commands.DriveCommands;
import frc.robot.constants.DriveConstants;
import frc.robot.constants.FuelConstants;
import frc.robot.subsystems.CANFuelSubsystem;
import frc.robot.subsystems.DriveSubsystem;

/**
 * =====================================================================══════
 *                            BUTTON BINDINGS
 * =====================================================================══════
 *
 * This file sets up what each button on the controllers does.
 *
 * CONTROLLER LAYOUT:
 *
 *   DRIVER (Xbox Controller):
 *     Left Stick Click: Gyro reset (zero heading)
 *     A Button: Hub lock - Aim at hub then X-stance (toggle)
 *     B Button: Robot relative mode (toggle field-centric)
 *     X Button: X-Stance (toggle)
 *     Y Button: Precision mode (50% speed, hold)
 *     Left Trigger: Intake (hold)
 *     Left Bumper: Eject (hold)
 *     Right Trigger: Shoot (hold)
 *     Right Bumper: Diamond snap (45° angles, hold)
 *     POV Up: Sniper mode - Auto-aim at hub while strafing (toggle)
 *
 *   CO-DRIVER (Xbox Controller):
 *     A Button: Shoot (hold)
 *     B Button: Eject (hold)
 *     X Button: Intake (hold)
 *     Y Button: Square snap (90° angles, hold)
 *
 * HOW TO ADD A NEW BUTTON:
 *   1. Find the button you want to use (see layout above)
 *   2. Add a binding in configureDriverBindings() or configureCoDriverBindings()
 *   3. Look at existing examples for the pattern to follow
 *
 * Related files:
 *   - DriveCommands.java: Pre-made commands you can use
 *   - DriveSubsystem.java: The drive system these commands control
 */
public class ButtonBindings {

  // Controllers
  private final CommandXboxController driver;
  private final CommandXboxController coDriver;

  // Subsystems
  private final DriveSubsystem driveSubsystem;
  private final CANFuelSubsystem ballSubsystem;

  // Test target (for vision testing)
  private final Supplier<Pose2d> testTargetSupplier;
  private final Consumer<Pose2d> testTargetUpdater;

  // =========================================================================
  // CONSTRUCTOR
  // =========================================================================

  /**
   * Creates button bindings.
   *
   * @param driveSubsystem The drive subsystem
   * @param ballSubsystem The fuel subsystem
   * @param driver Driver's Xbox controller
   * @param coDriver Co-driver's Xbox controller
   * @param testTargetSupplier Gets the current test target
   * @param testTargetUpdater Updates the test target
   */
  public ButtonBindings(
      DriveSubsystem driveSubsystem,
      CANFuelSubsystem ballSubsystem,
      CommandXboxController driver,
      CommandXboxController coDriver,
      Supplier<Pose2d> testTargetSupplier,
      Consumer<Pose2d> testTargetUpdater) {
    this.driveSubsystem = driveSubsystem;
    this.ballSubsystem=ballSubsystem;
    this.driver=driver;
    this.coDriver = coDriver;
    this.testTargetSupplier = testTargetSupplier;
    this.testTargetUpdater = testTargetUpdater;
  }

  // =========================================================================
  // CONFIGURE ALL BINDINGS
  // =========================================================================

  /**
   * Configures all button bindings.
   *
   * <p>Call this once from RobotContainer after creating all subsystems.
   */
  public void configureAll() {
    configureDriverBindings();
    configureCoDriverBindings();
  }

  // =========================================================================
  // DRIVER CONTROLS (Thrustmaster Joysticks)
  // =========================================================================

  /**
   * Configures driver Xbox controller buttons.
   */
  private void configureDriverBindings() {
    // Left stick click: Gyro reset (zero heading)
    driver.leftStick().onTrue(DriveCommands.zeroHeading(driveSubsystem));

    // Y button: Precision mode (50% speed while held)
    driver.y()
        .onTrue(DriveCommands.setSpeed(driveSubsystem, 0.5))
        .onFalse(DriveCommands.setSpeed(driveSubsystem, 1.0));

    // B button: Toggle robot-relative mode
    driver.b().onTrue(DriveCommands.toggleFieldRelative(driveSubsystem));

    // X button: Toggle X-Stance
    driver.x().toggleOnTrue(DriveCommands.xStance(driveSubsystem));

    // Left trigger: Intake (hold)
    driver.leftTrigger()
        .whileTrue(ballSubsystem.runEnd(() -> ballSubsystem.intake(), () -> ballSubsystem.stop()));

    // Left bumper: Eject (hold)
    driver.leftBumper()
        .whileTrue(ballSubsystem.runEnd(() -> ballSubsystem.eject(), () -> ballSubsystem.stop()));

    // Right trigger: Shoot (hold - spin up for 1s then launch)
    driver.rightTrigger()
        .whileTrue(ballSubsystem.spinUpCommand().withTimeout(FuelConstants.SPIN_UP_SECONDS)
            .andThen(ballSubsystem.launchCommand())
            .finallyDo(() -> ballSubsystem.stop()));

    // Right bumper: Diamond snap (45° angles while held)
    driver.rightBumper()
        .whileTrue(DriveCommands.snapToDiamond(
            driveSubsystem,
            () -> -driver.getLeftY(),
            () -> -driver.getLeftX()));

    // POV Up: Sniper mode (auto-aim at hub while allowing strafing, toggle)
    driver.povUp()
        .toggleOnTrue(DriveCommands.driveWhileAimingAtHub(
            driveSubsystem,
            () -> -driver.getLeftY(),
            () -> -driver.getLeftX()));

    // A button: Hub lock (aim at hub then X-stance, toggle)
    driver.a()
        .toggleOnTrue(
            DriveCommands.aimAtHub(driveSubsystem)
                .andThen(DriveCommands.xStance(driveSubsystem)));
  }



  // =========================================================================
  // CO-DRIVER CONTROLS (Xbox Controller)
  // =========================================================================

  /**
   * Configures co-driver Xbox controller buttons.
   */
  private void configureCoDriverBindings() {
    // X button: Intake (hold)
    coDriver.x()
        .whileTrue(ballSubsystem.runEnd(() -> ballSubsystem.intake(), () -> ballSubsystem.stop()));

    // B button: Eject (hold)
    coDriver.b()
        .whileTrue(ballSubsystem.runEnd(() -> ballSubsystem.eject(), () -> ballSubsystem.stop()));

    // A button: Shoot (hold - spin up for 1s then launch)
    coDriver.a()
        .whileTrue(ballSubsystem.spinUpCommand().withTimeout(FuelConstants.SPIN_UP_SECONDS)
            .andThen(ballSubsystem.launchCommand())
            .finallyDo(() -> ballSubsystem.stop()));

    // Y button: Square snap (90° angles while held)
    coDriver.y()
        .whileTrue(DriveCommands.snapToClosestCardinal(
            driveSubsystem,
            () -> -driver.getLeftY(),
            () -> -driver.getLeftX()));
  }
}
