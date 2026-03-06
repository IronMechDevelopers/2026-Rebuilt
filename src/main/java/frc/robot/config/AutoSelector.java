// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.config;

import java.util.Set;

import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.commands.PathPlannerAuto;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.CANFuelSubsystem;

/**
 * =====================================================================══════
 *                            AUTO SELECTOR
 * =====================================================================══════
 *
 * This file sets up which autonomous routines you can choose from.
 *
 * WHAT IS AUTONOMOUS?
 *   The first 15 seconds of a match where the robot drives itself!
 *   You pick which "auto" to run before the match starts.
 *
 * HOW TO ADD A NEW AUTO:
 *   1. Create the path in PathPlanner (the app)
 *   2. Add it to createAutoChooser() below
 *   3. It will show up on the dashboard!
 *
 * WHAT TO UPDATE EACH YEAR:
 *   - Add game-specific autos in createAutoChooser()
 *   - Add game-specific named commands in registerNamedCommands()
 *
 * Related files:
 *   - PathPlanner paths are in deploy/pathplanner/
 */
public class AutoSelector {

  /** The chooser that appears on the dashboard */
  private final SendableChooser<Command> autoChooser;

  /** Drive subsystem for simple autos */
  private final DriveSubsystem drive;

  /** Fuel subsystem for simple autos */
  private final CANFuelSubsystem fuel;

  // =========================================================================
  // CONSTRUCTOR
  // =========================================================================

  /**
   * Creates the auto selector.
   *
   * <p>IMPORTANT: Call registerNamedCommands() BEFORE creating this!
   *
   * @param drive Drive subsystem for command-based autos
   * @param fuel Fuel subsystem for command-based autos
   */
  public AutoSelector(DriveSubsystem drive, CANFuelSubsystem fuel) {
    this.drive = drive;
    this.fuel = fuel;
    this.autoChooser = createAutoChooser();
  }

  // =========================================================================
  // GET THE CHOOSER (for dashboard)
  // =========================================================================

  /**
   * Gets the auto chooser to display on Shuffleboard.
   *
   * @return The SendableChooser for autonomous modes
   */
  public SendableChooser<Command> getChooser() {
    return autoChooser;
  }

  // =========================================================================
  // GET SELECTED AUTO (called when auto starts)
  // =========================================================================

  /**
   * Gets the currently selected autonomous command.
   *
   * <p>Called by Robot.java when autonomous period starts.
   *
   * @return The selected auto command, or null if "None" selected
   */
  public Command getSelectedAuto() {
    return autoChooser.getSelected();
  }

  // =========================================================================
  // CREATE THE AUTO CHOOSER
  // =========================================================================

  /**
   * Creates the autonomous selector dropdown.
   *
   * <p><b>UPDATE EACH YEAR:</b> Add PathPlanner autos created for current game.
   *
   * <p><b>TEST AUTOS INCLUDED:</b>
   * <ul>
   *   <li><b>Test: Drive Forward:</b> Simple 2m straight line (verify basic movement)
   *   <li><b>Test: L-Shape:</b> Drive 2m, turn 90 deg, drive 1m (verify turning)
   * </ul>
   *
   * <p><b>How to create these paths in PathPlanner:</b>
   * <ol>
   *   <li>Install PathPlanner: https://pathplanner.dev/home.html
   *   <li>Open PathPlanner, select current season field layout
   *   <li>Create "Test Drive Forward": Start (1, 4), End (3, 4), straight line
   *   <li>Create "Test L-Shape": Start (1, 4), waypoint (3, 4), End (3, 6), 90 deg turn
   *   <li>Set constraints: Max Vel 2.0 m/s, Max Accel 1.5 m/s sq (SAFE for testing!)
   *   <li>Save to deploy/pathplanner/autos/ folder
   * </ol>
   */
  private SendableChooser<Command> createAutoChooser() {
    SendableChooser<Command> chooser = new SendableChooser<>();
    chooser.setDefaultOption("None", null);

    // =====================================================================
    // TEST AUTOS - PathPlanner test paths (disabled until paths are created)
    // =====================================================================

    // TODO: Uncomment these once you've created the paths in PathPlanner
    // try {
    //   // Test Auto 1: Simple straight line (2 meters forward)
    //   // Purpose: Verify basic path following, encoders, and gyro
    //   chooser.addOption("Test: Drive Forward", new PathPlannerAuto("Test Drive Forward"));
    //
    //   // Test Auto 2: L-shape path (forward 2m, turn 90 deg, forward 1m)
    //   // Purpose: Verify turning, rotation PID, and path transitions
    //   chooser.addOption("Test: L-Shape", new PathPlannerAuto("Test L-Shape"));
    //
    //   System.out.println("Test autos loaded successfully");
    // } catch (Exception e) {
    //   System.out.println("Test autos not found - create them in PathPlanner first!");
    //   System.out.println("   See AutoSelector.createAutoChooser() comments for instructions");
    // }

    // =====================================================================
    // GAME AUTOS - Command-based autonomous routines
    // =====================================================================

    // Simple Auto 1: Drive back 2 feet and shoot for 10 seconds
    // Purpose: Score preloaded game piece and back away from starting zone
    chooser.addOption("Drive Back and Shoot",
      Commands.sequence(
        // Drive backwards 2 feet using odometry to track distance
        // Easy to adjust on competition day: just change the feet/inches numbers
        createDriveDistanceCommand(-0.5, 0, 2, 0), // backwards at 0.5 m/s for 2 feet 0 inches
        // Run shooter for 10 seconds
        fuel.launchCommand().withTimeout(10.0),
        // Stop shooter
        Commands.runOnce(() -> fuel.stop(), fuel)
      )
    );

    // Simple Auto 2: Shoot for 10 seconds then drive backwards 3 feet
    // Purpose: Score preloaded game piece, then leave starting zone
    chooser.addOption("Shoot then Drive Back",
      Commands.sequence(
        // Run shooter for 10 seconds
        fuel.launchCommand().withTimeout(10.0),
        // Stop shooter
        Commands.runOnce(() -> fuel.stop(), fuel),
        // Drive backwards 3 feet
        createDriveDistanceCommand(-0.5, 0, 3, 0) // backwards at 0.5 m/s for 3 feet
      )
    );

    // Simple Auto 3: Do nothing
    // Purpose: For testing, or when you just want to stay put
    chooser.addOption("Do Nothing", Commands.none());

    // TODO: Create additional autonomous routines for current game (update each year)
    //
    // OPTION 1 - Command-based (like the autos above):
    //   Use createDriveDistanceCommand() and Commands.sequence() for simple routines
    //
    // OPTION 2 - PathPlanner (for complex paths with curves/obstacles):
    //   Create paths in PathPlanner app, then add:
    //   chooser.addOption("3 Piece Auto", new PathPlannerAuto("3 Piece Auto"));
    //   chooser.addOption("2 Piece Auto", new PathPlannerAuto("2 Piece Auto"));

    return chooser;
  }

  // =========================================================================
  // HELPER METHODS FOR SIMPLE AUTOS
  // =========================================================================

  /**
   * Creates a command that drives a specific distance using odometry.
   *
   * <p><b>COMPETITION DAY FRIENDLY:</b> Uses feet + inches for easy adjustments.
   * No need to convert to decimals or meters in high-pressure situations!
   *
   * <p><b>Example usage:</b>
   * <pre>
   * // Drive forward 3 feet
   * createDriveDistanceCommand(0.5, 0, 3, 0)
   *
   * // Drive backward 2 feet 6 inches
   * createDriveDistanceCommand(-0.5, 0, 2, 6)
   *
   * // Drive backward 1 foot 3 inches
   * createDriveDistanceCommand(-0.5, 0, 1, 3)
   *
   * // Strafe right 2 feet
   * createDriveDistanceCommand(0, 0.5, 2, 0)
   * </pre>
   *
   * @param xSpeed Forward speed in m/s (negative = backwards)
   * @param ySpeed Strafe speed in m/s (negative = left, positive = right)
   * @param feet Distance in feet
   * @param inches Additional distance in inches (added to feet)
   * @return Command that drives the specified distance
   */
  private Command createDriveDistanceCommand(double xSpeed, double ySpeed, int feet, int inches) {
    // Convert feet + inches to meters once
    double distanceMeters = Units.feetToMeters(feet) + Units.inchesToMeters(inches);

    return Commands.defer(() -> {
      // Capture the starting pose once when command initializes
      Pose2d startPose = drive.getCurrentPose();

      return Commands.runEnd(
        // Execute: Drive at specified speed
        () -> drive.drive(xSpeed, ySpeed, 0, false),
        // End: Stop driving
        () -> drive.drive(0, 0, 0, false),
        drive
      ).until(() -> {
        // Check if we've traveled the target distance
        double distanceTraveled = drive.getCurrentPose().getTranslation()
            .getDistance(startPose.getTranslation());
        return distanceTraveled >= distanceMeters;
      });
    }, Set.of(drive)).withTimeout(5.0); // Safety timeout: 5 seconds max
  }

  // =========================================================================
  // NAMED COMMANDS (for PathPlanner)
  // =========================================================================

  /**
   * Registers named commands for use in PathPlanner autonomous routines.
   *
   * <p><b>NOTE:</b> These are only used when you create PathPlanner paths.
   * The simple command-based autos above don't need named commands.
   *
   * <p><b>What are named commands?</b> Custom actions that can be inserted into
   * autonomous paths (e.g., run intake, shoot, deploy mechanism).
   *
   * <p><b>How to use:</b>
   * <ol>
   *   <li>Register command here with a name
   *   <li>In PathPlanner GUI, add "Named Command" marker to path
   *   <li>Select registered command name
   *   <li>Command runs when robot reaches that point in path
   * </ol>
   *
   * <p><b>CALL THIS BEFORE creating AutoSelector!</b>
   *
   * <p><b>UPDATE EACH YEAR:</b> Add game-specific commands here!
   */
  public static void registerNamedCommands() {
    // =====================================================================
    // DEBUG COMMANDS - Useful for testing PathPlanner paths
    // =====================================================================

    NamedCommands.registerCommand(
      "Print Start",
      Commands.runOnce(() -> System.out.println("Auto started!"))
    );

    NamedCommands.registerCommand(
      "Print End",
      Commands.runOnce(() -> System.out.println("Auto complete!"))
    );

    // =====================================================================
    // GAME-SPECIFIC NAMED COMMANDS - Add yours here when using PathPlanner!
    // =====================================================================

    // TODO: Register game-specific commands for use in PathPlanner autonomous
    //
    // Examples (update for current game):
    // NamedCommands.registerCommand("Intake Game Piece", intakeSubsystem.runIntake());
    // NamedCommands.registerCommand("Score Game Piece", scorerSubsystem.score());
    // NamedCommands.registerCommand("Deploy Mechanism", deploySubsystem.deploy());
  }
}
