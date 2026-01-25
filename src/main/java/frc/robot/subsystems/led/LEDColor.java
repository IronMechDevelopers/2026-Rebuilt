// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.led;

/**
 * LED color definitions.
 *
 * <p>Provides predefined colors for common FRC use cases plus the ability
 * to create custom colors.
 */
public class LEDColor {

    private final int red;
    private final int green;
    private final int blue;
    private final String name;

    // =========================================================================
    // PREDEFINED COLORS - Alliance & Game
    // =========================================================================

    /** FRC Red Alliance color. */
    public static final LEDColor RED = new LEDColor(255, 0, 0, "Red");

    /** FRC Blue Alliance color. */
    public static final LEDColor BLUE = new LEDColor(0, 0, 255, "Blue");

    /** Gold/Yellow - for unknown alliance or warnings. */
    public static final LEDColor GOLD = new LEDColor(255, 180, 0, "Gold");

    /** Yellow - for inactive shift. */
    public static final LEDColor YELLOW = new LEDColor(255, 255, 0, "Yellow");

    /** Purple - for transition period. */
    public static final LEDColor PURPLE = new LEDColor(128, 0, 128, "Purple");

    /** White - for final countdown warnings. */
    public static final LEDColor WHITE = new LEDColor(255, 255, 255, "White");

    /** Green - for shooter ready, success states. */
    public static final LEDColor GREEN = new LEDColor(0, 255, 0, "Green");

    /** Orange - for caution/warning states. */
    public static final LEDColor ORANGE = new LEDColor(255, 100, 0, "Orange");

    // =========================================================================
    // PREDEFINED COLORS - Utility
    // =========================================================================

    /** LEDs off. */
    public static final LEDColor OFF = new LEDColor(0, 0, 0, "Off");

    /** Dim white for low-power/standby. */
    public static final LEDColor DIM_WHITE = new LEDColor(50, 50, 50, "Dim White");

    /** Hot pink - for errors/faults. */
    public static final LEDColor HOT_PINK = new LEDColor(255, 0, 100, "Hot Pink");

    /** Cyan - for intake/indexer states. */
    public static final LEDColor CYAN = new LEDColor(0, 255, 255, "Cyan");

    // =========================================================================
    // TEAM COLORS (customize these for your team!)
    // =========================================================================

    /** Team primary color - customize in code. */
    public static final LEDColor TEAM_PRIMARY = new LEDColor(0, 0, 255, "Team Primary");

    /** Team secondary color - customize in code. */
    public static final LEDColor TEAM_SECONDARY = new LEDColor(255, 180, 0, "Team Secondary");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Creates a custom LED color.
     *
     * @param red   Red component (0-255)
     * @param green Green component (0-255)
     * @param blue  Blue component (0-255)
     * @param name  Human-readable name for logging
     */
    public LEDColor(int red, int green, int blue, String name) {
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
        this.name = name;
    }

    /**
     * Creates a custom LED color with auto-generated name.
     *
     * @param red   Red component (0-255)
     * @param green Green component (0-255)
     * @param blue  Blue component (0-255)
     */
    public LEDColor(int red, int green, int blue) {
        this(red, green, blue, String.format("RGB(%d,%d,%d)", red, green, blue));
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns color as a single integer (0xRRGGBB format).
     */
    public int toInt() {
        return (red << 16) | (green << 8) | blue;
    }

    /**
     * Returns color as normalized doubles (0.0-1.0) for WPILib Color.
     */
    public double[] toNormalized() {
        return new double[] { red / 255.0, green / 255.0, blue / 255.0 };
    }

    // =========================================================================
    // COLOR MANIPULATION
    // =========================================================================

    /**
     * Returns a dimmed version of this color.
     *
     * @param factor Brightness factor (0.0 = off, 1.0 = full)
     */
    public LEDColor dim(double factor) {
        factor = Math.max(0, Math.min(1, factor));
        return new LEDColor(
            (int) (red * factor),
            (int) (green * factor),
            (int) (blue * factor),
            name + " (dimmed)"
        );
    }

    /**
     * Blends this color with another color.
     *
     * @param other  The other color
     * @param factor Blend factor (0.0 = this, 1.0 = other)
     */
    public LEDColor blend(LEDColor other, double factor) {
        factor = Math.max(0, Math.min(1, factor));
        return new LEDColor(
            (int) (red + (other.red - red) * factor),
            (int) (green + (other.green - green) * factor),
            (int) (blue + (other.blue - blue) * factor),
            name + " blend"
        );
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LEDColor other) {
            return red == other.red && green == other.green && blue == other.blue;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toInt();
    }
}
