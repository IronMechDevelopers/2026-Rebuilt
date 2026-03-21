// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.SparkMax;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import static frc.robot.constants.FuelConstants.*;

public class CANFuelSubsystem extends SubsystemBase {
  private final SparkMax indexerRoller;
  private final SparkMax mainRoller;

  // State tracking for logging
  private String currentState = "STOPPED";

  /** Creates a new CANBallSubsystem. */
  public CANFuelSubsystem() {
    // create brushed motors for each of the motors on the launcher mechanism
    mainRoller = new SparkMax(MAIN_ROLLER_MOTOR_ID, MotorType.kBrushed);
    indexerRoller = new SparkMax(INDEXER_MOTOR_ID, MotorType.kBrushed);

    // put default values for various fuel operations onto the dashboard
    // all methods in this subsystem pull their values from the dashbaord to allow
    // you to tune the values easily, and then replace the values in Constants.java
    // with your new values. For more information, see the Software Guide.
    SmartDashboard.putNumber("Intaking indexer roller value", INTAKING_INDEXER_VOLTAGE);
    SmartDashboard.putNumber("Intaking main roller value", INTAKING_MAIN_ROLLER_VOLTAGE);
    SmartDashboard.putNumber("Launching indexer roller value", LAUNCHING_INDEXER_VOLTAGE);
    SmartDashboard.putNumber("Launching main roller value", LAUNCHING_MAIN_ROLLER_VOLTAGE);
    SmartDashboard.putNumber("Spin-up indexer roller value", SPIN_UP_INDEXER_VOLTAGE);
    SmartDashboard.putNumber("Slow indexer", LAUNCHING_SLOW_INDEXER_VOLTAGE);
    SmartDashboard.putNumber("Slow Launching", LAUNCHING_MAIN_ROLLER_SLOW_VOLTAGE);

    // create the configuration for the indexer roller, set a current limit and apply
    // the config to the controller
    SparkMaxConfig indexerConfig = new SparkMaxConfig();
    indexerConfig.smartCurrentLimit(INDEXER_CURRENT_LIMIT);
    //indexerConfig.inverted(true);
    indexerRoller.configure(indexerConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    // create the configuration for the main roller, set a current limit, set
    // the motor to inverted so that positive values are used for both intaking and
    // launching, and apply the config to the controller
    SparkMaxConfig mainRollerConfig = new SparkMaxConfig();
    //mainRollerConfig.inverted(true);
    mainRollerConfig.smartCurrentLimit(MAIN_ROLLER_CURRENT_LIMIT);
    mainRoller.configure(mainRollerConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
  }

  // A method to set the rollers to values for intaking
  public void intake() {
    currentState = "INTAKING";
    indexerRoller.setVoltage(SmartDashboard.getNumber("Intaking indexer roller value", INTAKING_INDEXER_VOLTAGE));
    mainRoller
        .setVoltage(SmartDashboard.getNumber("Intaking main roller value", INTAKING_MAIN_ROLLER_VOLTAGE));
  }

    public void slowIntake() {
    currentState = "SLOW_INTAKING";
    indexerRoller.setVoltage(SmartDashboard.getNumber("Intaking indexer roller value", INTAKING_INDEXER_VOLTAGE));
    mainRoller
        .setVoltage(SmartDashboard.getNumber("Intaking main roller value", INTAKING_MAIN_ROLLER_VOLTAGE));
  }

  // A method to set the rollers to values for ejecting fuel out the intake. Uses
  // the same values as intaking, but in the opposite direction.
  public void eject() {
    currentState = "EJECTING";
    indexerRoller
        .setVoltage(EJECT_INDEXER_VOLTAGE);
    mainRoller
        .setVoltage(EJECT_MAIN_ROLLER_VOLTAGE);
  }

  // A method to set the rollers to values for launching.
  public void launch() {
    currentState = "LAUNCHING";
    indexerRoller.setVoltage(SmartDashboard.getNumber("Launching indexer roller value", LAUNCHING_INDEXER_VOLTAGE));
    mainRoller
        .setVoltage(SmartDashboard.getNumber("Launching main roller value", LAUNCHING_MAIN_ROLLER_VOLTAGE));
  }


    public void launchSlow() {
    currentState = "LAUNCHING_SLOW";
    indexerRoller.setVoltage(SmartDashboard.getNumber("Slow indexer", LAUNCHING_SLOW_INDEXER_VOLTAGE));
    mainRoller
        .setVoltage(SmartDashboard.getNumber("Slow Launching", LAUNCHING_MAIN_ROLLER_SLOW_VOLTAGE));
  }

  // A method to stop the rollers
  public void stop() {
    currentState = "STOPPED";
    indexerRoller.set(0);
    mainRoller.set(0);
  }

  // A method to spin up the main roller while spinning the indexer roller to
  // push Fuel away from the launcher
  public void spinUp() {
    currentState = "SPINNING_UP";
    indexerRoller
        .setVoltage(SmartDashboard.getNumber("Spin-up indexer roller value", SPIN_UP_INDEXER_VOLTAGE));
    mainRoller
        .setVoltage(SmartDashboard.getNumber("Launching main roller value", LAUNCHING_MAIN_ROLLER_VOLTAGE));
  }

  public void spinUpAuto(){
    currentState = "SPINNING_UP_AUTO";
        indexerRoller
        .setVoltage(0);
    mainRoller
        .setVoltage(SmartDashboard.getNumber("Launching main roller value", LAUNCHING_MAIN_ROLLER_VOLTAGE));
  }

  // A command factory to turn the spinUp method into a command that requires this
  // subsystem
  public Command spinUpCommand() {
    return this.run(() -> spinUp());
  }

    public Command spinUpAutoCommand() {
    return this.run(() -> spinUpAuto());
  }

  // A command factory to turn the launch method into a command that requires this
  // subsystem
  public Command launchCommand() {
    return this.run(() -> launch());
  }


    // A command factory to turn the launch method into a command that requires this
  // subsystem
  public Command launchSlowCommand() {
    return this.run(() -> launchSlow());
  }

  // A command factory to turn the intake method into a command that requires this
  // subsystem
  public Command intakeCommand() {
    return this.run(() -> intake());
  }
  public Command slowIntakeCommand() {
    return this.run(() -> slowIntake());
  }

  // A command factory to turn the eject method into a command that requires this
  // subsystem
  public Command ejectCommand() {
    return this.run(() -> eject());
  }

  @Override
  public void periodic() {
    // =====================================================================══
    // ADVANTAGEKIT LOGGING - Fuel handling telemetry for AdvantageScope
    // =====================================================================══
    //
    // All data logged here is available in AdvantageScope for post-match analysis.
    // Use this to debug fuel handling issues, tune voltages, and verify states.
    //
    // NOTE: CIM motors don't have encoders, so we log current/voltage/state only
    //
    // =====================================================================══

    // --- STATE TRACKING (For debugging command sequencing) ---
    Logger.recordOutput("Fuel/State", currentState);

    // --- INDEXER ROLLER TELEMETRY ---
    Logger.recordOutput("Fuel/Indexer/Current", indexerRoller.getOutputCurrent());
    Logger.recordOutput("Fuel/Indexer/AppliedOutput", indexerRoller.getAppliedOutput());
    Logger.recordOutput("Fuel/Indexer/Voltage", indexerRoller.getAppliedOutput() * indexerRoller.getBusVoltage());

    // --- MAIN ROLLER TELEMETRY ---
    Logger.recordOutput("Fuel/MainRoller/Current", mainRoller.getOutputCurrent());
    Logger.recordOutput("Fuel/MainRoller/AppliedOutput", mainRoller.getAppliedOutput());
    Logger.recordOutput("Fuel/MainRoller/Voltage", mainRoller.getAppliedOutput() * mainRoller.getBusVoltage());
  }
}
