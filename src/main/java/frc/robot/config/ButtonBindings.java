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
import edu.wpi.first.wpilibj2.command.button.CommandPS5Controller;
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
 *   DRIVER (Thrustmaster Joysticks):
 *     - Left stick: Move forward/back, left/right
 *     - Right stick X: Rotate
 *     - Right stick Button 1: Toggle speed (full/half)
 *     - Right stick Button 2: Zero heading
 *     - Right stick Button 3: EMERGENCY OVERRIDE
 *     - Left stick Button 1: Toggle field-relative
 *
 *   CO-DRIVER (PlayStation Controller):
 *     Face Buttons (Vision/Shooting):
 *       Triangle: Drive while aiming at hub (driver controls movement)
 *       Circle: Aim at hub → X-stance (locks driver out while held)
 *       Cross: Drive to test target
 *       Square: X-stance (safety lock)
 *
 *     Bumpers (Speed Control):
 *       L1: Switch to SLOW mode
 *       R1: Switch to FAST mode
 *
 *     Triggers (Rotation Snap):
 *       L2: Snap to cardinal (0/90/180/270)
 *       R2: Snap to diamond (45/135/225/315)
 *
 *     Utilities:
 *       Touchpad: Force vision reset
 *       Options: Reload test target from dashboard
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
  private final CommandPS5Controller coDriver;

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
   * @param driverLeftStick Driver's left joystick
   * @param driverRightStick Driver's right joystick
   * @param coDriver Co-driver's PS5 controller
   * @param testTargetSupplier Gets the current test target
   * @param testTargetUpdater Updates the test target
   */
  public ButtonBindings(
      DriveSubsystem driveSubsystem,
      CANFuelSubsystem ballSubsystem,
      CommandXboxController driver,
      CommandPS5Controller coDriver,
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
   * Configures driver joystick buttons.
   */
  private void configureDriverBindings() {
    // Right stick button 1: Toggle speed (full/half)

    driver.povUp().onTrue(Commands.runOnce(() -> driveSubsystem.zeroHeading() ,driveSubsystem));
    
  }



  // =========================================================================
  // CO-DRIVER CONTROLS (PlayStation Controller)
  // =========================================================================

  /**
   * Configures co-driver PS5 controller buttons.
   */
  private void configureCoDriverBindings() {
    // =====================================================================
    // FACE BUTTONS - Vision Testing
    // =====================================================================



    // Circle button: Aim at hub, then hold X-stance (driver can't move while held)
    // coDriver.circle()
    //     .whileTrue(Commands.deferredProxy(() ->
    //         DriveCommands.aimAtHub(driveSubsystem)
    //             .andThen(DriveCommands.xStance(driveSubsystem))));

    // // Square button: X-stance (makes an X pattern with wheels)
    // coDriver.square()
    //     .whileTrue(DriveCommands.xStance(driveSubsystem));

      coDriver.square()
        .whileTrue(ballSubsystem.runEnd(() -> ballSubsystem.intake(), () -> ballSubsystem.stop()));

      coDriver.cross().whileTrue(ballSubsystem.spinUpCommand().withTimeout(FuelConstants.SPIN_UP_SECONDS)
            .andThen(ballSubsystem.launchCommand())
            .finallyDo(() -> ballSubsystem.stop()));

      coDriver.circle().whileTrue(ballSubsystem.runEnd(() -> ballSubsystem.eject(), () -> ballSubsystem.stop()));



    // =====================================================================
    // D-PAD - Reserved for future use
    // =====================================================================
    // Available for game-specific commands or preset speeds if needed
    // Example: coDriver.povUp().onTrue(intakeCommand);
  }
}
