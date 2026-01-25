// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.led;

/**
 * Hardware abstraction interface for LED controllers.
 *
 * <p>Implement this interface for each LED hardware type:
 * <ul>
 *   <li>{@code REVBlinkController} - REV Robotics Blink LED driver</li>
 *   <li>{@code CANdleController} - CTR Electronics CANdle</li>
 *   <li>{@code AddressableLEDController} - Direct RoboRIO addressable LED control</li>
 * </ul>
 *
 * <p>The LEDSubsystem calls these methods; implementations handle hardware-specific details.
 */
public interface LEDController {

    /**
     * Sets the current LED state.
     *
     * <p>The implementation should render the pattern and color(s) appropriately
     * for the specific hardware.
     *
     * @param state The desired LED state
     */
    void setState(LEDState state);

    /**
     * Sets the overall brightness.
     *
     * @param brightness 0.0 (off) to 1.0 (full brightness)
     */
    void setBrightness(double brightness);

    /**
     * Gets the current brightness setting.
     *
     * @return Current brightness (0.0 to 1.0)
     */
    double getBrightness();

    /**
     * Called periodically to update animations.
     *
     * <p>Some patterns (chase, rainbow, blink) require periodic updates.
     * Call this from LEDSubsystem.periodic().
     */
    void update();

    /**
     * Turns off all LEDs immediately.
     */
    default void off() {
        setState(LEDState.OFF);
    }

    /**
     * Returns true if the controller is connected and functioning.
     *
     * @return true if hardware is working
     */
    default boolean isConnected() {
        return true;
    }

    /**
     * Gets a description of this controller (for logging).
     *
     * @return Controller description (e.g., "CANdle on CAN ID 0")
     */
    String getDescription();
}
