package frc.robot.constants;

  public final class FuelConstants {
    // Motor controller IDs for Fuel Mechanism motors
    public static final int FEEDER_MOTOR_ID = 6;
    public static final int INTAKE_LAUNCHER_MOTOR_ID = 5;

    // Current limit and nominal voltage for fuel mechanism motors.
    public static final int FEEDER_MOTOR_CURRENT_LIMIT = 60;
    public static final int LAUNCHER_MOTOR_CURRENT_LIMIT = 60;

    // Voltage values for various fuel operations. These values may need to be tuned
    // based on exact robot construction.
    // See the Software Guide for tuning information
    public static final double INTAKING_FEEDER_VOLTAGE = -9;
    public static final double INTAKING_INTAKE_VOLTAGE = 10;
    public static final double LAUNCHING_FEEDER_VOLTAGE = 9;
    public static final double LAUNCHING_LAUNCHER_VOLTAGE = 12;
    public static final double SPIN_UP_FEEDER_VOLTAGE = -6;
    public static final double SPIN_UP_SECONDS = 1;
    public static final double EJECT_FEEDER_VOLTAGE=9;
    public static final double EJECT_INTAKE_vOLTAGE=-12;
  }