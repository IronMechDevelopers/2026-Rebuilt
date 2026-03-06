// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.MatchConstants;

/**
 * Tracks match state and provides predictive information for the drive team.
 *
 * <p><b>PURPOSE:</b> Help the drive team live in the future, not react to the past.
 * This subsystem calculates upcoming hub status changes and provides warnings
 * so the team can position before shifts occur.
 *
 * <p><b>KEY OUTPUTS:</b>
 * <ul>
 *   <li>Current match phase (AUTO, TRANSITION, SHIFT 1-4, ENDGAME)</li>
 *   <li>Our hub status (ACTIVE / INACTIVE)</li>
 *   <li>Time until next shift</li>
 *   <li>Warnings (CLEAR_ZONE, GET_READY, CLIMB_NOW)</li>
 * </ul>
 *
 * <p><b>FMS DATA:</b>
 * The game-specific message ('R' or 'B') indicates which alliance's hub is
 * INACTIVE during Shifts 1 and 3. We poll until data arrives (~3s after auto).
 */
public class MatchStateTracker extends SubsystemBase {

    // =========================================================================
    // ENUMS
    // =========================================================================

    /** Current phase of the match. */
    public enum MatchPhase {
        PRE_MATCH("Pre-Match"),
        AUTO("Auto"),
        TRANSITION("Transition"),
        SHIFT_1("Shift 1"),
        SHIFT_2("Shift 2"),
        SHIFT_3("Shift 3"),
        SHIFT_4("Shift 4"),
        ENDGAME("Endgame"),
        POST_MATCH("Post-Match");

        private final String displayName;

        MatchPhase(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /** Status of our alliance's hub. */
    public enum HubStatus {
        ACTIVE("ACTIVE"),
        INACTIVE("INACTIVE"),
        UNKNOWN("UNKNOWN");

        private final String displayName;

        HubStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /** Warning level for drive team. */
    public enum WarningLevel {
        NONE(""),
        HUB_CLOSING_SOON("CLEAR ZONE!"),
        HUB_OPENING_SOON("GET READY!"),
        ENDGAME_APPROACHING("ENDGAME SOON"),
        ENDGAME_URGENT("CLIMB NOW!"),
        ENDGAME_CRITICAL("GO GO GO!");

        private final String message;

        WarningLevel(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    // =========================================================================
    // STATE
    // =========================================================================

    private Alliance ourAlliance = Alliance.Blue;
    private boolean fmsDataReceived = false;
    private char fmsInactiveCode = ' '; // 'R' or 'B'

    // Cached values (updated each periodic)
    private MatchPhase currentPhase = MatchPhase.PRE_MATCH;
    private HubStatus ourHubStatus = HubStatus.UNKNOWN;
    private HubStatus nextHubStatus = HubStatus.UNKNOWN;
    private double timeInPhase = 0;
    private double timeUntilNextShift = 0;
    private WarningLevel currentWarning = WarningLevel.NONE;

    // =========================================================================
    // PRACTICE MODE STATE
    // =========================================================================

    private boolean practiceMode = false;
    private double practiceStartTime = 0;
    private boolean practiceWeAreInactiveFirst = true; // Configurable for practice

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public MatchStateTracker() {
        // Practice mode controls (read/write for dashboard interaction)
        SmartDashboard.putBoolean("Practice/Enabled", false);
        SmartDashboard.putBoolean("Practice/WeAreInactiveFirst", true);
        SmartDashboard.putBoolean("Practice/Restart", false);
    }

    // =========================================================================
    // PUBLIC GETTERS
    // =========================================================================

    public MatchPhase getCurrentPhase() {
        return currentPhase;
    }

    public HubStatus getOurHubStatus() {
        return ourHubStatus;
    }

    public HubStatus getNextHubStatus() {
        return nextHubStatus;
    }

    public double getTimeInPhase() {
        return timeInPhase;
    }

    public double getTimeUntilNextShift() {
        return timeUntilNextShift;
    }

    public WarningLevel getCurrentWarning() {
        return currentWarning;
    }

    public boolean isFmsDataReceived() {
        return fmsDataReceived;
    }

    /** Returns true if our hub is currently active. */
    public boolean isOurHubActive() {
        return ourHubStatus == HubStatus.ACTIVE;
    }

    /** Returns true if we're in a warning state. */
    public boolean hasWarning() {
        return currentWarning != WarningLevel.NONE;
    }

    /**
     * Gets the full match shift pattern for timeline visualization.
     *
     * @return String array with 7 elements representing the match timeline:
     *         [AUTO, TRANSITION, SHIFT_1, SHIFT_2, SHIFT_3, SHIFT_4, ENDGAME]
     *         Each formatted as "SHIFT 1: ACTIVE (25s)" or "AUTO: ACTIVE (20s)"
     */
    public String[] getShiftPatternArray() {
        String[] pattern = new String[7];

        // Auto and Transition are always ACTIVE
        pattern[0] = "AUTO: ACTIVE (20s)";
        pattern[1] = "TRANS: ACTIVE (10s)";

        // If FMS data not received yet, show UNKNOWN
        if (!fmsDataReceived && !practiceMode) {
            pattern[2] = "SHIFT 1: UNKNOWN (25s)";
            pattern[3] = "SHIFT 2: UNKNOWN (25s)";
            pattern[4] = "SHIFT 3: UNKNOWN (25s)";
            pattern[5] = "SHIFT 4: UNKNOWN (25s)";
        } else {
            // Determine if we're inactive in odd shifts (1 & 3)
            boolean weAreInactiveInOddShifts = practiceMode ?
                practiceWeAreInactiveFirst : isOurAllianceInactiveInOddShifts();

            // Odd shifts (1 & 3)
            String oddStatus = weAreInactiveInOddShifts ? "INACTIVE" : "ACTIVE";
            String evenStatus = weAreInactiveInOddShifts ? "ACTIVE" : "INACTIVE";

            pattern[2] = "SHIFT 1: " + oddStatus + " (25s)";
            pattern[3] = "SHIFT 2: " + evenStatus + " (25s)";
            pattern[4] = "SHIFT 3: " + oddStatus + " (25s)";
            pattern[5] = "SHIFT 4: " + evenStatus + " (25s)";
        }

        // Endgame is always ACTIVE
        pattern[6] = "ENDGAME: ACTIVE (30s)";

        return pattern;
    }

    // =========================================================================
    // PERIODIC UPDATE
    // =========================================================================

    @Override
    public void periodic() {
        // Update alliance
        ourAlliance = DriverStation.getAlliance().orElse(Alliance.Blue);

        // Check practice mode controls from dashboard
        checkPracticeModeControls();

        if (practiceMode) {
            // Practice mode: use internal timer and simulate shifts
            updatePracticeMode();
        } else {
            // Normal match mode
            // Poll for FMS data if not yet received
            if (!fmsDataReceived) {
                pollFmsData();
            }

            // Calculate current state
            updateMatchPhase();
            updateHubStatus();
            updateWarnings();
        }

        // AdvantageKit logging (streams to NT via NT4Publisher)
        logToAdvantageKit();
    }

    // =========================================================================
    // FMS DATA POLLING
    // =========================================================================

    /**
     * Polls for the game-specific message from FMS.
     * Called each periodic until data is received.
     */
    private void pollFmsData() {
        String gameData = DriverStation.getGameSpecificMessage();

        if (gameData != null && !gameData.isEmpty()) {
            char code = gameData.charAt(0);
            if (code == MatchConstants.kRedInactiveCode || code == MatchConstants.kBlueInactiveCode) {
                fmsInactiveCode = code;
                fmsDataReceived = true;
            }
        }
    }

    // =========================================================================
    // PHASE CALCULATION
    // =========================================================================

    /**
     * Determines the current match phase based on match time.
     */
    private void updateMatchPhase() {
        double matchTime = DriverStation.getMatchTime();

        if (DriverStation.isDisabled()) {
            currentPhase = MatchPhase.PRE_MATCH;
            timeInPhase = 0;
            timeUntilNextShift = 0;
            return;
        }

        if (DriverStation.isAutonomous()) {
            currentPhase = MatchPhase.AUTO;
            timeInPhase = MatchConstants.kAutoDurationSeconds - matchTime;
            timeUntilNextShift = matchTime; // Time until teleop
            return;
        }

        // Teleop phases based on remaining time
        if (matchTime > MatchConstants.kTransitionEndTime) {
            // 140.0 - 130.0: Transition
            currentPhase = MatchPhase.TRANSITION;
            timeInPhase = MatchConstants.kTeleopDurationSeconds - matchTime;
            timeUntilNextShift = matchTime - MatchConstants.kTransitionEndTime;
        } else if (matchTime > MatchConstants.kShift1EndTime) {
            // 130.0 - 105.0: Shift 1
            currentPhase = MatchPhase.SHIFT_1;
            timeInPhase = MatchConstants.kTransitionEndTime - matchTime;
            timeUntilNextShift = matchTime - MatchConstants.kShift1EndTime;
        } else if (matchTime > MatchConstants.kShift2EndTime) {
            // 105.0 - 80.0: Shift 2
            currentPhase = MatchPhase.SHIFT_2;
            timeInPhase = MatchConstants.kShift1EndTime - matchTime;
            timeUntilNextShift = matchTime - MatchConstants.kShift2EndTime;
        } else if (matchTime > MatchConstants.kShift3EndTime) {
            // 80.0 - 55.0: Shift 3
            currentPhase = MatchPhase.SHIFT_3;
            timeInPhase = MatchConstants.kShift2EndTime - matchTime;
            timeUntilNextShift = matchTime - MatchConstants.kShift3EndTime;
        } else if (matchTime > MatchConstants.kShift4EndTime) {
            // 55.0 - 30.0: Shift 4
            currentPhase = MatchPhase.SHIFT_4;
            timeInPhase = MatchConstants.kShift3EndTime - matchTime;
            timeUntilNextShift = matchTime - MatchConstants.kShift4EndTime;
        } else if (matchTime > 0) {
            // 30.0 - 0.0: Endgame
            currentPhase = MatchPhase.ENDGAME;
            timeInPhase = MatchConstants.kShift4EndTime - matchTime;
            timeUntilNextShift = matchTime; // Time until match end
        } else {
            currentPhase = MatchPhase.POST_MATCH;
            timeInPhase = 0;
            timeUntilNextShift = 0;
        }
    }

    // =========================================================================
    // HUB STATUS CALCULATION
    // =========================================================================

    /**
     * Determines if our alliance's hub is active based on current phase and FMS data.
     */
    private void updateHubStatus() {
        // During these phases, both hubs are active
        if (currentPhase == MatchPhase.AUTO ||
            currentPhase == MatchPhase.TRANSITION ||
            currentPhase == MatchPhase.ENDGAME) {
            ourHubStatus = HubStatus.ACTIVE;
            nextHubStatus = calculateNextStatus();
            return;
        }

        // Pre/post match - unknown
        if (currentPhase == MatchPhase.PRE_MATCH || currentPhase == MatchPhase.POST_MATCH) {
            ourHubStatus = HubStatus.UNKNOWN;
            nextHubStatus = HubStatus.UNKNOWN;
            return;
        }

        // During alliance shifts, need FMS data
        if (!fmsDataReceived) {
            ourHubStatus = HubStatus.UNKNOWN;
            nextHubStatus = HubStatus.UNKNOWN;
            return;
        }

        // Determine if we're inactive in odd shifts (1 & 3) or even shifts (2 & 4)
        boolean weAreInactiveInOddShifts = isOurAllianceInactiveInOddShifts();

        // Odd shifts (1 & 3): FMS code alliance is inactive
        // Even shifts (2 & 4): Opposite alliance is inactive
        boolean isOddShift = (currentPhase == MatchPhase.SHIFT_1 || currentPhase == MatchPhase.SHIFT_3);

        if (isOddShift) {
            ourHubStatus = weAreInactiveInOddShifts ? HubStatus.INACTIVE : HubStatus.ACTIVE;
        } else {
            ourHubStatus = weAreInactiveInOddShifts ? HubStatus.ACTIVE : HubStatus.INACTIVE;
        }

        nextHubStatus = calculateNextStatus();
    }

    /**
     * Returns true if our alliance is inactive during Shifts 1 & 3.
     */
    private boolean isOurAllianceInactiveInOddShifts() {
        if (ourAlliance == Alliance.Red) {
            return fmsInactiveCode == MatchConstants.kRedInactiveCode;
        } else {
            return fmsInactiveCode == MatchConstants.kBlueInactiveCode;
        }
    }

    /**
     * Calculates what our hub status will be in the next phase.
     */
    private HubStatus calculateNextStatus() {
        if (!fmsDataReceived) {
            return HubStatus.UNKNOWN;
        }

        boolean weAreInactiveInOddShifts = isOurAllianceInactiveInOddShifts();

        switch (currentPhase) {
            case AUTO:
            case TRANSITION:
                // Next is Shift 1 (odd)
                return weAreInactiveInOddShifts ? HubStatus.INACTIVE : HubStatus.ACTIVE;
            case SHIFT_1:
                // Next is Shift 2 (even)
                return weAreInactiveInOddShifts ? HubStatus.ACTIVE : HubStatus.INACTIVE;
            case SHIFT_2:
                // Next is Shift 3 (odd)
                return weAreInactiveInOddShifts ? HubStatus.INACTIVE : HubStatus.ACTIVE;
            case SHIFT_3:
                // Next is Shift 4 (even)
                return weAreInactiveInOddShifts ? HubStatus.ACTIVE : HubStatus.INACTIVE;
            case SHIFT_4:
                // Next is Endgame (both active)
                return HubStatus.ACTIVE;
            case ENDGAME:
                // Match ending
                return HubStatus.UNKNOWN;
            default:
                return HubStatus.UNKNOWN;
        }
    }

    // =========================================================================
    // WARNING CALCULATION
    // =========================================================================

    /**
     * Determines what warning (if any) to display to the drive team.
     */
    private void updateWarnings() {
        currentWarning = WarningLevel.NONE;

        // Endgame warnings take priority
        if (currentPhase == MatchPhase.ENDGAME) {
            double matchTime = DriverStation.getMatchTime();
            if (matchTime <= MatchConstants.kEndgameCriticalSeconds) {
                currentWarning = WarningLevel.ENDGAME_CRITICAL;
            } else if (matchTime <= MatchConstants.kEndgameUrgentSeconds) {
                currentWarning = WarningLevel.ENDGAME_URGENT;
            }
            return;
        }

        // Endgame approaching warning (during Shift 4)
        if (currentPhase == MatchPhase.SHIFT_4 &&
            timeUntilNextShift <= MatchConstants.kShiftWarningSeconds) {
            currentWarning = WarningLevel.ENDGAME_APPROACHING;
            return;
        }

        // Hub status change warnings
        if (timeUntilNextShift <= MatchConstants.kShiftWarningSeconds && timeUntilNextShift > 0) {
            // Is our hub going from active to inactive?
            if (ourHubStatus == HubStatus.ACTIVE && nextHubStatus == HubStatus.INACTIVE) {
                currentWarning = WarningLevel.HUB_CLOSING_SOON;
            }
            // Is our hub going from inactive to active?
            else if (ourHubStatus == HubStatus.INACTIVE && nextHubStatus == HubStatus.ACTIVE) {
                currentWarning = WarningLevel.HUB_OPENING_SOON;
            }
        }
    }

    // =========================================================================
    // DASHBOARD OUTPUT
    // =========================================================================

    /**
     * Logs match state to AdvantageKit for replay analysis.
     * DashboardSetup reads data directly from subsystem methods.
     */
    private void logToAdvantageKit() {
        // Phase with practice indicator
        String phaseDisplay = practiceMode ? "[PRACTICE] " + currentPhase.getDisplayName() : currentPhase.getDisplayName();

        Logger.recordOutput("Match/Phase", phaseDisplay);
        Logger.recordOutput("Match/HubStatus", ourHubStatus.getDisplayName());
        Logger.recordOutput("Match/NextStatus", nextHubStatus.getDisplayName());
        Logger.recordOutput("Match/TimeInPhase", timeInPhase);
        Logger.recordOutput("Match/TimeUntilShift", timeUntilNextShift);
        Logger.recordOutput("Match/ShiftCountdown", Math.ceil(timeUntilNextShift));
        Logger.recordOutput("Match/Warning", currentWarning.getMessage());
        Logger.recordOutput("Match/HubActive", ourHubStatus == HubStatus.ACTIVE);
        Logger.recordOutput("Match/FmsDataReceived", fmsDataReceived || practiceMode);
        // Note: Match/Alliance is logged in RobotContainer to avoid duplication
        Logger.recordOutput("Match/PracticeMode", practiceMode);

        // Shift pattern array for timeline visualization
        Logger.recordOutput("Match/ShiftPattern", getShiftPatternArray());

        // Phase index for graphing (ordinal value)
        Logger.recordOutput("Match/PhaseIndex", currentPhase.ordinal());
    }

    // =========================================================================
    // PRACTICE MODE
    // =========================================================================

    /**
     * Checks dashboard controls for practice mode.
     */
    private void checkPracticeModeControls() {
        boolean enableRequested = SmartDashboard.getBoolean("Practice/Enabled", false);
        boolean restartRequested = SmartDashboard.getBoolean("Practice/Restart", false);
        practiceWeAreInactiveFirst = SmartDashboard.getBoolean("Practice/WeAreInactiveFirst", true);

        // Handle restart button
        if (restartRequested) {
            SmartDashboard.putBoolean("Practice/Restart", false);
            if (practiceMode) {
                practiceStartTime = Timer.getFPGATimestamp();
            }
        }

        // Handle enable/disable
        if (enableRequested && !practiceMode) {
            startPracticeMode();
        } else if (!enableRequested && practiceMode) {
            stopPracticeMode();
        }
    }

    /**
     * Starts practice mode - simulates teleop shift timing on a loop.
     */
    public void startPracticeMode() {
        practiceMode = true;
        practiceStartTime = Timer.getFPGATimestamp();
        SmartDashboard.putBoolean("Practice/Enabled", true);
    }

    /**
     * Stops practice mode and returns to normal operation.
     */
    public void stopPracticeMode() {
        practiceMode = false;
        SmartDashboard.putBoolean("Practice/Enabled", false);

        // Reset to pre-match state
        currentPhase = MatchPhase.PRE_MATCH;
        ourHubStatus = HubStatus.UNKNOWN;
        nextHubStatus = HubStatus.UNKNOWN;
    }

    /**
     * Returns true if practice mode is active.
     */
    public boolean isPracticeMode() {
        return practiceMode;
    }

    /**
     * Updates match state during practice mode using internal timer.
     * Cycles through: Shift 1 → Shift 2 → Shift 3 → Shift 4 → (repeat)
     */
    private void updatePracticeMode() {
        double elapsed = Timer.getFPGATimestamp() - practiceStartTime;

        // Total cycle time: 4 shifts × 25 seconds = 100 seconds
        double cycleTime = 4 * MatchConstants.kShiftDurationSeconds;
        double timeInCycle = elapsed % cycleTime;

        // Determine which shift we're in
        int shiftNumber = (int) (timeInCycle / MatchConstants.kShiftDurationSeconds) + 1;
        double timeIntoShift = timeInCycle % MatchConstants.kShiftDurationSeconds;
        double timeUntilShiftEnd = MatchConstants.kShiftDurationSeconds - timeIntoShift;

        // Set phase
        switch (shiftNumber) {
            case 1:
                currentPhase = MatchPhase.SHIFT_1;
                break;
            case 2:
                currentPhase = MatchPhase.SHIFT_2;
                break;
            case 3:
                currentPhase = MatchPhase.SHIFT_3;
                break;
            case 4:
            default:
                currentPhase = MatchPhase.SHIFT_4;
                break;
        }

        timeInPhase = timeIntoShift;
        timeUntilNextShift = timeUntilShiftEnd;

        // Calculate hub status based on practice settings
        // Odd shifts (1 & 3): We are inactive if practiceWeAreInactiveFirst is true
        // Even shifts (2 & 4): Opposite
        boolean isOddShift = (shiftNumber == 1 || shiftNumber == 3);

        if (isOddShift) {
            ourHubStatus = practiceWeAreInactiveFirst ? HubStatus.INACTIVE : HubStatus.ACTIVE;
        } else {
            ourHubStatus = practiceWeAreInactiveFirst ? HubStatus.ACTIVE : HubStatus.INACTIVE;
        }

        // Calculate next hub status
        boolean nextIsOddShift = (shiftNumber == 4 || shiftNumber == 2);
        if (nextIsOddShift) {
            nextHubStatus = practiceWeAreInactiveFirst ? HubStatus.INACTIVE : HubStatus.ACTIVE;
        } else {
            nextHubStatus = practiceWeAreInactiveFirst ? HubStatus.ACTIVE : HubStatus.INACTIVE;
        }

        // Calculate warnings (same logic as normal mode)
        updateWarnings();

        // Log practice cycle info to AdvantageKit
        Logger.recordOutput("Practice/CycleNumber", (int) (elapsed / cycleTime) + 1);
        Logger.recordOutput("Practice/TotalElapsed", elapsed);
    }

    // =========================================================================
    // TESTING / SIMULATION
    // =========================================================================

    /**
     * Manually sets FMS data for testing (use in simulation).
     * @param code 'R' for Red inactive first, 'B' for Blue inactive first
     */
    public void setFmsDataForTesting(char code) {
        if (code == MatchConstants.kRedInactiveCode || code == MatchConstants.kBlueInactiveCode) {
            fmsInactiveCode = code;
            fmsDataReceived = true;
        }
    }

    /**
     * Resets FMS data (for testing between matches).
     */
    public void resetFmsData() {
        fmsDataReceived = false;
        fmsInactiveCode = ' ';
    }
}
