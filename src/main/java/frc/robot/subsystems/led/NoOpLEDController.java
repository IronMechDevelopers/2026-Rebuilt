// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.led;

/**
 * No-operation LED controller for testing without hardware.
 *
 * <p>Use this when:
 * <ul>
 *   <li>Running in simulation</li>
 *   <li>Testing without LED hardware connected</li>
 *   <li>Debugging LED logic without physical LEDs</li>
 * </ul>
 *
 * <p>All methods log to console but do nothing else.
 */
public class NoOpLEDController implements LEDController {

    private LEDState currentState = LEDState.OFF;
    private double brightness = 1.0;
    private final boolean verbose;

    /**
     * Creates a no-op controller.
     *
     * @param verbose If true, prints state changes to console
     */
    public NoOpLEDController(boolean verbose) {
        this.verbose = verbose;
        if (verbose) {
            System.out.println("NoOpLEDController: Initialized (no hardware)");
        }
    }

    /**
     * Creates a quiet no-op controller (no console output).
     */
    public NoOpLEDController() {
        this(false);
    }

    @Override
    public void setState(LEDState state) {
        if (verbose && !state.equals(currentState)) {
            System.out.println("NoOpLED: " + state.getDescription());
        }
        currentState = state;
    }

    @Override
    public void setBrightness(double brightness) {
        this.brightness = Math.max(0, Math.min(1, brightness));
    }

    @Override
    public double getBrightness() {
        return brightness;
    }

    @Override
    public void update() {
        // No-op - nothing to update
    }

    @Override
    public String getDescription() {
        return "No-Op LED Controller (simulation/testing)";
    }
}
