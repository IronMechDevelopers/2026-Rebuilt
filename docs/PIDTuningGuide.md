# PID Tuning Guide for Swerve Drive

This guide explains all PID controllers in your robot codebase, what they do, and the recommended order for tuning them.

---

## Table of Contents
1. [What is PID?](#what-is-pid)
2. [PID Controllers Overview](#pid-controllers-overview)
3. [Tuning Order](#tuning-order)
4. [Detailed Tuning Instructions](#detailed-tuning-instructions)
5. [Common Symptoms & Fixes](#common-symptoms--fixes)
6. [Dashboard Tuning Workflow](#dashboard-tuning-workflow)

---

## What is PID?

PID stands for **Proportional-Integral-Derivative**. It's a control algorithm that takes an error (difference between where you are and where you want to be) and outputs a motor power to correct it.

| Term | What it Does | When to Increase |
|------|--------------|------------------|
| **P** (Proportional) | Reacts to current error. Bigger error = bigger correction | Response is too slow or sluggish |
| **I** (Integral) | Accumulates past errors. Fixes steady-state error | System gets close but never quite reaches target |
| **D** (Derivative) | Predicts future error. Dampens oscillation | System overshoots and oscillates |

**For FRC robots:** Start with just P. Most FRC PIDs work fine with P alone. Only add I or D if you have specific problems.

---

## PID Controllers Overview

Your robot has **6 PID controllers** organized in two layers:

### Layer 1: Motor PIDs (Low-Level)
These run **on the SparkMax motor controllers** at 1kHz. They control individual motor behavior.

| PID | File | Values | Controls |
|-----|------|--------|----------|
| **Driving Motor PID** | `HardwareConstants.java:104-111` | P=0.04, I=0, D=0 | Wheel speed (velocity control) |
| **Turning Motor PID** | `HardwareConstants.java:113-120` | P=1.0, I=0, D=0 | Module angle (position control) |

### Layer 2: Robot PIDs (High-Level)
These run **on the roboRIO** at 50Hz. They tell the robot where to go and the motor PIDs execute it.

| PID | File | Values | Controls |
|-----|------|--------|----------|
| **PathPlanner Translation** | `DriveConstants.java:152` | P=5.0, I=0, D=0 | Following paths (X/Y position) |
| **PathPlanner Rotation** | `DriveConstants.java:159` | P=5.0, I=0, D=0 | Following paths (heading) |
| **Auto-Aim** | `DriveConstants.java:173-180` | P=5.0, I=0, D=0 | Rotating to face targets |
| **Heading Lock** | `DriveConstants.java:185-192` | P=5.0, I=0, D=0 | Holding heading while driving |

---

## Tuning Order

**CRITICAL:** Always tune from the bottom up! Low-level PIDs must work before high-level PIDs can work.

```
┌─────────────────────────────────────────────────────────┐
│                    TUNING ORDER                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  STEP 1: Motor PIDs (Must be first!)                   │
│  ├── 1a. Turning Motor PID                             │
│  └── 1b. Driving Motor PID                             │
│                                                         │
│  STEP 2: Teleop PIDs (Test with driver input)          │
│  ├── 2a. Heading Lock PID                              │
│  └── 2b. Auto-Aim PID                                  │
│                                                         │
│  STEP 3: Autonomous PIDs (After teleop works)          │
│  ├── 3a. PathPlanner Translation PID                   │
│  └── 3b. PathPlanner Rotation PID                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Detailed Tuning Instructions

### Step 1a: Turning Motor PID

**Purpose:** Controls how fast and accurately each swerve module rotates to the commanded angle.

**Location:** `HardwareConstants.java` lines 113-120
```java
public static final double kTurningP = 1.0;
public static final double kTurningI = 0.0;
public static final double kTurningD = 0.0;
```

**How to Tune:**

1. **Lift the robot** so wheels are off the ground
2. **Open Debug tab** in Shuffleboard
3. **Command a 90° turn** (use the drive joystick)
4. Watch how the module rotates:

| Observation | Action |
|-------------|--------|
| Module turns slowly, takes > 0.3s to reach angle | Increase P (try 1.5, 2.0) |
| Module oscillates back and forth around target | Decrease P (try 0.75, 0.5) |
| Module snaps to angle quickly and stops | Perfect! Move on |

**Typical Values:** 0.5 to 2.0

**Signs of Good Tuning:**
- Module rotates to commanded angle in < 0.2 seconds
- No visible oscillation or vibration
- All 4 modules respond identically

---

### Step 1b: Driving Motor PID

**Purpose:** Controls wheel speed. Ensures wheels spin at the commanded velocity.

**Location:** `HardwareConstants.java` lines 104-111
```java
public static final double kDrivingP = 0.04;
public static final double kDrivingI = 0.0;
public static final double kDrivingD = 0.0;
```

**How to Tune:**

1. **Lift the robot** so wheels are off the ground
2. **Command full forward speed** (push joystick forward)
3. Watch/listen to wheels:

| Observation | Action |
|-------------|--------|
| Wheels spin up slowly (> 0.5s to full speed) | Increase P (try 0.06, 0.08) |
| Wheels oscillate in speed (pulsing/surging) | Decrease P (try 0.03, 0.02) |
| Wheels spin up quickly and maintain speed | Perfect! |

**Note:** The `velocityFF` (feedforward) in `Configs.java` does most of the work. P just corrects for errors.

**Typical Values:** 0.02 to 0.1

---

### Step 2a: Heading Lock PID

**Purpose:** Maintains robot heading while driving. Prevents drift when pushing joystick forward.

**Location:** `DriveConstants.java` lines 185-192
```java
public static final double kHeadingLockP = 5.0;
public static final double kHeadingLockI = 0.0;
public static final double kHeadingLockD = 0.0;
```

**Dashboard:** The Debug tab shows `Heading Lock PID` with live kP/kI/kD values and error.

**How to Tune:**

1. **Robot on the floor**, plenty of space
2. **Open Debug tab** - watch the "Heading Lock PID" section
3. **Zero the heading** (button on Debug tab or controller)
4. **Drive forward** without touching rotation stick
5. Push the robot sideways while driving - see how it corrects:

| Observation | Dashboard Shows | Action |
|-------------|-----------------|--------|
| Robot drifts, doesn't correct | Error stays high | Increase P (try 7.0, 10.0) |
| Robot oscillates/wobbles | Error flips +/- rapidly | Decrease P (try 3.0, 2.0) |
| Robot holds heading firmly | Error stays near 0 | Perfect! |

**Live Tuning via SmartDashboard:**
You can adjust these values at runtime by putting to SmartDashboard:
- `HeadingLock/kP`
- `HeadingLock/kI`
- `HeadingLock/kD`

**Typical Values:** 3.0 to 10.0

---

### Step 2b: Auto-Aim PID

**Purpose:** Rotates robot to face a target (e.g., speaker, amp, source).

**Location:** `DriveConstants.java` lines 173-180
```java
public static final double kAutoAimP = 5.0;
public static final double kAutoAimI = 0.0;
public static final double kAutoAimD = 0.0;
```

**How to Tune:**

1. **Position robot** facing ~45° away from target
2. **Trigger auto-aim** command
3. Watch the rotation:

| Observation | Action |
|-------------|--------|
| Robot turns slowly, takes > 1s to face target | Increase P |
| Robot overshoots target, oscillates | Decrease P |
| Robot snaps to target and holds | Perfect! |

**Typical Values:** 3.0 to 8.0

**Tolerance:** `kAutoAimToleranceDegrees = 2.0` defines "on target"

---

### Step 3a: PathPlanner Translation PID

**Purpose:** Controls how accurately the robot follows autonomous path X/Y positions.

**Location:** `DriveConstants.java` line 152
```java
public static final PIDConstants kTranslationPID = new PIDConstants(5.0, 0.0, 0.0);
```

**How to Tune:**

1. **Create a simple straight-line auto path** in PathPlanner
2. **Run the auto** multiple times
3. Watch the robot's path:

| Observation | Action |
|-------------|--------|
| Robot lags behind path, curves to catch up | Increase P |
| Robot oscillates side-to-side along path | Decrease P |
| Robot follows path smoothly | Perfect! |

**Typical Values:** 3.0 to 8.0

**Note:** This works closely with `kPathfindMaxVelMetersPerSecond` and `kPathfindMaxAccelMetersPerSecondSquared`. Lower max velocity may allow higher P.

---

### Step 3b: PathPlanner Rotation PID

**Purpose:** Controls how accurately the robot maintains heading during autonomous paths.

**Location:** `DriveConstants.java` line 159
```java
public static final PIDConstants kRotationPID = new PIDConstants(5.0, 0.0, 0.0);
```

**How to Tune:**

1. **Create a path with heading changes** in PathPlanner
2. **Run the auto** multiple times
3. Watch the robot's rotation:

| Observation | Action |
|-------------|--------|
| Robot rotates slowly, lags behind commanded heading | Increase P |
| Robot oscillates in heading, jerky rotation | Decrease P |
| Robot rotates smoothly with path | Perfect! |

**Typical Values:** 3.0 to 8.0

---

## Common Symptoms & Fixes

### Modules

| Symptom | Likely Cause | Fix |
|---------|--------------|-----|
| Modules "jitter" at rest | Turning P too high | Decrease `kTurningP` |
| Modules turn slowly | Turning P too low | Increase `kTurningP` |
| Wheels surge in speed | Driving P too high | Decrease `kDrivingP` |
| Robot doesn't reach full speed | Driving FF wrong | Check `kDriveWheelFreeSpeedRps` calculation |

### Teleop Driving

| Symptom | Likely Cause | Fix |
|---------|--------------|-----|
| Robot drifts when driving straight | Heading lock not engaged or P too low | Increase `kHeadingLockP` |
| Robot wobbles when driving | Heading lock P too high | Decrease `kHeadingLockP` |
| Auto-aim overshoots target | Auto-aim P too high | Decrease `kAutoAimP` |
| Auto-aim too slow | Auto-aim P too low | Increase `kAutoAimP` |

### Autonomous

| Symptom | Likely Cause | Fix |
|---------|--------------|-----|
| Robot cuts corners on paths | Translation P too low | Increase `kTranslationPID` P |
| Robot oscillates along path | Translation P too high | Decrease `kTranslationPID` P |
| Robot heading lags during turns | Rotation P too low | Increase `kRotationPID` P |
| Robot heading oscillates | Rotation P too high | Decrease `kRotationPID` P |
| Robot overshoots end of path | Max velocity/accel too high | Lower `kPathfindMaxVel/Accel` |

---

## Dashboard Tuning Workflow

### Using the Debug Tab

The Debug tab in Shuffleboard provides live PID tuning capability:

```
┌─────────────────────────────────────────────────────────────┐
│  DEBUG TAB LAYOUT                                           │
├──────────────────┬──────────────────┬──────────────────────┤
│  POSE DATA       │  HEADING LOCK    │  VISION SUMMARY      │
│  X, Y, Heading   │  kP, kI, kD      │  Health status       │
│  Gyro Rate       │  Error (°)       │  Failures, targets   │
│  Speed Mult      │                  │                      │
├──────────────────┼──────────────────┼──────────────────────┤
│  TEST COMMANDS   │                  │  SYSTEM INFO         │
│  Speed buttons   │                  │  Battery, CPU temp   │
│  X-Stance, etc   │                  │                      │
└──────────────────┴──────────────────┴──────────────────────┘
```

### Workflow for Heading Lock Tuning

1. **Deploy robot code** and open Shuffleboard
2. **Navigate to Debug tab**
3. In a terminal or via outline viewer, set:
   - `SmartDashboard/HeadingLock/kP` = your test value
4. **Watch "Error (°)"** while driving:
   - Should stay near 0 when driving straight
   - Should return to 0 quickly after disturbance
5. **Iterate** until behavior is good
6. **Update constants file** with final values

### Adding Runtime Tuning (Advanced)

To enable real-time PID adjustment from Shuffleboard:

1. **Publish defaults at startup** (in RobotContainer or DashboardSetup):
```java
SmartDashboard.putNumber("HeadingLock/kP", DriveConstants.kHeadingLockP);
SmartDashboard.putNumber("HeadingLock/kI", DriveConstants.kHeadingLockI);
SmartDashboard.putNumber("HeadingLock/kD", DriveConstants.kHeadingLockD);
```

2. **Read values in command** (in your heading lock command):
```java
double kP = SmartDashboard.getNumber("HeadingLock/kP", DriveConstants.kHeadingLockP);
double kI = SmartDashboard.getNumber("HeadingLock/kI", DriveConstants.kHeadingLockI);
double kD = SmartDashboard.getNumber("HeadingLock/kD", DriveConstants.kHeadingLockD);
pidController.setPID(kP, kI, kD);
```

3. **Edit values in Shuffleboard** and see changes immediately

---

## Quick Reference Card

Print this for pit crew:

```
╔══════════════════════════════════════════════════════════════╗
║                    PID QUICK REFERENCE                       ║
╠══════════════════════════════════════════════════════════════╣
║  TURNING MOTOR (kTurningP)                                   ║
║  Current: 1.0 | Range: 0.5-2.0                              ║
║  Symptom: Jittery → Lower P | Slow → Higher P               ║
╠══════════════════════════════════════════════════════════════╣
║  DRIVING MOTOR (kDrivingP)                                   ║
║  Current: 0.04 | Range: 0.02-0.1                            ║
║  Symptom: Surging → Lower P | Slow accel → Higher P         ║
╠══════════════════════════════════════════════════════════════╣
║  HEADING LOCK (kHeadingLockP)                                ║
║  Current: 5.0 | Range: 3.0-10.0                             ║
║  Symptom: Wobbling → Lower P | Drifting → Higher P          ║
╠══════════════════════════════════════════════════════════════╣
║  AUTO-AIM (kAutoAimP)                                        ║
║  Current: 5.0 | Range: 3.0-8.0                              ║
║  Symptom: Overshoots → Lower P | Slow → Higher P            ║
╠══════════════════════════════════════════════════════════════╣
║  PATHPLANNER TRANSLATION (kTranslationPID)                   ║
║  Current: 5.0 | Range: 3.0-8.0                              ║
║  Symptom: Oscillates → Lower P | Cuts corners → Higher P    ║
╠══════════════════════════════════════════════════════════════╣
║  PATHPLANNER ROTATION (kRotationPID)                         ║
║  Current: 5.0 | Range: 3.0-8.0                              ║
║  Symptom: Jerky turns → Lower P | Slow turns → Higher P     ║
╚══════════════════════════════════════════════════════════════╝
```

---

## File Locations Summary

| What to Change | File | Line |
|----------------|------|------|
| Turning Motor P/I/D | `constants/HardwareConstants.java` | 113-120 |
| Driving Motor P/I/D | `constants/HardwareConstants.java` | 104-111 |
| Heading Lock P/I/D | `constants/DriveConstants.java` | 185-192 |
| Auto-Aim P/I/D | `constants/DriveConstants.java` | 173-180 |
| PathPlanner Translation | `constants/DriveConstants.java` | 152 |
| PathPlanner Rotation | `constants/DriveConstants.java` | 159 |
| Motor Config Application | `Configs.java` | 29, 45 |
