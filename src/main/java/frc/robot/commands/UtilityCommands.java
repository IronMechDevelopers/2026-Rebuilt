// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import java.util.function.DoubleSupplier;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.constants.DriveConstants;
import frc.robot.subsystems.DriveSubsystem;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *                          UTILITY COMMANDS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Miscellaneous utility commands for the drive subsystem.
 *
 * WHAT'S IN THIS FILE:
 *   - xStance: Lock wheels in X pattern (resists pushing)
 *   - setStraightAhead: Point all wheels forward (for alignment)
 *   - zeroHeading: Reset the gyro
 *   - forceVisionReset: Reset position to what vision sees
 *   - setCoastMode/setBrakeMode: Motor idle modes
 *   - Snap commands: Snap rotation to specific angles
 *
 * WHEN TO USE THESE:
 *   - xStance: When you don't want to be pushed (defense, end of match)
 *   - zeroHeading: At the start of a match, facing away from driver station
 *   - Coast/Brake mode: Coast for pushing robot around, Brake for competition
 *
 * Related files:
 *   - BasicDriveCommands.java: Normal joystick driving
 *   - VisionDriveCommands.java: Auto-aim and pathfinding
 */
public class UtilityCommands {

  // Private constructor - use static methods only
  private UtilityCommands() {}

  // =========================================================================
  // WHEEL POSITIONING
  // =========================================================================

  /**
   * Sets wheels to X formation to resist pushing.
   *
   * <p>Makes an X pattern with the wheels, which makes it very hard
   * for other robots to push you. Good for defense or end of match.
   *
   * @param drive The drive subsystem
   * @return Command that holds X stance
   */
  public static Command xStance(DriveSubsystem drive) {
    return Commands.run(drive::setX, drive);
  }

  /**
   * Sets all wheels to point straight ahead.
   *
   * <p>Useful for mechanical alignment checks - makes sure all wheels
   * are pointing the same direction.
   *
   * @param drive The drive subsystem
   * @return Command that sets wheels straight
   */
  public static Command setStraightAhead(DriveSubsystem drive) {
    return Commands.runOnce(drive::setStraightAhead, drive).ignoringDisable(true);
  }

  // =========================================================================
  // HEADING/GYRO
  // =========================================================================

  /**
   * Resets the gyro heading to zero.
   *
   * <p>Call this when the robot is facing AWAY from your driver station.
   * After this, "forward" in field-relative mode will be away from you.
   *
   * @param drive The drive subsystem
   * @return Command that zeros the heading
   */
  public static Command zeroHeading(DriveSubsystem drive) {
    return Commands.runOnce(drive::zeroHeading, drive);
  }

  /**
   * Forces the robot position to match what vision sees.
   *
   * <p>Use this if odometry has drifted and you want to snap back to
   * the vision-detected position.
   *
   * @param drive The drive subsystem
   * @return Command that resets to vision position
   */
  public static Command forceVisionReset(DriveSubsystem drive) {
    return Commands.runOnce(drive::forceVisionReset, drive);
  }

  // =========================================================================
  // MOTOR MODES
  // =========================================================================

  /**
   * Sets motors to coast mode (wheels spin freely when not driven).
   *
   * <p>Use this when you need to push the robot around by hand.
   *
   * @param drive The drive subsystem
   * @return Command that enables coast mode
   */
  public static Command setCoastMode(DriveSubsystem drive) {
    return Commands.runOnce(() -> drive.setMotorBrake(false), drive).ignoringDisable(true);
  }

  /**
   * Sets motors to brake mode (wheels resist movement when not driven).
   *
   * <p>Use this for competition - the robot will hold position better.
   *
   * @param drive The drive subsystem
   * @return Command that enables brake mode
   */
  public static Command setBrakeMode(DriveSubsystem drive) {
    return Commands.runOnce(() -> drive.setMotorBrake(true), drive).ignoringDisable(true);
  }

  // =========================================================================
  // ROTATION SNAP COMMANDS
  // =========================================================================

  /**
   * Snaps rotation to cardinal angles (0, 90, 180, 270 degrees).
   *
   * <p>While held, the robot will snap to the nearest 90-degree angle.
   * You can still drive translation normally.
   *
   * @param drive The drive subsystem
   * @param xInput Forward/backward joystick input
   * @param yInput Left/right joystick input
   * @return Command that snaps to cardinal angles
   */
  public static Command snapToClosestCardinal(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput) {
    return snapToClosestAngle(
        drive, xInput, yInput,
        DriveConstants.kCardinalSnapIncrementDegrees,
        DriveConstants.kCardinalSnapOffsetDegrees);
  }

  /**
   * Snaps rotation to diamond angles (45, 135, 225, 315 degrees).
   *
   * <p>Useful for cage climbing or other diagonal alignments.
   *
   * @param drive The drive subsystem
   * @param xInput Forward/backward joystick input
   * @param yInput Left/right joystick input
   * @return Command that snaps to diamond angles
   */
  public static Command snapToDiamond(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput) {
    return snapToClosestAngle(
        drive, xInput, yInput,
        DriveConstants.kCardinalSnapIncrementDegrees,
        DriveConstants.kDiamondSnapOffsetDegrees);
  }

  /**
   * Helper method for snap commands.
   *
   * <p>Rounds the current angle to the nearest increment + offset.
   */
  private static Command snapToClosestAngle(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput,
      double angleIncrementDegrees,
      double offsetDegrees) {

    PIDController turnPID = new PIDController(
        DriveConstants.kRotationPID.kP,
        DriveConstants.kRotationPID.kI,
        DriveConstants.kRotationPID.kD);
    turnPID.enableContinuousInput(-Math.PI, Math.PI);

    SlewRateLimiter xLimiter = new SlewRateLimiter(DriveConstants.kTranslationSlewRate);
    SlewRateLimiter yLimiter = new SlewRateLimiter(DriveConstants.kTranslationSlewRate);

    return Commands.run(() -> {
      // Handle translation normally
      double x = MathUtil.applyDeadband(xInput.getAsDouble(), DriveConstants.kJoystickDeadband);
      double y = MathUtil.applyDeadband(yInput.getAsDouble(), DriveConstants.kJoystickDeadband);
      x = xLimiter.calculate(Math.copySign(x * x, x));
      y = yLimiter.calculate(Math.copySign(y * y, y));

      if (DriverStation.getAlliance().orElse(Alliance.Blue) == Alliance.Red) {
        x = -x;
        y = -y;
      }

      // Calculate snapped angle
      double currentDeg = drive.getRotation2d().getDegrees();
      double snappedDeg = Math.round((currentDeg - offsetDegrees) / angleIncrementDegrees)
          * angleIncrementDegrees + offsetDegrees;

      double rotSpeed = turnPID.calculate(drive.getRotation2d().getRadians(), Math.toRadians(snappedDeg));

      drive.drive(
          x * DriveConstants.kMaxSpeedMetersPerSecond,
          y * DriveConstants.kMaxSpeedMetersPerSecond,
          rotSpeed,
          true
      );
    }, drive);
  }
}
