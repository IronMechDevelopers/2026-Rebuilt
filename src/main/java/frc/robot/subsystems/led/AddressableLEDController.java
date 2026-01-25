// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.led;

import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.Timer;

/**
 * LED controller using WPILib's AddressableLED (direct RoboRIO PWM control).
 *
 * <p>Connect addressable LEDs (WS2812B, NeoPixels, etc.) directly to a RoboRIO PWM port.
 *
 * <p><b>Wiring:</b>
 * <ul>
 *   <li>Data line → PWM port (specified in constructor)</li>
 *   <li>Power → 5V (use external power supply for >30 LEDs)</li>
 *   <li>Ground → Common ground with RoboRIO</li>
 * </ul>
 */
public class AddressableLEDController implements LEDController {

    // Hardware
    private final AddressableLED led;
    private final AddressableLEDBuffer buffer;
    private final int ledCount;

    // State
    private LEDState currentState = LEDState.OFF;
    private double brightness = 1.0;

    // Animation timing
    private double lastUpdateTime = 0;
    private int animationFrame = 0;
    private boolean blinkOn = true;

    // Animation speeds (seconds per frame)
    private static final double BLINK_PERIOD = 0.25;      // 2Hz blink
    private static final double BLINK_FAST_PERIOD = 0.125; // 4Hz blink
    private static final double CHASE_PERIOD = 0.05;      // Chase animation speed
    private static final double RAINBOW_PERIOD = 0.02;    // Rainbow cycle speed
    private static final double STROBE_PERIOD = 0.05;     // Strobe speed

    /**
     * Creates an addressable LED controller.
     *
     * @param pwmPort  The PWM port the LEDs are connected to
     * @param ledCount Number of LEDs in the strip
     */
    public AddressableLEDController(int pwmPort, int ledCount) {
        this.ledCount = ledCount;

        led = new AddressableLED(pwmPort);
        buffer = new AddressableLEDBuffer(ledCount);

        led.setLength(ledCount);
        led.setData(buffer);
        led.start();

        // Initialize to off
        setAllPixels(LEDColor.OFF);

        System.out.println("AddressableLEDController: " + ledCount + " LEDs on PWM " + pwmPort);
    }

    @Override
    public void setState(LEDState state) {
        currentState = state;
        animationFrame = 0; // Reset animation
        blinkOn = true;
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
        double now = Timer.getFPGATimestamp();

        switch (currentState.getPattern()) {
            case OFF:
                setAllPixels(LEDColor.OFF);
                break;

            case SOLID:
                setAllPixels(currentState.getPrimaryColor());
                break;

            case BLINK:
                if (now - lastUpdateTime >= BLINK_PERIOD) {
                    blinkOn = !blinkOn;
                    lastUpdateTime = now;
                }
                setAllPixels(blinkOn ? currentState.getPrimaryColor() : LEDColor.OFF);
                break;

            case BLINK_FAST:
                if (now - lastUpdateTime >= BLINK_FAST_PERIOD) {
                    blinkOn = !blinkOn;
                    lastUpdateTime = now;
                }
                setAllPixels(blinkOn ? currentState.getPrimaryColor() : LEDColor.OFF);
                break;

            case STROBE:
                if (now - lastUpdateTime >= STROBE_PERIOD) {
                    blinkOn = !blinkOn;
                    lastUpdateTime = now;
                }
                setAllPixels(blinkOn ? currentState.getPrimaryColor() : LEDColor.OFF);
                break;

            case CHASE:
                if (now - lastUpdateTime >= CHASE_PERIOD) {
                    animationFrame = (animationFrame + 1) % ledCount;
                    lastUpdateTime = now;
                }
                renderChase();
                break;

            case RAINBOW:
                if (now - lastUpdateTime >= RAINBOW_PERIOD) {
                    animationFrame = (animationFrame + 1) % 180;
                    lastUpdateTime = now;
                }
                renderRainbow();
                break;

            case ALTERNATING:
                renderAlternating();
                break;

            case PULSE:
                renderPulse(now);
                break;

            default:
                setAllPixels(currentState.getPrimaryColor());
                break;
        }

        // Send data to LEDs
        led.setData(buffer);
    }

    @Override
    public String getDescription() {
        return "Addressable LED (" + ledCount + " LEDs)";
    }

    // =========================================================================
    // RENDERING HELPERS
    // =========================================================================

    private void setAllPixels(LEDColor color) {
        int r = (int) (color.getRed() * brightness);
        int g = (int) (color.getGreen() * brightness);
        int b = (int) (color.getBlue() * brightness);

        for (int i = 0; i < ledCount; i++) {
            buffer.setRGB(i, r, g, b);
        }
    }

    private void setPixel(int index, LEDColor color) {
        if (index >= 0 && index < ledCount) {
            int r = (int) (color.getRed() * brightness);
            int g = (int) (color.getGreen() * brightness);
            int b = (int) (color.getBlue() * brightness);
            buffer.setRGB(index, r, g, b);
        }
    }

    private void renderChase() {
        LEDColor primary = currentState.getPrimaryColor();
        LEDColor secondary = currentState.hasTwoColors()
            ? currentState.getSecondaryColor()
            : primary.dim(0.2);

        for (int i = 0; i < ledCount; i++) {
            // Create a "comet tail" effect
            int distance = (i - animationFrame + ledCount) % ledCount;
            if (distance < 5) {
                // Head of the comet
                double fade = 1.0 - (distance * 0.2);
                setPixel(i, primary.dim(fade));
            } else {
                setPixel(i, secondary);
            }
        }
    }

    private void renderRainbow() {
        for (int i = 0; i < ledCount; i++) {
            int hue = (animationFrame + (i * 180 / ledCount)) % 180;
            buffer.setHSV(i, hue, 255, (int) (255 * brightness));
        }
    }

    private void renderAlternating() {
        LEDColor primary = currentState.getPrimaryColor();
        LEDColor secondary = currentState.hasTwoColors()
            ? currentState.getSecondaryColor()
            : LEDColor.OFF;

        for (int i = 0; i < ledCount; i++) {
            setPixel(i, (i % 2 == 0) ? primary : secondary);
        }
    }

    private void renderPulse(double time) {
        // Sine wave brightness pulse
        double pulse = (Math.sin(time * 4) + 1) / 2; // 0 to 1
        pulse = 0.3 + (pulse * 0.7); // 0.3 to 1.0 (never fully off)

        LEDColor dimmed = currentState.getPrimaryColor().dim(pulse);
        setAllPixels(dimmed);
    }
}
