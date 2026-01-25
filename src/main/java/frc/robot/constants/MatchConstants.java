// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.constants;

/**
 * Constants for the 2026 REBUILT match structure and timing.
 *
 * <p><b>MATCH STRUCTURE:</b>
 * <pre>
 * AUTO (20s)      : Both hubs active
 * TRANSITION (10s): Both hubs active, FMS data arrives
 * SHIFT 1 (25s)   : One alliance hub inactive
 * SHIFT 2 (25s)   : Other alliance hub inactive
 * SHIFT 3 (25s)   : Same as Shift 1
 * SHIFT 4 (25s)   : Same as Shift 2
 * ENDGAME (30s)   : Both hubs active, focus on climbing
 * </pre>
 *
 * <p><b>FMS DATA:</b>
 * The game-specific message is 'R' or 'B', indicating which alliance's hub
 * is INACTIVE during Shifts 1 and 3. The opposite alliance is inactive in Shifts 2 and 4.
 */
public final class MatchConstants {

    private MatchConstants() {}

    // =========================================================================
    // MATCH PHASE DURATIONS
    // =========================================================================

    /** Duration of autonomous period in seconds. */
    public static final double kAutoDurationSeconds = 20.0;

    /** Duration of teleop period in seconds. */
    public static final double kTeleopDurationSeconds = 140.0;

    /** Duration of transition shift (both hubs active) at start of teleop. */
    public static final double kTransitionDurationSeconds = 10.0;

    /** Duration of each alliance shift. */
    public static final double kShiftDurationSeconds = 25.0;

    /** Duration of endgame period. */
    public static final double kEndgameDurationSeconds = 30.0;

    /** Number of alliance shifts in the match. */
    public static final int kNumAllianceShifts = 4;

    // =========================================================================
    // TELEOP TIME BOUNDARIES (countdown from 140.0)
    // =========================================================================

    /** Teleop time when transition ends and Shift 1 begins. */
    public static final double kTransitionEndTime = 130.0;

    /** Teleop time when Shift 1 ends and Shift 2 begins. */
    public static final double kShift1EndTime = 105.0;

    /** Teleop time when Shift 2 ends and Shift 3 begins. */
    public static final double kShift2EndTime = 80.0;

    /** Teleop time when Shift 3 ends and Shift 4 begins. */
    public static final double kShift3EndTime = 55.0;

    /** Teleop time when Shift 4 ends and Endgame begins. */
    public static final double kShift4EndTime = 30.0;

    // =========================================================================
    // WARNING THRESHOLDS (tunable)
    // =========================================================================

    /**
     * Seconds before shift change to warn driver.
     * Used for both "hub going inactive" and "hub going active" warnings.
     */
    public static final double kShiftWarningSeconds = 9.0;

    /** Seconds remaining when endgame "approaching" warning triggers. */
    public static final double kEndgameApproachingSeconds = 30.0;

    /** Seconds remaining when endgame "urgent climb" warning triggers. */
    public static final double kEndgameUrgentSeconds = 15.0;

    /** Seconds remaining for final climb warning. */
    public static final double kEndgameCriticalSeconds = 5.0;

    // =========================================================================
    // FMS DATA HANDLING
    // =========================================================================

    /** Expected delay (seconds) after auto before FMS data arrives. */
    public static final double kFmsDataDelaySeconds = 3.0;

    /** Character indicating Red alliance hub inactive in Shifts 1 & 3. */
    public static final char kRedInactiveCode = 'R';

    /** Character indicating Blue alliance hub inactive in Shifts 1 & 3. */
    public static final char kBlueInactiveCode = 'B';
}
