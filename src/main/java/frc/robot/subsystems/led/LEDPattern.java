// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.led;

/**
 * LED display patterns.
 *
 * <p>These patterns are hardware-agnostic. Each LED controller implementation
 * is responsible for rendering these patterns appropriately.
 */
public enum LEDPattern {
    /** LEDs off. */
    OFF,

    /** Solid color (no animation). */
    SOLID,

    /** Blinking on/off at standard rate (~2Hz). */
    BLINK,

    /** Fast blinking (~4Hz) for urgent warnings. */
    BLINK_FAST,

    /** Chasing/scrolling pattern (for pre-match). */
    CHASE,

    /** Rainbow cycle (for celebrations). */
    RAINBOW,

    /** Alternating between two colors. */
    ALTERNATING,

    /** Pulsing/breathing effect (fade in/out). */
    PULSE,

    /** Strobe effect (very fast flash). */
    STROBE
}
