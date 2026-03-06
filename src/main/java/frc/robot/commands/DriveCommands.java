// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import java.util.function.DoubleSupplier;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.DriveSubsystem;

/**
 * =====================================================================══════
 *                           DRIVE COMMANDS
 * =====================================================================══════
 *
 * THE MAIN ENTRY POINT FOR ALL DRIVE COMMANDS.
 *
 * Use this file for all drive commands - it provides a clean, unified API.
 *
 * <h2>COMMAND CATEGORIES:</h2>
 * <ul>
 *   <li><b>Basic Driving:</b> joystickDrive, setSpeed, toggleFieldRelative</li>
 *   <li><b>Aiming:</b> aimAtTarget, aimAtHub, driveWhileAiming, driveWhileAimingAtHub</li>
 *   <li><b>Pathfinding:</b> driveToPose</li>
 *   <li><b>Utilities:</b> xStance, zeroHeading, forceVisionReset, etc.</li>
 * </ul>
 *
 * <h2>EXAMPLES:</h2>
 * <pre>
 * // Rotate to face a target
 * button.onTrue(DriveCommands.aimAtTarget(drive, targetPose));
 *
 * // Rotate to face the alliance hub
 * button.onTrue(DriveCommands.aimAtHub(drive));
 *
 * // Drive while auto-aiming at hub
 * drive.setDefaultCommand(DriveCommands.driveWhileAimingAtHub(drive, xSupplier, ySupplier));
 * </pre>
 */
public class DriveCommands {

  private DriveCommands() {}

  // =========================================================================
  // BASIC DRIVING (see BasicDriveCommands.java for details)
  // =========================================================================

  /** Standard teleop driving with joysticks. */
  public static Command joystickDrive(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput,
      DoubleSupplier rotInput) {
    return BasicDriveCommands.joystickDrive(drive, xInput, yInput, rotInput);
  }

  /** Sets drive speed multiplier (0.0 to 1.0). */
  public static Command setSpeed(DriveSubsystem drive, double multiplier) {
    return BasicDriveCommands.setSpeed(drive, multiplier);
  }

  /** Toggles between full and half speed. */
  public static Command toggleSpeed(DriveSubsystem drive) {
    return BasicDriveCommands.toggleSpeed(drive);
  }

  /** Toggles field-relative driving on/off. */
  public static Command toggleFieldRelative(DriveSubsystem drive) {
    return BasicDriveCommands.toggleFieldRelative(drive);
  }

  // =========================================================================
  // AIMING - Rotate to face targets
  // =========================================================================

  /** Rotates in place to face any target. */
  public static Command aimAtTarget(DriveSubsystem drive, Pose2d target) {
    return VisionDriveCommands.aimAtTarget(drive, target);
  }

  /** Rotates in place to face the alliance hub. */
  public static Command aimAtHub(DriveSubsystem drive) {
    return VisionDriveCommands.aimAtHub(drive);
  }

  /** Drives with joystick while auto-aiming at any target. */
  public static Command driveWhileAiming(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput,
      Pose2d target,
      double aimOffsetDegrees) {
    return VisionDriveCommands.driveWhileAiming(drive, xInput, yInput, target, aimOffsetDegrees);
  }

  /** Drives with joystick while auto-aiming at the hub. */
  public static Command driveWhileAimingAtHub(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput) {
    return VisionDriveCommands.driveWhileAimingAtHub(drive, xInput, yInput);
  }

  // =========================================================================
  // PATHFINDING - Navigate to t
  // =========================================================================

  /** Pathfinds to a specific pose using PathPlanner. */
  public static Command driveToPose(DriveSubsystem drive, Pose2d targetPose) {
    return VisionDriveCommands.driveToPose(drive, targetPose);
  }

  // =========================================================================
  // UTILITIES (see UtilityCommands.java for details)
  // =========================================================================

  /** Sets wheels to X formation to resist pushing. */
  public static Command xStance(DriveSubsystem drive) {
    return UtilityCommands.xStance(drive);
  }

  /** Sets all wheels to point straight ahead. */
  public static Command setStraightAhead(DriveSubsystem drive) {
    return UtilityCommands.setStraightAhead(drive);
  }

  /** Resets the gyro heading to zero. */
  public static Command zeroHeading(DriveSubsystem drive) {
    return UtilityCommands.zeroHeading(drive);
  }

  /** Forces position to match vision. */
  public static Command forceVisionReset(DriveSubsystem drive) {
    return UtilityCommands.forceVisionReset(drive);
  }

  /** Sets motors to coast mode. */
  public static Command setCoastMode(DriveSubsystem drive) {
    return UtilityCommands.setCoastMode(drive);
  }

  /** Sets motors to brake mode. */
  public static Command setBrakeMode(DriveSubsystem drive) {
    return UtilityCommands.setBrakeMode(drive);
  }

  /** Snaps rotation to 0/90/180/270 degrees. */
  public static Command snapToClosestCardinal(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput) {
    return UtilityCommands.snapToClosestCardinal(drive, xInput, yInput);
  }

  /** Snaps rotation to 45/135/225/315 degrees. */
  public static Command snapToDiamond(
      DriveSubsystem drive,
      DoubleSupplier xInput,
      DoubleSupplier yInput) {
    return UtilityCommands.snapToDiamond(drive, xInput, yInput);
  }

  public static Command moveOneWheel(DriveSubsystem drive)
  {
    return Commands.startEnd(() -> drive.moveOneWheel(), () -> drive.setStraightAhead(), drive);
  }
}
