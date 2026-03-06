# Dashboard Guide

Four Elastic dashboard layouts for different roles and situations.

---

## Table of Contents

1. [Dashboard Overview](#dashboard-overview)
2. [Match Dashboard](#1-match-dashboard)
3. [Pit Mode Dashboard](#2-pit-mode-dashboard)
4. [Vision Dashboard](#3-vision-dashboard)
5. [Software Dashboard](#4-software-dashboard)
6. [Practice Mode](#practice-mode)
7. [AdvantageScope Usage](#advantagescope-usage)

---

## Dashboard Overview

```
FOUR DASHBOARDS - FOUR PURPOSES
=====================================================================══════════

┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│     MATCH       │  │    PIT MODE     │  │     VISION      │  │    SOFTWARE     │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│                 │  │                 │  │                 │  │                 │
│  Drive Team     │  │  Pit Crew       │  │  Vision Team    │  │  Programmers    │
│  During Match   │  │  Between Matches│  │  Camera Tuning  │  │  Debugging      │
│                 │  │                 │  │                 │  │                 │
│  Hub Status     │  │  System Checks  │  │  Tag Detection  │  │  PID Tuning     │
│  Shift Timing   │  │  Setup Commands │  │  Pose Estimates │  │  Module States  │
│  Warnings       │  │  Battery Check  │  │  Ambiguity      │  │  CAN Bus        │
│                 │  │                 │  │                 │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘
```

### Switching Dashboards in Elastic

Save each layout as a separate file:
- `match.json`
- `pit.json`
- `vision.json`
- `software.json`

Use **File → Open** to switch between them quickly.

---

## 1. Match Dashboard

**WHO:** Drive team during competition
**WHEN:** Matches and practice matches
**GOAL:** Show only what drivers need to make decisions

### Layout

```
┌───────────────────────────────────────────────────────────────────────────┐
│  MATCH DASHBOARD                                               Team 5684  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                                                                      │ │
│  │   ⚠️  WARNING MESSAGE HERE                    Match/Warning          │ │
│  │                                                                      │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌─────────────────────┐  ┌──────────────┐  ┌──────────────────────────┐ │
│  │                     │  │              │  │                          │ │
│  │     OUR HUB IS      │  │  SHIFT IN    │  │        PHASE             │ │
│  │                     │  │              │  │                          │ │
│  │     [ACTIVE]        │  │     18       │  │       Shift 2            │ │
│  │        or           │  │   seconds    │  │                          │ │
│  │    [INACTIVE]       │  │              │  │  Next: WE'RE INACTIVE    │ │
│  │                     │  │              │  │                          │ │
│  │  Match/HubActive    │  │Match/Shift   │  │  Match/Phase             │ │
│  │                     │  │  Countdown   │  │  Match/NextStatus        │ │
│  └─────────────────────┘  └──────────────┘  └──────────────────────────┘ │
│                                                                           │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐            │
│  │   AUTO     │ │  BATTERY   │ │  IN RANGE  │ │   VISION   │            │
│  │ [Chooser]  │ │   12.4V    │ │  [GREEN]   │ │  [GREEN]   │            │
│  │            │ │            │ │            │ │            │            │
│  │   Auto     │ │  System/   │ │Hub/InRange │ │  Vision/   │            │
│  │  Selector  │ │  Battery   │ │            │ │  Healthy   │            │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘            │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### NetworkTables Keys

| Key | Type | Widget | Description |
|-----|------|--------|-------------|
| `Match/HubActive` | Boolean | Large indicator | GREEN=score, RED=wait |
| `Match/HubStatus` | String | Text | "ACTIVE" / "INACTIVE" |
| `Match/ShiftCountdown` | Number | Large number | Seconds until shift change |
| `Match/Phase` | String | Text | Current match phase |
| `Match/Warning` | String | Large banner | Warning message (hide when empty) |
| `Match/HasWarning` | Boolean | - | Use to show/hide warning banner |
| `Match/NextStatus` | String | Text | What happens next |
| `Hub/InRange` | Boolean | Indicator | GREEN=can score |
| `Vision/Healthy` | Boolean | Indicator | GREEN=vision working |
| `System/BatteryVoltage` | Number | Gauge | Battery level |
| `SmartDashboard/Auto Selector` | SendableChooser | Dropdown | Auto routine picker |

### Warning Colors

| `Match/Warning` Value | Color | Meaning |
|----------------------|-------|---------|
| `"CLEAR ZONE!"` | Orange | Hub closing in 9s |
| `"GET READY!"` | Yellow | Hub opening in 9s |
| `"ENDGAME SOON"` | Purple | 30s remaining |
| `"CLIMB NOW!"` | Red | 15s remaining |
| `"GO GO GO!"` | Flashing Red | 5s remaining |

---

## 2. Pit Mode Dashboard

**WHO:** Pit crew
**WHEN:** Between matches, before queuing
**GOAL:** Verify robot is ready, run setup commands

### Layout

```
┌───────────────────────────────────────────────────────────────────────────┐
│  PIT MODE                                                      Team 5684  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  SYSTEM STATUS                                                            │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐             │
│  │  BATTERY   │ │   VISION   │ │  CAN BUS   │ │   GYRO     │             │
│  │   12.6V    │ │  [GREEN]   │ │    24%     │ │  [GREEN]   │             │
│  │  [GREEN]   │ │            │ │  [GREEN]   │ │            │             │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘             │
│                                                                           │
│  SETUP COMMANDS                                                           │
│  ┌───────────────────┐ ┌───────────────────┐ ┌───────────────────┐       │
│  │                   │ │                   │ │                   │       │
│  │  SET WHEELS       │ │   ZERO HEADING    │ │   COAST MODE      │       │
│  │  STRAIGHT         │ │                   │ │                   │       │
│  │                   │ │                   │ │                   │       │
│  │  [BUTTON]         │ │     [BUTTON]      │ │    [BUTTON]       │       │
│  │                   │ │                   │ │                   │       │
│  │Pit/SetWheels      │ │  Pit/ZeroHeading  │ │  Pit/CoastMode    │       │
│  │  Straight         │ │                   │ │                   │       │
│  └───────────────────┘ └───────────────────┘ └───────────────────┘       │
│                                                                           │
│  ┌───────────────────┐ ┌───────────────────┐                             │
│  │                   │ │                   │                             │
│  │   BRAKE MODE      │ │  VISION RESET     │                             │
│  │                   │ │                   │                             │
│  │    [BUTTON]       │ │     [BUTTON]      │                             │
│  │                   │ │                   │                             │
│  │  Pit/BrakeMode    │ │Pit/ForceVisionReset                            │
│  └───────────────────┘ └───────────────────┘                             │
│                                                                           │
│  ALLIANCE SELECTION                                                       │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  [ ] Red Alliance    [ ] Blue Alliance         Pit/Alliance        │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### NetworkTables Keys - Status

| Key | Type | Widget | Good Value |
|-----|------|--------|------------|
| `System/BatteryVoltage` | Number | Gauge | > 12.0V |
| `System/BatteryStatus` | String | Text | "GOOD" |
| `Vision/Healthy` | Boolean | Indicator | TRUE |
| `Vision/CameraCount` | Number | Text | Expected count |
| `CAN/PercentUtilization` | Number | Gauge | < 70% |
| `Drive/GyroConnected` | Boolean | Indicator | TRUE |

### NetworkTables Keys - Commands (Writable)

These keys trigger commands when set to TRUE. The robot code reads them and runs the command.

| Key | Type | Action |
|-----|------|--------|
| `Pit/SetWheelsStraight` | Boolean | Points all wheels forward |
| `Pit/ZeroHeading` | Boolean | Resets gyro heading to 0 |
| `Pit/CoastMode` | Boolean | Sets motors to coast (push robot) |
| `Pit/BrakeMode` | Boolean | Sets motors to brake (competition) |
| `Pit/ForceVisionReset` | Boolean | Resets pose to vision estimate |

### Pre-Match Checklist

Use these indicators to verify before queuing:

1. **Battery** - `System/BatteryVoltage` > 12.5V
2. **Vision** - `Vision/Healthy` = TRUE
3. **CAN Bus** - `CAN/PercentUtilization` < 50%
4. **Gyro** - `Drive/GyroConnected` = TRUE
5. **Wheels** - Run "Set Wheels Straight" and visually verify
6. **Motors** - Set to Brake Mode before match

---

## 3. Vision Dashboard

**WHO:** Vision team / programmers
**WHEN:** Tuning cameras, debugging pose estimation
**GOAL:** See what vision sees, tune parameters

### Layout

```
┌───────────────────────────────────────────────────────────────────────────┐
│  VISION TUNING                                                 Team 5684  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  CAMERA STATUS                                                            │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐          │
│  │  FRONT CAMERA    │ │  BACK CAMERA     │ │  SIDE CAMERA     │          │
│  │  Connected: ✓    │ │  Connected: ✓    │ │  Connected: ✗    │          │
│  │  Targets: 2      │ │  Targets: 1      │ │  Targets: 0      │          │
│  │  Latency: 22ms   │ │  Latency: 25ms   │ │  Latency: --     │          │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘          │
│                                                                           │
│  POSE COMPARISON                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                                                                     │  │
│  │   Odometry Pose:    X: 3.24   Y: 5.67   Rot: 45.2°                 │  │
│  │   Vision Pose:      X: 3.21   Y: 5.65   Rot: 45.0°                 │  │
│  │   Difference:       X: 0.03   Y: 0.02   Rot: 0.2°                  │  │
│  │                                                                     │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                           │
│  POSE QUALITY                                                             │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐            │
│  │  AMBIGUITY │ │  TAG COUNT │ │  ACCEPTED  │ │  REJECTED  │            │
│  │    0.12    │ │      3     │ │     142    │ │      7     │            │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘            │
│                                                                           │
│  TUNING PARAMETERS (Writable)                                             │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  Max Ambiguity:     [0.20]     Vision/MaxAmbiguity                 │  │
│  │  Max Speed Filter:  [2.0 m/s]  Vision/MaxSpeedForVision            │  │
│  │  Vision Enabled:    [  ✓  ]    Vision/Enabled                      │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### NetworkTables Keys - Per Camera

Replace `{Camera}` with camera name (e.g., `FrontCamera`, `BackCamera`):

| Key | Type | Description |
|-----|------|-------------|
| `Vision/{Camera}/Connected` | Boolean | Camera communication OK |
| `Vision/{Camera}/HasTargets` | Boolean | Sees AprilTags |
| `Vision/{Camera}/TargetCount` | Number | Number of tags visible |
| `Vision/{Camera}/Latency` | Number | Pipeline latency (ms) |
| `Vision/{Camera}/Ambiguity` | Number | Pose ambiguity (lower=better) |

### NetworkTables Keys - Summary

| Key | Type | Description |
|-----|------|-------------|
| `Vision/Healthy` | Boolean | Overall vision health |
| `Vision/AnyTargetsVisible` | Boolean | Any camera sees tags |
| `Vision/TotalTagCount` | Number | Total tags across all cameras |
| `Vision/PosesAccepted` | Number | Accepted pose updates (session) |
| `Vision/PosesRejected` | Number | Rejected pose updates (session) |

### NetworkTables Keys - Tuning (Writable)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `Vision/Enabled` | Boolean | TRUE | Kill switch for vision updates |
| `Vision/MaxAmbiguity` | Number | 0.2 | Reject poses above this |
| `Vision/MaxSpeedForVision` | Number | 2.0 | Disable vision when moving fast |

### NetworkTables Keys - Pose Comparison

| Key | Type | Description |
|-----|------|-------------|
| `Drive/PoseX` | Number | Fused pose X (meters) |
| `Drive/PoseY` | Number | Fused pose Y (meters) |
| `Drive/PoseRotation` | Number | Fused pose rotation (degrees) |
| `Vision/LatestPoseX` | Number | Raw vision X (meters) |
| `Vision/LatestPoseY` | Number | Raw vision Y (meters) |
| `Vision/LatestPoseRot` | Number | Raw vision rotation (degrees) |

---

## 4. Software Dashboard

**WHO:** Programmers
**WHEN:** Debugging, PID tuning, development
**GOAL:** Deep system visibility, tunable parameters

### Layout

```
┌───────────────────────────────────────────────────────────────────────────┐
│  SOFTWARE DEBUG                                                Team 5684  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  SWERVE MODULES                                                           │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────┐ │
│  │   FRONT LEFT   │ │  FRONT RIGHT   │ │   REAR LEFT    │ │ REAR RIGHT │ │
│  │  Vel: 2.3 m/s  │ │  Vel: 2.2 m/s  │ │  Vel: 2.4 m/s  │ │ Vel: 2.3   │ │
│  │  Ang: 45.2°    │ │  Ang: 44.8°    │ │  Ang: 45.5°    │ │ Ang: 45.1° │ │
│  │  Temp: 42°C    │ │  Temp: 43°C    │ │  Temp: 41°C    │ │ Temp: 42°C │ │
│  └────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘
│                                                                           │
│  CAN BUS HEALTH                                                           │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐             │
│  │    BUS     │ │   TX FULL  │ │  RX ERROR  │ │  TX ERROR  │             │
│  │    24%     │ │      0     │ │      0     │ │      0     │             │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘             │
│                                                                           │
│  SYSTEM                                                                   │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐             │
│  │  BATTERY   │ │  BROWNOUT  │ │  CPU TEMP  │ │  LOOP TIME │             │
│  │   12.4V    │ │  [GREEN]   │ │    45°C    │ │   18.2ms   │             │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘             │
│                                                                           │
│  PID TUNING (Writable)                                                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  Drive P: [0.04]   I: [0.0]   D: [0.0]   Tune/Drive/*              │  │
│  │  Turn P:  [1.0]    I: [0.0]   D: [0.0]   Tune/Turn/*               │  │
│  │  Rot P:   [3.0]    I: [0.0]   D: [0.0]   Tune/Rotation/*           │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                           │
│  PRACTICE MODE                                                            │
│  ┌───────────────────┐ ┌───────────────────┐ ┌───────────────────┐       │
│  │  ENABLED          │ │ WE'RE INACTIVE    │ │    RESTART        │       │
│  │    [  ✓  ]        │ │  FIRST [  ✓  ]    │ │    [BUTTON]       │       │
│  │Practice/Enabled   │ │Practice/WeAre...  │ │ Practice/Restart  │       │
│  └───────────────────┘ └───────────────────┘ └───────────────────┘       │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### NetworkTables Keys - Module Data

Replace `{Module}` with `FL`, `FR`, `RL`, `RR`:

| Key | Type | Description |
|-----|------|-------------|
| `Drive/{Module}/Velocity` | Number | Wheel velocity (m/s) |
| `Drive/{Module}/Angle` | Number | Wheel angle (degrees) |
| `Drive/{Module}/DriveTemp` | Number | Drive motor temp (°C) |
| `Drive/{Module}/TurnTemp` | Number | Turn motor temp (°C) |
| `Drive/{Module}/DriveCurrent` | Number | Drive motor current (A) |

### NetworkTables Keys - CAN Bus

| Key | Type | Healthy Value |
|-----|------|---------------|
| `CAN/PercentUtilization` | Number | < 70% |
| `CAN/BusOffCount` | Number | 0 |
| `CAN/TxFullCount` | Number | 0 |
| `CAN/ReceiveErrorCount` | Number | 0 |
| `CAN/TransmitErrorCount` | Number | 0 |

### NetworkTables Keys - System

| Key | Type | Description |
|-----|------|-------------|
| `System/BatteryVoltage` | Number | Battery voltage |
| `System/Brownout` | Boolean | TRUE = brownout detected |
| `System/CPUTemp` | Number | RoboRIO CPU temp |
| `System/LoopTime` | Number | Main loop time (ms) |
| `System/MemoryUsage` | Number | Memory usage (%) |

### NetworkTables Keys - PID Tuning (Writable)

| Key | Type | Description |
|-----|------|-------------|
| `Tune/Drive/P` | Number | Drive motor kP |
| `Tune/Drive/I` | Number | Drive motor kI |
| `Tune/Drive/D` | Number | Drive motor kD |
| `Tune/Turn/P` | Number | Turn motor kP |
| `Tune/Turn/I` | Number | Turn motor kI |
| `Tune/Turn/D` | Number | Turn motor kD |
| `Tune/Rotation/P` | Number | Rotation controller kP |
| `Tune/Rotation/I` | Number | Rotation controller kI |
| `Tune/Rotation/D` | Number | Rotation controller kD |

### NetworkTables Keys - Practice Mode

| Key | Type | Description |
|-----|------|-------------|
| `Practice/Enabled` | Boolean | Toggle practice mode |
| `Practice/WeAreInactiveFirst` | Boolean | Which scenario to practice |
| `Practice/Restart` | Boolean | Restart from Shift 1 |
| `Practice/CycleNumber` | Number | Current cycle count |
| `Practice/TotalElapsed` | Number | Total practice time |

---

## Practice Mode

Practice mode simulates match shift timing for driver training.

### Starting Practice

1. Go to Software Dashboard (or add these to any dashboard)
2. Set `Practice/WeAreInactiveFirst` for the scenario you want
3. Toggle `Practice/Enabled` to TRUE
4. Practice starts immediately - works even when disabled!

### Cycle

```
100-SECOND PRACTICE CYCLE (loops forever)
=====================================================================══════════

  Shift 1 (25s)     Shift 2 (25s)     Shift 3 (25s)     Shift 4 (25s)
 ───────────────   ───────────────   ───────────────   ───────────────
    INACTIVE   →      ACTIVE     →     INACTIVE    →      ACTIVE
                                                              │
                          ┌───────────────────────────────────┘
                          │
                          └─────────────→  LOOPS BACK TO SHIFT 1
```

LEDs and warnings work exactly like a real match.

---

## AdvantageScope Usage

### When to Use AdvantageScope

- **Post-match analysis** - Review what happened
- **Tuning** - Graph PID response curves
- **Debugging** - Find exactly when something failed
- **Vision** - Compare odometry vs vision poses in 3D

### Recommended Views

**Pose Visualization (3D Field):**
- Robot pose: `Drive/Pose`
- Vision targets: `Vision/Summary/TagPoses`
- Accepted estimates: `Vision/Summary/RobotPosesAccepted` (green)
- Rejected estimates: `Vision/Summary/RobotPosesRejected` (red)

**Match Timing:**
- Graph `Match/ShiftCountdown` to see timing
- Overlay with `Match/HubActive` to see scoring windows

**Driver Inputs (Replay):**
- `Inputs/DriverLeftX`, `Inputs/DriverLeftY`
- `Inputs/DriverRightX`
- Replay exactly what the driver did

---

## LED System

The LED system automatically shows match state. See [LEDSystem.md](LEDSystem.md) for:
- LED behavior by match phase
- Hardware setup options
- Interrupt system for custom states

---

## Key Files

| File | Purpose |
|------|---------|
| `DashboardSetup.java` | Publishes data to NetworkTables |
| `MatchStateTracker.java` | Hub status, phases, warnings, practice mode |
| `MatchConstants.java` | Timing values for match phases |
| `DriveSubsystem.java` | Drive and pose data |
| `UtilityCommands.java` | Pit setup commands |
| `LEDSubsystem.java` | LED control |

---

*Dashboard Guide for Team 5684 - 2026 REBUILT*
