// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.led;

/**
 * Represents a complete LED state (pattern + color + optional second color).
 *
 * <p>Used to define what the LEDs should display at any given moment.
 * Can represent simple states (solid red) or complex ones (alternating blue/gold chase).
 */
public class LEDState {

    private final LEDPattern pattern;
    private final LEDColor primaryColor;
    private final LEDColor secondaryColor;
    private final String description;

    // =========================================================================
    // PREDEFINED STATES - Match Phases
    // =========================================================================

    /** Pre-match: Chasing alliance color (or blue/gold if unknown). */
    public static final LEDState PRE_MATCH_UNKNOWN = new LEDState(
        LEDPattern.CHASE, LEDColor.BLUE, LEDColor.GOLD, "Pre-Match (Unknown Alliance)"
    );

    /** Pre-match: Chasing red. */
    public static final LEDState PRE_MATCH_RED = new LEDState(
        LEDPattern.CHASE, LEDColor.RED, "Pre-Match (Red Alliance)"
    );

    /** Pre-match: Chasing blue. */
    public static final LEDState PRE_MATCH_BLUE = new LEDState(
        LEDPattern.CHASE, LEDColor.BLUE, "Pre-Match (Blue Alliance)"
    );

    /** Autonomous: Solid alliance color (red). */
    public static final LEDState AUTO_RED = new LEDState(
        LEDPattern.SOLID, LEDColor.RED, "Auto (Red)"
    );

    /** Autonomous: Solid alliance color (blue). */
    public static final LEDState AUTO_BLUE = new LEDState(
        LEDPattern.SOLID, LEDColor.BLUE, "Auto (Blue)"
    );

    /** Transition: Solid purple (waiting for FMS data). */
    public static final LEDState TRANSITION_UNKNOWN = new LEDState(
        LEDPattern.SOLID, LEDColor.PURPLE, "Transition (Waiting for FMS)"
    );

    /** Transition: Blinking red (Red starts active). */
    public static final LEDState TRANSITION_RED_ACTIVE = new LEDState(
        LEDPattern.BLINK, LEDColor.RED, "Transition (Red Active First)"
    );

    /** Transition: Blinking blue (Blue starts active). */
    public static final LEDState TRANSITION_BLUE_ACTIVE = new LEDState(
        LEDPattern.BLINK, LEDColor.BLUE, "Transition (Blue Active First)"
    );

    // =========================================================================
    // PREDEFINED STATES - Active Shift
    // =========================================================================

    /** Active shift start: Solid alliance color (red). */
    public static final LEDState ACTIVE_START_RED = new LEDState(
        LEDPattern.SOLID, LEDColor.RED, "Active Shift (Red)"
    );

    /** Active shift start: Solid alliance color (blue). */
    public static final LEDState ACTIVE_START_BLUE = new LEDState(
        LEDPattern.SOLID, LEDColor.BLUE, "Active Shift (Blue)"
    );

    /** Active shift mid-warning: Blinking alliance color (red). */
    public static final LEDState ACTIVE_WARNING_RED = new LEDState(
        LEDPattern.BLINK, LEDColor.RED, "Active Shift Warning (Red)"
    );

    /** Active shift mid-warning: Blinking alliance color (blue). */
    public static final LEDState ACTIVE_WARNING_BLUE = new LEDState(
        LEDPattern.BLINK, LEDColor.BLUE, "Active Shift Warning (Blue)"
    );

    /** Active/Inactive shift final 3s: Blinking white. */
    public static final LEDState SHIFT_FINAL_WARNING = new LEDState(
        LEDPattern.BLINK_FAST, LEDColor.WHITE, "Shift Final Warning (3s)"
    );

    // =========================================================================
    // PREDEFINED STATES - Inactive Shift
    // =========================================================================

    /** Inactive shift start: Solid yellow. */
    public static final LEDState INACTIVE_START = new LEDState(
        LEDPattern.SOLID, LEDColor.YELLOW, "Inactive Shift"
    );

    /** Inactive shift mid-warning: Blinking yellow. */
    public static final LEDState INACTIVE_WARNING = new LEDState(
        LEDPattern.BLINK, LEDColor.YELLOW, "Inactive Shift Warning"
    );

    // =========================================================================
    // PREDEFINED STATES - Endgame
    // =========================================================================

    /** Endgame approaching (35s left): Blinking purple. */
    public static final LEDState ENDGAME_APPROACHING = new LEDState(
        LEDPattern.BLINK, LEDColor.PURPLE, "Endgame Approaching"
    );

    /** Endgame urgent (16s left): Blinking white. */
    public static final LEDState ENDGAME_URGENT = new LEDState(
        LEDPattern.BLINK_FAST, LEDColor.WHITE, "Endgame Urgent"
    );

    /** Post-match: Rainbow celebration. */
    public static final LEDState POST_MATCH = new LEDState(
        LEDPattern.RAINBOW, LEDColor.WHITE, "Post-Match Celebration"
    );

    // =========================================================================
    // PREDEFINED STATES - Interrupts
    // =========================================================================

    /** Shooter ready: Flashing green. */
    public static final LEDState SHOOTER_READY = new LEDState(
        LEDPattern.BLINK_FAST, LEDColor.GREEN, "Shooter Ready"
    );

    /** Game piece acquired: Flashing purple/cyan. */
    public static final LEDState GAME_PIECE_ACQUIRED = new LEDState(
        LEDPattern.BLINK, LEDColor.PURPLE, LEDColor.CYAN, "Game Piece Acquired"
    );

    /** Error/fault: Flashing hot pink. */
    public static final LEDState ERROR = new LEDState(
        LEDPattern.STROBE, LEDColor.HOT_PINK, "Error/Fault"
    );

    /** Disabled: Dim white or off. */
    public static final LEDState DISABLED = new LEDState(
        LEDPattern.SOLID, LEDColor.DIM_WHITE, "Disabled"
    );

    /** Off state. */
    public static final LEDState OFF = new LEDState(
        LEDPattern.OFF, LEDColor.OFF, "Off"
    );

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    /**
     * Creates an LED state with a single color.
     *
     * @param pattern     The display pattern
     * @param color       The primary color
     * @param description Human-readable description
     */
    public LEDState(LEDPattern pattern, LEDColor color, String description) {
        this.pattern = pattern;
        this.primaryColor = color;
        this.secondaryColor = LEDColor.OFF;
        this.description = description;
    }

    /**
     * Creates an LED state with two colors (for alternating/chase patterns).
     *
     * @param pattern        The display pattern
     * @param primaryColor   The primary color
     * @param secondaryColor The secondary color
     * @param description    Human-readable description
     */
    public LEDState(LEDPattern pattern, LEDColor primaryColor, LEDColor secondaryColor, String description) {
        this.pattern = pattern;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.description = description;
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public LEDPattern getPattern() {
        return pattern;
    }

    public LEDColor getPrimaryColor() {
        return primaryColor;
    }

    public LEDColor getSecondaryColor() {
        return secondaryColor;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasTwoColors() {
        return !secondaryColor.equals(LEDColor.OFF);
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Creates a solid color state.
     */
    public static LEDState solid(LEDColor color, String description) {
        return new LEDState(LEDPattern.SOLID, color, description);
    }

    /**
     * Creates a blinking state.
     */
    public static LEDState blink(LEDColor color, String description) {
        return new LEDState(LEDPattern.BLINK, color, description);
    }

    /**
     * Creates a fast blinking state.
     */
    public static LEDState blinkFast(LEDColor color, String description) {
        return new LEDState(LEDPattern.BLINK_FAST, color, description);
    }

    /**
     * Creates a chase pattern state.
     */
    public static LEDState chase(LEDColor primary, LEDColor secondary, String description) {
        return new LEDState(LEDPattern.CHASE, primary, secondary, description);
    }

    @Override
    public String toString() {
        return description;
    }
}
