// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.config;

import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.commands.PathPlannerAuto;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *                            AUTO SELECTOR
 * ═══════════════════════════════════════════════════════════════════════════
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

  // =========================================================================
  // CONSTRUCTOR
  // =========================================================================

  /**
   * Creates the auto selector.
   *
   * <p>IMPORTANT: Call registerNamedCommands() BEFORE creating this!
   */
  public AutoSelector() {
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

    // ═════════════════════════════════════════════════════════════════════
    // TEST AUTOS - Safe, simple paths for verifying PathPlanner works
    // ═════════════════════════════════════════════════════════════════════

    try {
      // Test Auto 1: Simple straight line (2 meters forward)
      // Purpose: Verify basic path following, encoders, and gyro
      chooser.addOption("Test: Drive Forward", new PathPlannerAuto("Test Drive Forward"));

      // Test Auto 2: L-shape path (forward 2m, turn 90 deg, forward 1m)
      // Purpose: Verify turning, rotation PID, and path transitions
      chooser.addOption("Test: L-Shape", new PathPlannerAuto("Test L-Shape"));

      System.out.println("Test autos loaded successfully");
    } catch (Exception e) {
      System.out.println("Test autos not found - create them in PathPlanner first!");
      System.out.println("   See AutoSelector.createAutoChooser() comments for instructions");
    }

    // ═════════════════════════════════════════════════════════════════════
    // GAME AUTOS - Add your competition autonomous routines here!
    // ═════════════════════════════════════════════════════════════════════

    // TODO: Create autonomous routines for current game (update each year)
    //
    // Examples:
    // chooser.addOption("3 Piece Auto", new PathPlannerAuto("3 Piece Auto"));
    // chooser.addOption("2 Piece Auto", new PathPlannerAuto("2 Piece Auto"));
    // chooser.addOption("Leave Only", new PathPlannerAuto("Leave Only"));

    return chooser;
  }

  // =========================================================================
  // NAMED COMMANDS (for PathPlanner)
  // =========================================================================

  /**
   * Registers named commands for use in PathPlanner autonomous routines.
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
    // ═════════════════════════════════════════════════════════════════════
    // DEBUG COMMANDS - Useful for testing
    // ═════════════════════════════════════════════════════════════════════

    NamedCommands.registerCommand(
      "Print Start",
      Commands.runOnce(() -> System.out.println("Auto started!"))
    );

    NamedCommands.registerCommand(
      "Print End",
      Commands.runOnce(() -> System.out.println("Auto complete!"))
    );

    // ═════════════════════════════════════════════════════════════════════
    // GAME-SPECIFIC NAMED COMMANDS - Add yours here!
    // ═════════════════════════════════════════════════════════════════════

    // TODO: Register game-specific commands for use in PathPlanner autonomous
    //
    // Examples (update for current game):
    // NamedCommands.registerCommand("Intake Game Piece", intakeSubsystem.runIntake());
    // NamedCommands.registerCommand("Score Game Piece", scorerSubsystem.score());
    // NamedCommands.registerCommand("Deploy Mechanism", deploySubsystem.deploy());
  }
}
