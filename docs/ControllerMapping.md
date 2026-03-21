# Controller Mapping - 2026 Season

This document describes all button mappings for the driver and co-driver Xbox controllers.

---

## Controller Ports

- **Port 0**: Driver Xbox Controller
- **Port 1**: Co-Driver Xbox Controller

---

## Driver Controller (Xbox)

The driver controls robot movement and primary scoring functions.

### Button Layout

| ID | Command | Button | Trigger Type | Description |
|----|---------|--------|--------------|-------------|
| 1 | **Gyro Reset** | Left Stick Click | Once | Re-zeroes the gyro to the current heading; essential for field-oriented control |
| 2 | **Precision Mode** | Y Button | Hold | Scales maximum drivetrain output to 50% for fine-tuned maneuvering |
| 3 | **Robot Relative** | B Button | Toggle | Disables field-centric control; robot drives relative to its own front face |
| 4 | **Intake** | Left Trigger | Hold | Activates intake rollers to pull fuel into the robot storage |
| 5 | **Eject** | Left Bumper | Hold | Reverses intake rollers to clear jams or discard fuel |
| 6 | **Shoot** | Right Trigger | Hold | Sequential command: Spools shooter for 1s then feeds/fires the fuel |
| 8 | **Diamond Snap** | Right Bumper | Hold | Snaps the robot heading to the nearest 45° offset |
| 9 | **X-Stance** | X Button | Toggle | Turns all four swerve modules inward to create a locked "X" for defense |
| 10 | **Hub Lock** | A Button | Toggle | Rotates to face the hub and locks wheels in X-Stance (Stationary) |
| 11 | **Sniper Mode** | POV Up | Toggle | Active targeting: Robot stays locked on the Hub while allowing translation |

### Visual Layout - Driver Controller

```
                    ╔═══════════════════════╗
                    ║   [Y] Precision 50%   ║
                    ║                       ║
        [LB] Eject  ║ [X]     [ ] [A] Hub   ║  [RB] Diamond Snap
                    ║  X-Stance   Lock      ║
        [LT] Intake ║                       ║  [RT] Shoot
                    ║   [B] Robot Relative  ║
                    ║                       ║
                    ║  (LS)      (RS)       ║
                    ║  Click:               ║
                    ║  Gyro                 ║
                    ║  Reset                ║
                    ║         ↑             ║
                    ║       Sniper          ║
                    ║                       ║
                    ╚═══════════════════════╝
```

---

## Co-Driver Controller (Xbox)

The co-driver handles fuel management and assists with precision aiming.

### Button Layout

| ID | Command | Button | Trigger Type | Description |
|----|---------|--------|--------------|-------------|
| 4 | **Intake** | X Button | Hold | Activates intake rollers to pull fuel into the robot storage |
| 5 | **Eject** | B Button | Hold | Reverses intake rollers to clear jams or discard fuel |
| 6 | **Shoot** | A Button | Hold | Sequential command: Spools shooter for 1s then feeds/fires the fuel |
| 7 | **Square Snap** | Y Button | Hold | Snaps the robot heading to the nearest 90° cardinal direction |

### Visual Layout - Co-Driver Controller

```
                    ╔═══════════════════════╗
                    ║   [Y] Square Snap     ║
                    ║                       ║
                    ║ [X]         [A]       ║
                    ║ Intake      Shoot     ║
                    ║                       ║
                    ║   [B] Eject           ║
                    ║                       ║
                    ║                       ║
                    ║  (Available buttons)  ║
                    ║  - Left/Right Bumpers ║
                    ║  - Left/Right Triggers║
                    ║  - D-Pad (POV)        ║
                    ║  - Back/Start         ║
                    ╚═══════════════════════╝
```

---

## Command Descriptions

### Drive Commands

#### **Gyro Reset** (Driver: Left Stick Click)
- **Purpose**: Resets the robot's heading to zero
- **When to use**: After the robot has been moved manually, or when field-oriented driving feels incorrect
- **Implementation**: `DriveCommands.zeroHeading()`

#### **Precision Mode** (Driver: Y Button - Hold)
- **Purpose**: Reduces maximum drive speed to 50% for fine control
- **When to use**: During precise positioning near scoring locations
- **Implementation**: `DriveCommands.setSpeed(0.5)` while held, returns to 1.0 when released

#### **Robot Relative** (Driver: B Button - Toggle)
- **Purpose**: Toggles field-centric driving on/off
- **When to use**: When you want the robot to drive relative to its own front (useful for certain maneuvers)
- **Implementation**: `DriveCommands.toggleFieldRelative()`

#### **X-Stance** (Driver: X Button - Toggle)
- **Purpose**: Locks all swerve modules in an X pattern for maximum resistance to pushing
- **When to use**: Defense, or when you need to hold position
- **Implementation**: `DriveCommands.xStance()`

#### **Diamond Snap** (Driver: Right Bumper - Hold)
- **Purpose**: Snaps robot rotation to nearest 45° angle (45°, 135°, 225°, 315°)
- **When to use**: Quick alignment to diagonal field positions
- **Implementation**: `DriveCommands.snapToDiamond()`

#### **Square Snap** (Co-Driver: Y Button - Hold)
- **Purpose**: Snaps robot rotation to nearest 90° cardinal direction (0°, 90°, 180°, 270°)
- **When to use**: Quick alignment to field walls or straight positions
- **Implementation**: `DriveCommands.snapToClosestCardinal()`

### Vision/Scoring Commands

#### **Hub Lock** (Driver: A Button - Toggle)
- **Purpose**: Automatically aims at the hub, then locks wheels in X-stance for stable scoring
- **When to use**: When positioned for scoring and ready to shoot
- **Behavior**:
  1. Rotates robot to face the alliance hub
  2. Locks wheels in X-stance
  3. Robot is stationary until toggled off
- **Implementation**: `DriveCommands.aimAtHub().andThen(DriveCommands.xStance())`

#### **Sniper Mode** (Driver: POV Up - Toggle)
- **Purpose**: Auto-aims at hub while allowing driver to strafe/translate
- **When to use**: When you need to shoot while moving or adjusting position
- **Behavior**:
  - Robot continuously rotates to face the hub
  - Driver retains full control of translation (strafing)
  - Like "turret mode" - rotate independently from movement
- **Implementation**: `DriveCommands.driveWhileAimingAtHub()`

### Fuel/Shooter Commands

#### **Intake** (Driver: Left Trigger / Co-Driver: X Button - Hold)
- **Purpose**: Runs intake rollers to collect fuel from the field
- **When to use**: When approaching fuel on the field
- **Implementation**: `ballSubsystem.intake()`

#### **Eject** (Driver: Left Bumper / Co-Driver: B Button - Hold)
- **Purpose**: Reverses intake rollers to eject fuel
- **When to use**: Clearing jams, discarding unwanted fuel
- **Implementation**: `ballSubsystem.eject()`

#### **Shoot** (Driver: Right Trigger / Co-Driver: A Button - Hold)
- **Purpose**: Spins up shooter, then launches fuel
- **Behavior**:
  1. Spins up shooter wheels for 1 second
  2. Feeds fuel into shooter
  3. Stops when button released
- **When to use**: After aligning with hub (Hub Lock or Sniper Mode)
- **Implementation**: `ballSubsystem.spinUpCommand().withTimeout(1s).andThen(ballSubsystem.launchCommand())`

---

## Usage Scenarios

### Scenario 1: Quick Score (Stationary)
1. Position robot near hub
2. Press **A** (Hub Lock) - Robot aims and locks
3. Hold **Right Trigger** (Shoot) - Score the fuel
4. Press **A** again to unlock

### Scenario 2: Score While Moving
1. Enable **POV Up** (Sniper Mode) - Robot tracks hub
2. Strafe with left stick to adjust position
3. Hold **Right Trigger** (Shoot) when ready
4. Press **POV Up** again to disable

### Scenario 3: Collect Fuel
1. Drive toward fuel
2. Hold **Left Trigger** (Intake)
3. Release when fuel collected

### Scenario 4: Defensive Position
1. Position robot
2. Press **X** (X-Stance) to lock wheels
3. Press **X** again to resume driving

### Scenario 5: Precision Positioning
1. Hold **Y** (Precision Mode) for slower movement
2. Position robot precisely
3. Release **Y** to return to full speed

---

## Available Buttons (Future Use)

### Driver
- Right Stick Click
- POV Down, Left, Right
- Back/Start buttons

### Co-Driver
- Left Bumper
- Right Bumper
- Left Trigger
- Right Trigger
- POV (D-pad): Up, Down, Left, Right
- Back/Start buttons

---

## Related Files

- **Implementation**: `src/main/java/frc/robot/config/ButtonBindings.java`
- **Controller Setup**: `src/main/java/frc/robot/RobotContainer.java`
- **Drive Commands**: `src/main/java/frc/robot/commands/DriveCommands.java`
- **Vision Commands**: `src/main/java/frc/robot/commands/VisionDriveCommands.java`
- **Fuel Subsystem**: `src/main/java/frc/robot/subsystems/CANFuelSubsystem.java`

---

## Tuning Constants

If commands need adjustment, check these files:

- **Auto-Aim PID**: `DriveConstants.kAutoAimP/I/D`
- **Snap Angles**: Built into snap commands (45° and 90° increments)
- **Shooter Spin-Up Time**: `FuelConstants.SPIN_UP_SECONDS`
- **Precision Mode Speed**: Currently hardcoded to 0.5 (50%) in ButtonBindings.java
- **Joystick Deadband**: `DriveConstants.kJoystickDeadband`

---

*Last Updated: 2026 Season*
