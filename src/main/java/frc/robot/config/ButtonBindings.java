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
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import frc.robot.commands.DriveCommands;
import frc.robot.constants.DriveConstants;
import frc.robot.subsystems.DriveSubsystem;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *                            BUTTON BINDINGS
 * ═══════════════════════════════════════════════════════════════════════════
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
 *     Face Buttons (Vision Testing):
 *       Triangle: Heading lock test
 *       Circle: Auto-aim test
 *       Cross: Drive to distance test
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
  private final Joystick driverLeftStick;
  private final Joystick driverRightStick;
  private final CommandPS5Controller coDriver;

  // Subsystems
  private final DriveSubsystem driveSubsystem;

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
      Joystick driverLeftStick,
      Joystick driverRightStick,
      CommandPS5Controller coDriver,
      Supplier<Pose2d> testTargetSupplier,
      Consumer<Pose2d> testTargetUpdater) {
    this.driveSubsystem = driveSubsystem;
    this.driverLeftStick = driverLeftStick;
    this.driverRightStick = driverRightStick;
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
    new JoystickButton(driverRightStick, 1)
        .onTrue(DriveCommands.toggleSpeed(driveSubsystem));

    // Right stick button 2: Zero heading (face AWAY from driver station first!)
    new JoystickButton(driverRightStick, 2)
        .onTrue(DriveCommands.zeroHeading(driveSubsystem));

    // Right stick button 3: EMERGENCY OVERRIDE (cancels any running command)
    new JoystickButton(driverRightStick, 3)
        .onTrue(new Command() {
          {
            setName("EmergencyOverride");
            addRequirements(driveSubsystem);
          }

          @Override
          public void initialize() {
            System.out.println("EMERGENCY OVERRIDE - Driver has control");
          }

          @Override
          public boolean isFinished() {
            return true;
          }
        });

    // Left stick button 1: Toggle field-relative driving
    new JoystickButton(driverLeftStick, 1)
        .onTrue(DriveCommands.toggleFieldRelative(driveSubsystem));
  }

  // =========================================================================
  // CO-DRIVER CONTROLS (PlayStation Controller)
  // =========================================================================

  /**
   * Configures co-driver PS5 controller buttons.
   */
  private void configureCoDriverBindings() {
    // ═════════════════════════════════════════════════════════════════════
    // FACE BUTTONS - Vision Testing
    // ═════════════════════════════════════════════════════════════════════

    // Triangle button: Drive while aiming (driver controls translation, robot auto-rotates)
    coDriver.triangle()
        .whileTrue(Commands.deferredProxy(() ->
            DriveCommands.driveWhileAiming(
                driveSubsystem,
                () -> -driverLeftStick.getY(),
                () -> -driverLeftStick.getX(),
                testTargetSupplier.get(),
                0.0  // Aim with front of robot (0 degrees)
            )));

    // Circle button: Aim at target (rotate to face test target)
    coDriver.circle()
        .whileTrue(Commands.deferredProxy(() ->
            DriveCommands.aimAtTarget(driveSubsystem, testTargetSupplier.get())));

    // Square button: X-stance (makes an X pattern with wheels)
    coDriver.square()
        .whileTrue(DriveCommands.xStance(driveSubsystem));

    // Cross button: Drive to test target
    coDriver.cross()
        .whileTrue(Commands.deferredProxy(() ->
            DriveCommands.driveToPose(driveSubsystem, testTargetSupplier.get())));

    // ═════════════════════════════════════════════════════════════════════
    // BUMPERS - Speed Control
    // ═════════════════════════════════════════════════════════════════════

    // L1 button: Switch to SLOW mode (safe for practice)
    coDriver.L1()
        .onTrue(DriveCommands.setSpeed(driveSubsystem, DriveConstants.kHalfSpeedMultiplier));

    // R1 button: Switch to FAST mode (when confident)
    coDriver.R1()
        .onTrue(DriveCommands.setSpeed(driveSubsystem, DriveConstants.kFullSpeedMultiplier));

    // ═════════════════════════════════════════════════════════════════════
    // TRIGGERS - Rotation Snap
    // ═════════════════════════════════════════════════════════════════════

    // L2 trigger: Snap to cardinal angle (0/90/180/270 deg)
    coDriver.L2()
        .whileTrue(DriveCommands.snapToClosestCardinal(
            driveSubsystem,
            () -> -driverLeftStick.getY(),
            () -> -driverLeftStick.getX()
        ));

    // R2 trigger: Snap to diamond angle (45/135/225/315 deg)
    coDriver.R2()
        .whileTrue(DriveCommands.snapToDiamond(
            driveSubsystem,
            () -> -driverLeftStick.getY(),
            () -> -driverLeftStick.getX()
        ));

    // ═════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═════════════════════════════════════════════════════════════════════

    // Touchpad button: Force vision reset (snap odometry to AprilTag)
    coDriver.touchpad()
        .onTrue(DriveCommands.forceVisionReset(driveSubsystem));

    // Options button: Reload test target from dashboard
    coDriver.options()
        .onTrue(new Command() {
          {
            setName("UpdateTestTarget");
          }

          @Override
          public void initialize() {
            double x = SmartDashboard.getNumber("Test/TargetX", DriveConstants.kDefaultTestTargetX);
            double y = SmartDashboard.getNumber("Test/TargetY", DriveConstants.kDefaultTestTargetY);
            double heading = SmartDashboard.getNumber("Test/TargetHeading", DriveConstants.kDefaultTestTargetHeadingDegrees);
            Pose2d newTarget = new Pose2d(x, y, Rotation2d.fromDegrees(heading));
            testTargetUpdater.accept(newTarget);
            System.out.println("Test target updated: (" + x + ", " + y + ", " + heading + " deg)");
          }

          @Override
          public boolean isFinished() {
            return true;
          }
        }.ignoringDisable(true));

    // ═════════════════════════════════════════════════════════════════════
    // D-PAD - Reserved for future use
    // ═════════════════════════════════════════════════════════════════════
    // Available for game-specific commands or preset speeds if needed
    // Example: coDriver.povUp().onTrue(intakeCommand);
  }
}
