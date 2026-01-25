// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.led;

import edu.wpi.first.wpilibj.motorcontrol.Spark;

/**
 * LED controller using REV Robotics Blink LED driver.
 *
 * <p>The Blink is controlled via a PWM signal that selects preset patterns.
 * Connect the Blink to a PWM port and set values between -1.0 and 1.0 to
 * select different patterns/colors.
 *
 * <p><b>REV Blink Limitations:</b>
 * <ul>
 *   <li>Only preset patterns (can't set arbitrary RGB)</li>
 *   <li>Limited animation options</li>
 *   <li>Must map LEDState to closest Blink pattern</li>
 * </ul>
 *
 * <p><b>Blink Pattern Values:</b>
 * See REV documentation for full list. Common values:
 * <ul>
 *   <li>-0.99: Rainbow</li>
 *   <li>-0.91: Red</li>
 *   <li>-0.87: Blue</li>
 *   <li>-0.69: Yellow</li>
 *   <li>-0.57: Green</li>
 *   <li>0.15: Red strobe</li>
 *   <li>0.17: Blue strobe</li>
 *   <li>0.99: Off</li>
 * </ul>
 */
public class REVBlinkController implements LEDController {

    // Blink pattern values (from REV documentation)
    private static final double PATTERN_RAINBOW = -0.99;
    private static final double PATTERN_RED_SOLID = -0.11;
    private static final double PATTERN_BLUE_SOLID = -0.87;
    private static final double PATTERN_GREEN_SOLID = -0.77;
    private static final double PATTERN_YELLOW_SOLID = -0.69;
    private static final double PATTERN_PURPLE_SOLID = -0.91;
    private static final double PATTERN_WHITE_SOLID = -0.81;
    private static final double PATTERN_ORANGE_SOLID = -0.65;

    private static final double PATTERN_RED_STROBE = 0.15;
    private static final double PATTERN_BLUE_STROBE = 0.17;
    private static final double PATTERN_GOLD_STROBE = 0.07;
    private static final double PATTERN_WHITE_STROBE = 0.05;

    private static final double PATTERN_RED_HEARTBEAT = 0.25;
    private static final double PATTERN_BLUE_HEARTBEAT = 0.27;

    private static final double PATTERN_OFF = 0.99;

    private final Spark blink;
    private LEDState currentState = LEDState.OFF;
    private double brightness = 1.0;

    /**
     * Creates a REV Blink controller.
     *
     * @param pwmPort PWM port the Blink is connected to
     */
    public REVBlinkController(int pwmPort) {
        blink = new Spark(pwmPort);
        blink.set(PATTERN_OFF);

        System.out.println("REVBlinkController: PWM port " + pwmPort);
    }

    @Override
    public void setState(LEDState state) {
        currentState = state;
        double pattern = mapStateToPattern(state);
        blink.set(pattern);
    }

    /**
     * Maps an LEDState to the closest Blink pattern value.
     */
    private double mapStateToPattern(LEDState state) {
        LEDPattern pattern = state.getPattern();
        LEDColor color = state.getPrimaryColor();

        // Handle OFF
        if (pattern == LEDPattern.OFF) {
            return PATTERN_OFF;
        }

        // Handle RAINBOW
        if (pattern == LEDPattern.RAINBOW) {
            return PATTERN_RAINBOW;
        }

        // Map color to pattern based on pattern type
        boolean isBlinking = pattern == LEDPattern.BLINK ||
                            pattern == LEDPattern.BLINK_FAST ||
                            pattern == LEDPattern.STROBE;

        // Determine color family
        if (color.equals(LEDColor.RED)) {
            return isBlinking ? PATTERN_RED_STROBE : PATTERN_RED_SOLID;
        } else if (color.equals(LEDColor.BLUE)) {
            return isBlinking ? PATTERN_BLUE_STROBE : PATTERN_BLUE_SOLID;
        } else if (color.equals(LEDColor.GREEN)) {
            return PATTERN_GREEN_SOLID; // No green strobe in Blink
        } else if (color.equals(LEDColor.YELLOW)) {
            return isBlinking ? PATTERN_GOLD_STROBE : PATTERN_YELLOW_SOLID;
        } else if (color.equals(LEDColor.GOLD) || color.equals(LEDColor.ORANGE)) {
            return isBlinking ? PATTERN_GOLD_STROBE : PATTERN_ORANGE_SOLID;
        } else if (color.equals(LEDColor.PURPLE)) {
            return PATTERN_PURPLE_SOLID; // No purple strobe in Blink
        } else if (color.equals(LEDColor.WHITE)) {
            return isBlinking ? PATTERN_WHITE_STROBE : PATTERN_WHITE_SOLID;
        }

        // Default fallback
        return PATTERN_OFF;
    }

    @Override
    public void setBrightness(double brightness) {
        // REV Blink doesn't support variable brightness
        this.brightness = brightness;
    }

    @Override
    public double getBrightness() {
        return brightness;
    }

    @Override
    public void update() {
        // Blink handles animations internally - no update needed
    }

    @Override
    public String getDescription() {
        return "REV Blink LED Driver";
    }
}
