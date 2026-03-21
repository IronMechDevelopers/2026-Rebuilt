package frc.robot.constants;

  public final class FuelConstants {
    // Motor controller IDs for Fuel Mechanism motors
    public static final int INDEXER_MOTOR_ID = 6;
    public static final int MAIN_ROLLER_MOTOR_ID = 5;

    // Current limit and nominal voltage for fuel mechanism motors.
    public static final int INDEXER_CURRENT_LIMIT = 60;
    public static final int MAIN_ROLLER_CURRENT_LIMIT = 60;

    // Voltage values for various fuel operations. These values may need to be tuned
    // based on exact robot construction.
    // See the Software Guide for tuning information

    // INTAKING operation
    public static final double INTAKING_INDEXER_VOLTAGE = -10.5;
    public static final double INTAKING_MAIN_ROLLER_VOLTAGE = 10;

        // INTAKING operation
    public static final double SLOW_INTAKING_INDEXER_VOLTAGE = -9;
    public static final double SLOW_INTAKING_MAIN_ROLLER_VOLTAGE = 8;


    // LAUNCHING operation
    public static final double LAUNCHING_INDEXER_VOLTAGE = 9; // usually is 
    public static final double LAUNCHING_MAIN_ROLLER_VOLTAGE = 12;

    public static final double LAUNCHING_MAIN_ROLLER_SLOW_VOLTAGE = 6;
    public static final double LAUNCHING_SLOW_INDEXER_VOLTAGE = 6; 

    // SPIN_UP operation
    public static final double SPIN_UP_INDEXER_VOLTAGE = -6;
    public static final double SPIN_UP_SECONDS = 1;
    public static final double SPIN_UP_SECONDS_AUTO = 2;

    // EJECT operation
    public static final double EJECT_INDEXER_VOLTAGE = 9;
    public static final double EJECT_MAIN_ROLLER_VOLTAGE = -11.5;
  }