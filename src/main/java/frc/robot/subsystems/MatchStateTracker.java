// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
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
    // CONSTRUCTOR
    // =========================================================================

    public MatchStateTracker() {
        // Initialize dashboard defaults
        SmartDashboard.putString("Match/Phase", "Pre-Match");
        SmartDashboard.putString("Match/HubStatus", "UNKNOWN");
        SmartDashboard.putString("Match/NextStatus", "UNKNOWN");
        SmartDashboard.putNumber("Match/TimeInPhase", 0);
        SmartDashboard.putNumber("Match/TimeUntilShift", 0);
        SmartDashboard.putString("Match/Warning", "");
        SmartDashboard.putBoolean("Match/FmsDataReceived", false);
        SmartDashboard.putString("Match/FirstInactive", "WAITING...");
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

    // =========================================================================
    // PERIODIC UPDATE
    // =========================================================================

    @Override
    public void periodic() {
        // Update alliance
        ourAlliance = DriverStation.getAlliance().orElse(Alliance.Blue);

        // Poll for FMS data if not yet received
        if (!fmsDataReceived) {
            pollFmsData();
        }

        // Calculate current state
        updateMatchPhase();
        updateHubStatus();
        updateWarnings();

        // Update dashboard
        updateDashboard();

        // AdvantageKit logging
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

                // Log which alliance is inactive first
                String inactiveFirst = (code == MatchConstants.kRedInactiveCode) ? "RED" : "BLUE";
                System.out.println("FMS DATA RECEIVED: " + inactiveFirst + " hub inactive in Shifts 1 & 3");
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
     * Updates SmartDashboard with current match state.
     */
    private void updateDashboard() {
        SmartDashboard.putString("Match/Phase", currentPhase.getDisplayName());
        SmartDashboard.putString("Match/HubStatus", ourHubStatus.getDisplayName());
        SmartDashboard.putString("Match/NextStatus", nextHubStatus.getDisplayName());
        SmartDashboard.putNumber("Match/TimeInPhase", timeInPhase);
        SmartDashboard.putNumber("Match/TimeUntilShift", timeUntilNextShift);
        SmartDashboard.putString("Match/Warning", currentWarning.getMessage());
        SmartDashboard.putBoolean("Match/FmsDataReceived", fmsDataReceived);
        SmartDashboard.putBoolean("Match/HubActive", ourHubStatus == HubStatus.ACTIVE);
        SmartDashboard.putBoolean("Match/HasWarning", currentWarning != WarningLevel.NONE);

        // Show which alliance is inactive first (once known)
        if (fmsDataReceived) {
            String firstInactive = (fmsInactiveCode == MatchConstants.kRedInactiveCode) ? "RED" : "BLUE";
            SmartDashboard.putString("Match/FirstInactive", firstInactive);

            // Show if WE are inactive first (more useful for drivers)
            boolean weAreFirst = isOurAllianceInactiveInOddShifts();
            SmartDashboard.putString("Match/WeAreFirst",
                weAreFirst ? "WE'RE INACTIVE FIRST" : "THEY'RE INACTIVE FIRST");
        }

        // Countdown display for drivers (large numbers)
        SmartDashboard.putNumber("Match/ShiftCountdown", Math.ceil(timeUntilNextShift));
    }

    /**
     * Logs match state to AdvantageKit for replay analysis.
     */
    private void logToAdvantageKit() {
        Logger.recordOutput("Match/Phase", currentPhase.toString());
        Logger.recordOutput("Match/HubStatus", ourHubStatus.toString());
        Logger.recordOutput("Match/NextStatus", nextHubStatus.toString());
        Logger.recordOutput("Match/TimeInPhase", timeInPhase);
        Logger.recordOutput("Match/TimeUntilShift", timeUntilNextShift);
        Logger.recordOutput("Match/Warning", currentWarning.toString());
        Logger.recordOutput("Match/FmsDataReceived", fmsDataReceived);
        Logger.recordOutput("Match/HubActive", ourHubStatus == HubStatus.ACTIVE);
        Logger.recordOutput("Match/Alliance", ourAlliance.toString());
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
            System.out.println("TEST: FMS data manually set to '" + code + "'");
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
