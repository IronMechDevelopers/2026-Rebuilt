// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import java.util.function.DoubleSupplier;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.path.PathConstraints;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.constants.DriveConstants;
import frc.robot.constants.FieldConstants;
import frc.robot.subsystems.DriveSubsystem;

/**
 * =====================================================================══════
 *                        VISION DRIVE COMMANDS
 * =====================================================================══════
 *
 * Commands that use vision or auto-aim to control the robot.
 *
 * <p><b>AVAILABLE COMMANDS:</b>
 * <ul>
 *   <li>{@link #aimAtTarget} - Rotate in place to face any target</li>
 *   <li>{@link #aimAtHub} - Rotate in place to face the alliance hub</li>
 *   <li>{@link #driveWhileAiming} - Drive with joystick while auto-aiming at a target</li>
 *   <li>{@link #driveWhileAimingAtHub} - Drive with joystick while auto-aiming at hub</li>
 *   <li>{@link #driveToPose} - Pathfind to a specific location</li>
 * </ul>
 *
 * <p><b>HOW AUTO-AIM WORKS:</b>
 * <ol>
 *   <li>Calculate angle from robot to target</li>
 *   <li>Use a PID controller to rotate toward that angle</li>
 *   <li>Keep updating as the robot moves</li>
 * </ol>
 *
 * <p><b>NOTE FOR DEVELOPERS:</b>
 * Use {@link DriveCommands} as the main entry point for all drive commands.
 * This file contains the implementations, but DriveCommands provides a
 * cleaner, unified API.
 */
public class VisionDriveCommands {

  private VisionDriveCommands() {}

  // =========================================================================
  // PATHFINDING
  // =========================================================================

  /**
   * Uses PathPlanner to pathfind to a specific pose on the field.
   *
   * @param drive The drive subsystem
   * @param targetPose Where to go (x, y, rotation)
   * @return Command that drives to the pose
   */
  public static Command driveToPose(DriveSubsystem drive, Pose2d targetPose) {
    PathConstraints constraints = new PathConstraints(
        DriveConstants.kPathfindMaxVelMetersPerSecond,
        DriveConstants.kPathfindMaxAccelMetersPerSecondSquared,
        DriveConstants.kPathfindMaxAngularVelRadiansPerSecond,
        DriveConstants.kPathfindMaxAngularAccelRadiansPerSecondSquared);

    return AutoBuilder.pathfindToPose(targetPose, constraints, DriveConstants.kPathfindGoalEndVelocity);
  }

  // =========================================================================
  // AIMING (rotation only)
  // =========================================================================

  /**
   * Rotates the robot in place to face a target.
   *
   * @param drive The drive subsystem
   * @param target The target to aim at
   * @return Command that aims at the target
   */
  public static Command aimAtTarget(DriveSubsystem drive, Pose2d target) {
    PIDController rotController = new PIDController(
        DriveConstants.kAutoAimP,
        DriveConstants.kAutoAimI,
        DriveConstants.kAutoAimD);
    rotController.enableContinuousInput(-Math.PI, Math.PI);
    rotController.setTolerance(Math.toRadians(DriveConstants.kAutoAimToleranceDegrees));

    return Commands.run(() -> {
      Pose2d currentPose = drive.getCurrentPose();

      double angleToTarget = Math.atan2(
          target.getY() - currentPose.getY(),
          target.getX() - currentPose.getX()
      );

      double rotSpeed = rotController.calculate(currentPose.getRotation().getRadians(), angleToTarget);
      drive.drive(0, 0, rotSpeed, true);
    }, drive)
    .until(rotController::atSetpoint);
  }

  // =========================================================================
  // HUB AIMING (2026 REBUILT game-specific)
  // =========================================================================

  /**
   * Aims at the alliance hub center.
   *
   * <p>Automatically selects the correct hub based on alliance color.
   *
   * @param drive The drive subsystem
   * @return Command that rotates to face the hub
   */
  public static Command aimAtHub(DriveSubsystem drive) {
    return aimAtTarget(drive, FieldConstants.getAllianceHubPose());
  }

  /**
   * Drives with joystick while automatically aiming at the hub.
   *
   * <p>Driver controls translation, robot automatically rotates to face hub.
   *
   * @param drive The drive subsystem
   * @param xInput Forward/backward joystick (-1 to 1)
   * @param yInput Left/right joystick (-1 to 1)
   * @return Command for hub-locked driving
   */
  public static Command driveWhileAimingAtHub(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput) {

    PIDController rotController = new PIDController(
        DriveConstants.kAutoAimP,
        DriveConstants.kAutoAimI,
        DriveConstants.kAutoAimD);
    rotController.enableContinuousInput(-Math.PI, Math.PI);

    SlewRateLimiter xLimiter = new SlewRateLimiter(DriveConstants.kTranslationSlewRate);
    SlewRateLimiter yLimiter = new SlewRateLimiter(DriveConstants.kTranslationSlewRate);

    return Commands.run(() -> {
      double x = MathUtil.applyDeadband(xInput.getAsDouble(), DriveConstants.kJoystickDeadband);
      double y = MathUtil.applyDeadband(yInput.getAsDouble(), DriveConstants.kJoystickDeadband);
      x = xLimiter.calculate(Math.copySign(x * x, x));
      y = yLimiter.calculate(Math.copySign(y * y, y));

      if (DriverStation.getAlliance().orElse(Alliance.Blue) == Alliance.Red) {
        x = -x;
        y = -y;
      }

      Pose2d currentPose = drive.getCurrentPose();
      var hubCenter = FieldConstants.getAllianceHubCenter();

      double angleToHub = Math.atan2(
          hubCenter.getY() - currentPose.getY(),
          hubCenter.getX() - currentPose.getX());

      double rotSpeed = rotController.calculate(
          currentPose.getRotation().getRadians(), angleToHub);

      drive.drive(
          x * DriveConstants.kMaxSpeedMetersPerSecond,
          y * DriveConstants.kMaxSpeedMetersPerSecond,
          rotSpeed,
          true);
    }, drive);
  }

  // =========================================================================
  // DRIVE WHILE AIMING (translation + auto-aim)
  // =========================================================================

  /**
   * Drives with joystick translation, but automatically rotates to aim at a target.
   *
   * @param drive The drive subsystem
   * @param xInput Forward/backward joystick input
   * @param yInput Left/right joystick input
   * @param target The target to aim at
   * @param aimOffsetDegrees Which side of robot to aim (0=front, 180=back)
   * @return Command that drives while aiming
   */
  public static Command driveWhileAiming(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput,
      Pose2d target,
      double aimOffsetDegrees) {

    PIDController rotController = new PIDController(
        DriveConstants.kHeadingLockP,
        DriveConstants.kHeadingLockI,
        DriveConstants.kHeadingLockD);
    rotController.enableContinuousInput(-Math.PI, Math.PI);

    SlewRateLimiter xLimiter = new SlewRateLimiter(DriveConstants.kTranslationSlewRate);
    SlewRateLimiter yLimiter = new SlewRateLimiter(DriveConstants.kTranslationSlewRate);

    return Commands.run(() -> {
      double x = MathUtil.applyDeadband(xInput.getAsDouble(), DriveConstants.kJoystickDeadband);
      double y = MathUtil.applyDeadband(yInput.getAsDouble(), DriveConstants.kJoystickDeadband);
      x = xLimiter.calculate(Math.copySign(x * x, x));
      y = yLimiter.calculate(Math.copySign(y * y, y));

      var alliance = DriverStation.getAlliance();
      if (alliance.isPresent() && alliance.get() == Alliance.Red) {
        x = -x;
        y = -y;
      }

      double multiplier = drive.getSpeedMultiplier();

      Pose2d currentPose = drive.getCurrentPose();
      double angleToTarget = Math.atan2(
          target.getY() - currentPose.getY(),
          target.getX() - currentPose.getX()
      );
      double targetAngle = angleToTarget - Math.toRadians(aimOffsetDegrees);
      double rotSpeed = rotController.calculate(currentPose.getRotation().getRadians(), targetAngle);

      drive.drive(
          x * DriveConstants.kMaxSpeedMetersPerSecond * multiplier,
          y * DriveConstants.kMaxSpeedMetersPerSecond * multiplier,
          rotSpeed,
          true
      );
    }, drive);
  }
}
