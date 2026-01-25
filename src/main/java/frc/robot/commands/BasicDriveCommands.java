// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import java.util.function.DoubleSupplier;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.constants.DriveConstants;
import frc.robot.subsystems.DriveSubsystem;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *                         BASIC DRIVE COMMANDS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Commands for everyday driving - joystick control and speed settings.
 *
 * WHAT'S IN THIS FILE:
 *   - joystickDrive: The main teleop driving command
 *   - Speed controls: full, half, quarter, toggle
 *   - Field-relative toggle
 *
 * HOW JOYSTICK DRIVING WORKS:
 *   1. Read joystick values (-1.0 to 1.0)
 *   2. Apply deadband (ignore tiny movements)
 *   3. Square the input (makes small movements more precise)
 *   4. Limit acceleration (prevents tipping)
 *   5. Flip for alliance color (so "forward" is always away from your driver station)
 *   6. Apply speed multiplier
 *   7. Send to drive subsystem
 *
 * Related files:
 *   - VisionDriveCommands.java: Auto-aim and pathfinding
 *   - UtilityCommands.java: X-stance, zero heading, etc.
 */
public class BasicDriveCommands {

  // Private constructor - use static methods only
  private BasicDriveCommands() {}

  // =========================================================================
  // JOYSTICK DRIVING
  // =========================================================================

  /**
   * Standard teleop driving command.
   *
   * <p>This is the main command that runs during teleop. It reads the joysticks
   * and makes the robot drive.
   *
   * @param drive The drive subsystem
   * @param xInput Forward/backward input (-1.0 to 1.0, negative = forward)
   * @param yInput Left/right input (-1.0 to 1.0, negative = left)
   * @param rotInput Rotation input (-1.0 to 1.0, negative = counterclockwise)
   * @return The driving command
   */
  public static Command joystickDrive(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput,
      DoubleSupplier rotInput) {

    // Slew rate limiters prevent sudden acceleration (which can tip the robot)
    SlewRateLimiter xLimiter = new SlewRateLimiter(DriveConstants.kTranslationSlewRate);
    SlewRateLimiter yLimiter = new SlewRateLimiter(DriveConstants.kTranslationSlewRate);
    SlewRateLimiter rotLimiter = new SlewRateLimiter(DriveConstants.kRotationSlewRate);

    return Commands.run(() -> {
      // Step 1: Apply deadband (ignore tiny joystick movements)
      double x = MathUtil.applyDeadband(xInput.getAsDouble(), DriveConstants.kJoystickDeadband);
      double y = MathUtil.applyDeadband(yInput.getAsDouble(), DriveConstants.kJoystickDeadband);
      double rot = MathUtil.applyDeadband(rotInput.getAsDouble(), DriveConstants.kJoystickDeadband);

      // Step 2: Square inputs (makes small movements more precise)
      x = Math.copySign(x * x, x);
      y = Math.copySign(y * y, y);
      rot = Math.copySign(rot * rot, rot);

      // Step 3: Limit acceleration
      x = xLimiter.calculate(x);
      y = yLimiter.calculate(y);
      rot = rotLimiter.calculate(rot);

      // Step 4: Flip for red alliance (so forward is always away from driver station)
      var alliance = DriverStation.getAlliance();
      if (alliance.isPresent() && alliance.get() == Alliance.Red) {
        x = -x;
        y = -y;
      }

      // Step 5: Apply speed multiplier and drive
      double multiplier = drive.getSpeedMultiplier();
      drive.drive(
          x * DriveConstants.kMaxSpeedMetersPerSecond * multiplier,
          y * DriveConstants.kMaxSpeedMetersPerSecond * multiplier,
          rot * DriveConstants.kMaxAngularSpeed * multiplier,
          drive.getFieldRelative()
      );
    }, drive);
  }

  // =========================================================================
  // SPEED CONTROL
  // =========================================================================

  /**
   * Sets the drive speed multiplier.
   *
   * @param drive The drive subsystem
   * @param multiplier Speed multiplier (0.0 to 1.0)
   * @return Command that sets the speed
   */
  public static Command setSpeed(DriveSubsystem drive, double multiplier) {
    return Commands.runOnce(() -> drive.setSpeedMultiplier(multiplier), drive);
  }

  /**
   * Toggles between full and half speed.
   */
  public static Command toggleSpeed(DriveSubsystem drive) {
    return Commands.runOnce(() -> {
      double current = drive.getSpeedMultiplier();
      drive.setSpeedMultiplier(current > DriveConstants.kSpeedToggleThreshold
          ? DriveConstants.kHalfSpeedMultiplier
          : DriveConstants.kFullSpeedMultiplier);
    }, drive);
  }

  // =========================================================================
  // FIELD-RELATIVE CONTROL
  // =========================================================================

  /**
   * Toggles field-relative driving on/off.
   *
   * <p>Field-relative: "forward" is always toward the opposite alliance wall.
   * <p>Robot-relative: "forward" is wherever the robot is pointing.
   */
  public static Command toggleFieldRelative(DriveSubsystem drive) {
    return Commands.runOnce(() -> drive.setFieldRelative(!drive.getFieldRelative()), drive);
  }
}
