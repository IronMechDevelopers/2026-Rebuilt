// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.led;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.subsystems.MatchStateTracker;
import frc.robot.subsystems.MatchStateTracker.HubStatus;
import frc.robot.subsystems.MatchStateTracker.MatchPhase;

/**
 * Manages LED state based on match phase and allows temporary interrupts.
 *
 * <p><b>DEFAULT BEHAVIOR (from match state):</b>
 * <table>
 *   <tr><th>Phase</th><th>LED Pattern</th></tr>
 *   <tr><td>Pre-Match</td><td>Chasing alliance color (or Blue/Gold if unknown)</td></tr>
 *   <tr><td>Auto</td><td>Solid alliance color</td></tr>
 *   <tr><td>Transition</td><td>Purple (unknown) → Blink color of first active alliance</td></tr>
 *   <tr><td>Active Shift</td><td>Solid → Blink alliance → Blink white (final 3s)</td></tr>
 *   <tr><td>Inactive Shift</td><td>Solid yellow → Blink yellow → Blink white (final 3s)</td></tr>
 *   <tr><td>Endgame (35s)</td><td>Blink purple</td></tr>
 *   <tr><td>Endgame (16s)</td><td>Blink white</td></tr>
 *   <tr><td>Post-Match</td><td>Rainbow/school colors</td></tr>
 * </table>
 *
 * <p><b>INTERRUPT SYSTEM:</b>
 * Temporary states (like "shooter ready") can override the default for a short duration,
 * then automatically return to the match-based state.
 *
 * <pre>
 * // Example: Flash green for 0.5s when shooter is ready
 * ledSubsystem.setInterrupt(LEDState.SHOOTER_READY, 0.5);
 * </pre>
 */
public class LEDSubsystem extends SubsystemBase {

    // =========================================================================
    // TIMING CONSTANTS
    // =========================================================================

    /** Seconds before shift end to show mid-shift warning (blink). */
    private static final double MID_SHIFT_WARNING_TIME = 9.0;

    /** Seconds before shift end to show final warning (blink white). */
    private static final double FINAL_SHIFT_WARNING_TIME = 3.0;

    /** Seconds remaining when endgame purple blink starts. */
    private static final double ENDGAME_PURPLE_TIME = 35.0;

    /** Seconds remaining when endgame white blink starts. */
    private static final double ENDGAME_WHITE_TIME = 16.0;

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final LEDController controller;
    private final MatchStateTracker matchStateTracker;

    // =========================================================================
    // STATE
    // =========================================================================

    private LEDState currentState = LEDState.OFF;
    private LEDState interruptState = null;
    private double interruptEndTime = 0;
    private Alliance lastAlliance = null;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Creates the LED subsystem.
     *
     * @param controller        The hardware controller (REV Blink, CANdle, etc.)
     * @param matchStateTracker The match state tracker for phase info
     */
    public LEDSubsystem(LEDController controller, MatchStateTracker matchStateTracker) {
        this.controller = controller;
        this.matchStateTracker = matchStateTracker;

        // Set initial state
        controller.setBrightness(1.0);
        controller.setState(LEDState.DISABLED);

        System.out.println("LEDSubsystem initialized with: " + controller.getDescription());
    }

    // =========================================================================
    // INTERRUPT SYSTEM
    // =========================================================================

    /**
     * Sets a temporary interrupt state that overrides the default.
     *
     * <p>After the duration expires, LEDs return to match-based state.
     *
     * @param state    The interrupt state to display
     * @param duration How long to display (seconds)
     */
    public void setInterrupt(LEDState state, double duration) {
        interruptState = state;
        interruptEndTime = Timer.getFPGATimestamp() + duration;
        controller.setState(state);
        System.out.println("LED Interrupt: " + state.getDescription() + " for " + duration + "s");
    }

    /**
     * Clears any active interrupt immediately.
     */
    public void clearInterrupt() {
        interruptState = null;
        interruptEndTime = 0;
    }

    /**
     * Returns true if an interrupt is currently active.
     */
    public boolean hasActiveInterrupt() {
        return interruptState != null && Timer.getFPGATimestamp() < interruptEndTime;
    }

    // =========================================================================
    // CONVENIENCE INTERRUPT METHODS
    // =========================================================================

    /** Flashes green to indicate shooter is ready. */
    public void flashShooterReady() {
        setInterrupt(LEDState.SHOOTER_READY, 0.5);
    }

    /** Flashes to indicate game piece acquired. */
    public void flashGamePieceAcquired() {
        setInterrupt(LEDState.GAME_PIECE_ACQUIRED, 0.75);
    }

    /** Flashes error pattern. */
    public void flashError() {
        setInterrupt(LEDState.ERROR, 1.0);
    }

    // =========================================================================
    // PERIODIC
    // =========================================================================

    @Override
    public void periodic() {
        // Check if interrupt has expired
        if (interruptState != null && Timer.getFPGATimestamp() >= interruptEndTime) {
            interruptState = null;
        }

        // Determine what state to display
        LEDState targetState;
        if (interruptState != null) {
            targetState = interruptState;
        } else {
            targetState = calculateMatchState();
        }

        // Only update if state changed
        if (!targetState.equals(currentState)) {
            currentState = targetState;
            controller.setState(currentState);
        }

        // Update animations
        controller.update();

        // Dashboard and logging
        updateDashboard();
    }

    // =========================================================================
    // MATCH STATE CALCULATION
    // =========================================================================

    /**
     * Calculates the LED state based on current match phase.
     */
    private LEDState calculateMatchState() {
        Alliance alliance = DriverStation.getAlliance().orElse(null);
        MatchPhase phase = matchStateTracker.getCurrentPhase();
        double timeUntilShift = matchStateTracker.getTimeUntilNextShift();
        double matchTime = DriverStation.getMatchTime();

        // Track alliance changes
        if (alliance != lastAlliance) {
            lastAlliance = alliance;
        }

        // Disabled/Pre-match
        if (DriverStation.isDisabled()) {
            if (alliance == null) {
                return LEDState.PRE_MATCH_UNKNOWN;
            } else if (alliance == Alliance.Red) {
                return LEDState.PRE_MATCH_RED;
            } else {
                return LEDState.PRE_MATCH_BLUE;
            }
        }

        // Autonomous
        if (phase == MatchPhase.AUTO) {
            return getAllianceSolidState(alliance);
        }

        // Transition period
        if (phase == MatchPhase.TRANSITION) {
            if (!matchStateTracker.isFmsDataReceived()) {
                return LEDState.TRANSITION_UNKNOWN;
            }
            // Blink the color of whoever starts ACTIVE (not inactive!)
            // FMS data tells us who is INACTIVE first, so active is the opposite
            return getTransitionActiveState(alliance);
        }

        // Check for endgame override (takes priority over shift states)
        if (matchTime <= ENDGAME_WHITE_TIME && matchTime > 0) {
            return LEDState.ENDGAME_URGENT;
        }
        if (matchTime <= ENDGAME_PURPLE_TIME && matchTime > 0) {
            return LEDState.ENDGAME_APPROACHING;
        }

        // Shift states
        HubStatus hubStatus = matchStateTracker.getOurHubStatus();

        if (hubStatus == HubStatus.ACTIVE) {
            return calculateActiveShiftState(alliance, timeUntilShift);
        } else if (hubStatus == HubStatus.INACTIVE) {
            return calculateInactiveShiftState(timeUntilShift);
        }

        // Endgame (both hubs active)
        if (phase == MatchPhase.ENDGAME) {
            if (matchTime <= ENDGAME_WHITE_TIME) {
                return LEDState.ENDGAME_URGENT;
            }
            return LEDState.ENDGAME_APPROACHING;
        }

        // Post-match
        if (phase == MatchPhase.POST_MATCH) {
            return LEDState.POST_MATCH;
        }

        // Fallback
        return LEDState.DISABLED;
    }

    /**
     * Returns the LED state for an active shift based on time remaining.
     */
    private LEDState calculateActiveShiftState(Alliance alliance, double timeUntilShift) {
        // Final 3 seconds: Blink white
        if (timeUntilShift <= FINAL_SHIFT_WARNING_TIME) {
            return LEDState.SHIFT_FINAL_WARNING;
        }

        // Mid-shift warning: Blink alliance color
        if (timeUntilShift <= MID_SHIFT_WARNING_TIME) {
            return getAllianceBlinkState(alliance);
        }

        // Start of shift: Solid alliance color
        return getAllianceSolidState(alliance);
    }

    /**
     * Returns the LED state for an inactive shift based on time remaining.
     */
    private LEDState calculateInactiveShiftState(double timeUntilShift) {
        // Final 3 seconds: Blink white
        if (timeUntilShift <= FINAL_SHIFT_WARNING_TIME) {
            return LEDState.SHIFT_FINAL_WARNING;
        }

        // Mid-shift warning: Blink yellow
        if (timeUntilShift <= MID_SHIFT_WARNING_TIME) {
            return LEDState.INACTIVE_WARNING;
        }

        // Start of shift: Solid yellow
        return LEDState.INACTIVE_START;
    }

    // =========================================================================
    // ALLIANCE-BASED STATE HELPERS
    // =========================================================================

    private LEDState getAllianceSolidState(Alliance alliance) {
        if (alliance == Alliance.Red) {
            return LEDState.AUTO_RED;
        } else if (alliance == Alliance.Blue) {
            return LEDState.AUTO_BLUE;
        }
        return LEDState.solid(LEDColor.PURPLE, "Unknown Alliance");
    }

    private LEDState getAllianceBlinkState(Alliance alliance) {
        if (alliance == Alliance.Red) {
            return LEDState.ACTIVE_WARNING_RED;
        } else if (alliance == Alliance.Blue) {
            return LEDState.ACTIVE_WARNING_BLUE;
        }
        return LEDState.blink(LEDColor.PURPLE, "Unknown Alliance Warning");
    }

    private LEDState getTransitionActiveState(Alliance alliance) {
        // During transition, we blink the color of whoever is ACTIVE first
        // This is critical information for the drive team!
        HubStatus nextStatus = matchStateTracker.getNextHubStatus();

        // If we're going to be active in Shift 1, blink our color
        // If opponent is active in Shift 1, blink their color
        if (nextStatus == HubStatus.ACTIVE) {
            // We're active first - blink our color
            if (alliance == Alliance.Red) {
                return LEDState.TRANSITION_RED_ACTIVE;
            } else {
                return LEDState.TRANSITION_BLUE_ACTIVE;
            }
        } else {
            // Opponent is active first - blink their color
            if (alliance == Alliance.Red) {
                return LEDState.TRANSITION_BLUE_ACTIVE;
            } else {
                return LEDState.TRANSITION_RED_ACTIVE;
            }
        }
    }

    // =========================================================================
    // DASHBOARD & LOGGING
    // =========================================================================

    private void updateDashboard() {
        SmartDashboard.putString("LED/State", currentState.getDescription());
        SmartDashboard.putString("LED/Pattern", currentState.getPattern().toString());
        SmartDashboard.putString("LED/Color", currentState.getPrimaryColor().getName());
        SmartDashboard.putBoolean("LED/HasInterrupt", hasActiveInterrupt());
        SmartDashboard.putBoolean("LED/Connected", controller.isConnected());

        // AdvantageKit logging
        Logger.recordOutput("LED/State", currentState.getDescription());
        Logger.recordOutput("LED/Pattern", currentState.getPattern().toString());
        Logger.recordOutput("LED/HasInterrupt", hasActiveInterrupt());
    }

    // =========================================================================
    // PUBLIC CONTROL
    // =========================================================================

    /**
     * Sets the LED brightness.
     *
     * @param brightness 0.0 to 1.0
     */
    public void setBrightness(double brightness) {
        controller.setBrightness(brightness);
    }

    /**
     * Forces a specific state (bypasses match-based logic until cleared).
     */
    public void forceState(LEDState state) {
        setInterrupt(state, Double.MAX_VALUE);
    }

    /**
     * Returns to automatic match-based control.
     */
    public void resumeAutomatic() {
        clearInterrupt();
    }

    /**
     * Gets the current LED state.
     */
    public LEDState getCurrentState() {
        return currentState;
    }
}
