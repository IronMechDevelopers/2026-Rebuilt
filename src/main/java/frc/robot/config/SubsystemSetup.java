// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.config;

import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.VisionProvider;

/**
 * =====================================================================══════
 *                            SUBSYSTEM SETUP
 * =====================================================================══════
 *
 * This file creates all the robot's subsystems (except vision).
 *
 * WHAT IS A SUBSYSTEM?
 *   A subsystem is a part of the robot that does something:
 *   - DriveSubsystem: Makes the robot move
 *   - IntakeSubsystem: Picks up game pieces (add this each year!)
 *   - ShooterSubsystem: Shoots game pieces (add this each year!)
 *
 * HOW TO ADD A NEW SUBSYSTEM:
 *   1. Create your subsystem class in the subsystems/ folder
 *   2. Add a createXxxSubsystem() method here
 *   3. Call it from RobotContainer
 *
 * Related files:
 *   - subsystems/DriveSubsystem.java: The drive code
 *   - VisionSetup.java: Creates the vision provider used by drive
 */
public class SubsystemSetup {

  // =========================================================================
  // DRIVE SUBSYSTEM
  // =========================================================================

  /**
   * Creates the drive subsystem.
   *
   * <p>The drive subsystem controls the swerve drivetrain and uses vision
   * to help track the robot's position on the field.
   *
   * @param visionProvider The vision system (from VisionSetup)
   * @return A new DriveSubsystem
   */
  public static DriveSubsystem createDriveSubsystem(VisionProvider visionProvider) {
    return new DriveSubsystem(visionProvider);
  }

  // =========================================================================
  // GAME-SPECIFIC SUBSYSTEMS - ADD YOURS HERE!
  // =========================================================================

  // TODO: Add subsystems for current game (update each year)
  //
  // Example - Intake subsystem:
  // public static IntakeSubsystem createIntakeSubsystem() {
  //   return new IntakeSubsystem();
  // }
  //
  // Example - Shooter subsystem:
  // public static ShooterSubsystem createShooterSubsystem() {
  //   return new ShooterSubsystem();
  // }
  //
  // Example - Climber subsystem:
  // public static ClimberSubsystem createClimberSubsystem() {
  //   return new ClimberSubsystem();
  // }
}
