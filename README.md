# Team 5684 - 2026 Robot Code

This repository contains the code for Team 5684's 2026 FRC robot.

## Robot Features

- **Swerve Drive**: REV MAXSwerve modules with field-centric and robot-centric control
- **Vision**: PhotonVision integration for pose estimation and targeting
- **Fuel System**: Intake, shooter, and ejection mechanisms
- **Data Logging**: AdvantageKit for comprehensive match logging and replay
- **Autonomous**: PathPlanner-based autonomous routines

## Controller Layout

### Driver (Xbox Controller)

**Movement:**
- Left Stick: Robot translation (forward/backward/strafe)
- Right Stick: Robot rotation

**Buttons:**
- **Back Button**: Zero heading (reset gyro)
- **Left Trigger**: Intake (hold)
- **Left Bumper**: Eject (hold)
- **Right Trigger**: Shoot (spin up 1s then launch, hold)
- **Right Bumper**: Diamond snap - Snap to 45° angles (hold)
- **Y Button**: Square snap - Snap to 90° angles (hold)
- **B Button**: Toggle field-relative mode
- **X Button**: X-Stance (hold)

**D-Pad (POV):**
- **POV Up**: Set speed to 100%
- **POV Down**: Set speed to 25%
- **POV Right**: Set speed to 50%
- **POV Left**: Set speed to 75%

### Co-Driver (PS4 Controller)

- **Square (□)**: Intake (hold)
- **Circle (○)**: Eject (hold)
- **Cross (✕)**: Shoot (spin up 1s then launch, hold)
- **Triangle (△)**: Slow intake (hold)

## Autonomous Routines

Available autonomous routines (selectable from dashboard):
- **Shoot Gather Shoot**: Shoot preload, gather more fuel, shoot again
- **Right Side Spin**: Right side starting position with spin maneuver
- **Right Hub Shoot**: Right side start, shoot at hub
- **Center Shoot**: Center starting position, shoot preload
- **Outpost**: Outpost-focused auto routine
- **Do Nothing**: Default - no movement

Autonomous paths are created using PathPlanner and stored in `src/main/deploy/pathplanner/`.

## Project Structure

- `src/main/java/frc/robot/` - Main robot code
  - `config/` - Configuration files (button bindings, subsystem setup, auto selector, etc.)
  - `constants/` - Robot constants (drive, vision, field, fuel, etc.)
  - `subsystems/` - Robot subsystems (drive, vision, fuel)
  - `commands/` - Robot commands
- `src/main/deploy/pathplanner/` - PathPlanner autonomous paths and autos
- `vendordeps/` - Third-party library dependencies

## Dependencies

This project uses the following libraries:
- **WPILib** - FRC control system library
- **REVLib** - REV Robotics motor controllers and sensors
- **PhotonLib** - PhotonVision computer vision
- **AdvantageKit** - Data logging and replay
- **PathPlanner** - Autonomous path planning and following

## Getting Started

### Prerequisites
- WPILib 2025 or later
- Java 17 or later
- Git

### Setup
1. Clone this repository
2. Open in VS Code with WPILib extension installed
3. Run `./gradlew build` to verify everything compiles

### Viewing Logs
Logs are recorded using AdvantageKit and can be replayed in AdvantageScope:
1. Download logs from the robot (stored in `/home/lvuser/logs/`)
2. Open AdvantageScope (located in `advantagescope/` directory)
3. Load the log file to review match data

## Building and Deploying

Use the standard WPILib commands to build and deploy:
- `./gradlew build` - Build the project
- `./gradlew deploy` - Deploy to the robot

## License

See [WPILib-License.md](WPILib-License.md) for license information.
