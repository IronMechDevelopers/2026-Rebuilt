# Team 5684 — 2026 REBUILT Robot Code

FRC Team 5684's robot code for the 2026 REBUILT season. Swerve drive, command-based Java, WPILib 2026.

---

## Hardware

| Component | Details |
|-----------|---------|
| Drivetrain | REV MAXSwerve (4-module), 29"×29" chassis |
| Drive Motors | REV NEO (Spark MAX), 14T pinion |
| Sensors | NavX (gyro), PhotonVision cameras |
| Controllers | Driver Xbox (port 0), Co-Driver Xbox (port 1) |

**CAN IDs:**

| Module | Drive | Turn |
|--------|-------|------|
| Front Left | 10 | 11 |
| Front Right | 20 | 21 |
| Rear Right | 30 | 31 |
| Rear Left | 40 | 41 |

---

## Software Dependencies

| Library | Purpose |
|---------|---------|
| WPILib 2026.2.1 | Robot framework |
| REVLib | Spark MAX / NEO motor control |
| AdvantageKit | Structured logging & replay |
| PathPlanner | Autonomous path following |
| PhotonLib | Vision / AprilTag tracking |

---

## Project Structure

```
src/main/java/frc/robot/
├── Robot.java              # Entry point, AdvantageKit setup
├── RobotContainer.java     # Wires everything together
├── Configs.java            # Spark MAX configurations
│
├── commands/               # Drive, vision, and utility commands
│   ├── DriveCommands.java
│   ├── BasicDriveCommands.java
│   ├── VisionDriveCommands.java
│   └── UtilityCommands.java
│
├── config/                 # Setup files (edit these to configure robot)
│   ├── ButtonBindings.java   ← button mappings
│   ├── AutoSelector.java     ← autonomous routines
│   ├── DashboardSetup.java   ← Shuffleboard layout
│   ├── SubsystemSetup.java   ← subsystem creation
│   └── VisionSetup.java      ← camera configuration
│
├── subsystems/
│   ├── DriveSubsystem.java
│   ├── MAXSwerveModule.java
│   ├── CANFuelSubsystem.java
│   ├── VisionSubsystem.java
│   ├── MatchStateTracker.java
│   └── Sim*.java             # Simulation providers
│
└── constants/              # Tuning values and hardware IDs
    ├── HardwareConstants.java  ← CAN IDs, motor config
    ├── DriveConstants.java     ← speeds, PID gains
    ├── VisionConstants.java    ← camera names, transforms
    ├── FieldConstants.java     ← field geometry
    ├── FuelConstants.java      ← shooter timing
    └── MatchConstants.java
```

---

## Controller Layout

See [`docs/ControllerMapping.md`](docs/ControllerMapping.md) for full details.

**Driver (port 0):**

| Button | Action |
|--------|--------|
| Left Stick | Drive (translate) |
| Right Stick X | Rotate |
| Left Stick Click | Reset gyro |
| A | Hub Lock (aim + X-stance) |
| B | Toggle robot-relative drive |
| X | Toggle X-stance |
| Y (hold) | Precision mode (50% speed) |
| Left Trigger (hold) | Intake |
| Left Bumper (hold) | Eject |
| Right Trigger (hold) | Shoot |
| Right Bumper (hold) | Diamond snap (45°) |
| POV Up | Sniper mode (track hub + strafe) |

**Co-Driver (port 1):**

| Button | Action |
|--------|--------|
| X (hold) | Intake |
| B (hold) | Eject |
| A (hold) | Shoot |
| Y (hold) | Square snap (90°) |

---

## Autonomous Routines

Selected via Shuffleboard before the match:

- **None** — sits still
- **Drive Back and Shoot** — backs up ~2 ft, then shoots
- **Shoot then Drive Back** — shoots, then backs up ~3 ft
- **Do Nothing** — explicit no-op

PathPlanner paths go in `src/main/deploy/pathplanner/`. Named commands registered in `AutoSelector.registerNamedCommands()`.

---

## Building & Deploying

```bash
# Build
./gradlew build

# Deploy to RoboRIO (robot must be connected)
./gradlew deploy

# Run simulation (opens SimGUI + DriverStation)
./gradlew simulateJava
```

Requires Java 17 and the WPILib VS Code extension (or standalone tools).

---

## Logging (AdvantageKit)

- **Real robot:** logs written to USB stick at `/U/logs/` and streamed over NetworkTables
- **Simulation:** streams over NetworkTables only
- Open `.wpilog` files in AdvantageScope to replay matches

Logged signals: driver inputs, match state, active commands, CAN bus health, drive odometry, vision pose estimates.

---

## Tuning Constants

| What | File | Key |
|------|------|-----|
| Drive speeds / joystick deadband | `DriveConstants.java` | `kMaxSpeed`, `kJoystickDeadband` |
| Auto-aim PID | `DriveConstants.java` | `kAutoAimP/I/D` |
| Swerve module PID | `HardwareConstants.java` | `kDrivingP`, `kTurningP` |
| Wheel size / pinion | `HardwareConstants.java` | `kWheelDiameterMeters`, `kDrivingMotorPinionTeeth` |
| Shooter timing | `FuelConstants.java` | `SPIN_UP_SECONDS` |
| Camera names / positions | `VisionConstants.java` | — |

---

## Documentation

| Doc | Topic |
|-----|-------|
| [`docs/ControllerMapping.md`](docs/ControllerMapping.md) | Full button map with diagrams |
| [`docs/DriveAndVision.md`](docs/DriveAndVision.md) | Drive + vision system overview |
| [`docs/VisionPipelineExplained.md`](docs/VisionPipelineExplained.md) | How vision pose estimation works |
| [`docs/VisionCalibrationAndDebugging.md`](docs/VisionCalibrationAndDebugging.md) | Camera calibration steps |
| [`docs/PIDTuningGuide.md`](docs/PIDTuningGuide.md) | How to tune PID loops |
| [`docs/DashboardBestPractices.md`](docs/DashboardBestPractices.md) | Shuffleboard setup tips |
| [`docs/LEDSystem.md`](docs/LEDSystem.md) | LED controller usage |
