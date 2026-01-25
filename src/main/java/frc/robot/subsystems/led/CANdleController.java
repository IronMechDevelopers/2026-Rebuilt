// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.led;

// TODO: Uncomment when using CANdle
// import com.ctre.phoenix.led.CANdle;
// import com.ctre.phoenix.led.CANdleConfiguration;
// import com.ctre.phoenix.led.RainbowAnimation;
// import com.ctre.phoenix.led.StrobeAnimation;
// import com.ctre.phoenix.led.LarsonAnimation;

/**
 * LED controller using CTR Electronics CANdle.
 *
 * <p><b>TO USE THIS CONTROLLER:</b>
 * <ol>
 *   <li>Add Phoenix library to vendordeps</li>
 *   <li>Uncomment the imports above</li>
 *   <li>Uncomment the CANdle hardware code below</li>
 *   <li>Configure CAN ID to match your hardware</li>
 * </ol>
 *
 * <p><b>CANdle Advantages:</b>
 * <ul>
 *   <li>Built-in animations (no CPU overhead)</li>
 *   <li>Multiple LED strip support</li>
 *   <li>CAN bus communication (more reliable than PWM)</li>
 *   <li>Onboard LEDs for status</li>
 * </ul>
 */
public class CANdleController implements LEDController {

    // TODO: Uncomment when using CANdle
    // private final CANdle candle;

    private final int canId;
    private final int ledCount;
    private LEDState currentState = LEDState.OFF;
    private double brightness = 1.0;

    /**
     * Creates a CANdle controller.
     *
     * @param canId    CAN ID of the CANdle
     * @param ledCount Total number of LEDs (onboard + strip)
     */
    public CANdleController(int canId, int ledCount) {
        this.canId = canId;
        this.ledCount = ledCount;

        // TODO: Uncomment when using CANdle
        // candle = new CANdle(canId);
        //
        // CANdleConfiguration config = new CANdleConfiguration();
        // config.stripType = CANdle.LEDStripType.RGB; // or GRB depending on your strip
        // config.brightnessScalar = 1.0;
        // candle.configAllSettings(config);

        System.out.println("CANdleController: CAN ID " + canId + " with " + ledCount + " LEDs");
        System.out.println("WARNING: CANdle code is stubbed - uncomment implementation when ready");
    }

    @Override
    public void setState(LEDState state) {
        currentState = state;

        // TODO: Uncomment when using CANdle
        // LEDColor color = state.getPrimaryColor();
        //
        // switch (state.getPattern()) {
        //     case OFF:
        //         candle.setLEDs(0, 0, 0);
        //         break;
        //
        //     case SOLID:
        //         candle.setLEDs(color.getRed(), color.getGreen(), color.getBlue());
        //         break;
        //
        //     case BLINK:
        //         candle.animate(new StrobeAnimation(
        //             color.getRed(), color.getGreen(), color.getBlue(),
        //             0, 0.5, ledCount)); // 0.5 = 2Hz
        //         break;
        //
        //     case BLINK_FAST:
        //         candle.animate(new StrobeAnimation(
        //             color.getRed(), color.getGreen(), color.getBlue(),
        //             0, 0.25, ledCount)); // 0.25 = 4Hz
        //         break;
        //
        //     case CHASE:
        //         candle.animate(new LarsonAnimation(
        //             color.getRed(), color.getGreen(), color.getBlue(),
        //             0, 0.5, ledCount, LarsonAnimation.BounceMode.Front, 7));
        //         break;
        //
        //     case RAINBOW:
        //         candle.animate(new RainbowAnimation(1.0, 0.5, ledCount));
        //         break;
        //
        //     default:
        //         candle.setLEDs(color.getRed(), color.getGreen(), color.getBlue());
        //         break;
        // }
    }

    @Override
    public void setBrightness(double brightness) {
        this.brightness = Math.max(0, Math.min(1, brightness));
        // TODO: Uncomment when using CANdle
        // candle.configBrightnessScalar(this.brightness);
    }

    @Override
    public double getBrightness() {
        return brightness;
    }

    @Override
    public void update() {
        // CANdle handles animations internally - no update needed
    }

    @Override
    public boolean isConnected() {
        // TODO: Uncomment when using CANdle
        // return candle.getLastError() == ErrorCode.OK;
        return false; // Stubbed - always returns false until implemented
    }

    @Override
    public String getDescription() {
        return "CANdle (CAN ID " + canId + ", " + ledCount + " LEDs) [STUBBED]";
    }
}
