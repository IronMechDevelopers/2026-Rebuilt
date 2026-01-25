# Dashboard Best Practices

A guide to organizing dashboards using Elastic and AdvantageScope.

---

## Table of Contents

1. [Dashboard Philosophy](#dashboard-philosophy)
2. [NetworkTables Key Reference](#networktables-key-reference)
3. [Elastic Setup Guide](#elastic-setup-guide)
4. [AdvantageScope Usage](#advantagescope-usage)
5. [2026 REBUILT Match Dashboard](#2026-rebuilt-match-dashboard)

---

## Dashboard Philosophy

```
THE TWO TOOLS - EACH HAS A PURPOSE
═══════════════════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────────────────┐
│                              ELASTIC                                         │
│                        (Live Match Display)                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  PURPOSE:  Show critical info DURING matches                                │
│  WHEN:     Competition, testing, pit checks                                 │
│  STYLE:    Large widgets, green/red indicators, minimal clutter            │
│                                                                             │
│  GOOD FOR:                                                                  │
│  ├── Auto selector                                                          │
│  ├── Hub status (ACTIVE/INACTIVE) for 2026 REBUILT                         │
│  ├── Warning banners (CLEAR ZONE, GET READY, CLIMB NOW)                    │
│  ├── Battery voltage                                                        │
│  ├── Vision status (working/broken)                                         │
│  └── Field visualization                                                    │
│                                                                             │
│  BAD FOR:                                                                   │
│  ├── Historical data (can't scroll back)                                    │
│  └── Detailed graphs (use AdvantageScope)                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          ADVANTAGESCOPE                                     │
│                     (Post-Match Analysis)                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  PURPOSE:  Analyze what happened AFTER matches                              │
│  WHEN:     Between matches, at home, debugging                              │
│  STYLE:    Graphs, 3D visualization, log replay                             │
│                                                                             │
│  GOOD FOR:                                                                  │
│  ├── Comparing odometry vs vision poses                                     │
│  ├── Finding when/why something failed                                      │
│  ├── Tuning PID by looking at response curves                               │
│  ├── 3D robot visualization with AprilTags                                  │
│  ├── Sharing logs with mentors/other teams                                  │
│  └── Replaying exact driver inputs                                          │
│                                                                             │
│  BAD FOR:                                                                   │
│  ├── Real-time driver feedback (use Elastic)                                │
│  └── Quick pit checks (takes time to load)                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Architecture

```
DATA FLOW IN YOUR ROBOT CODE
═══════════════════════════════════════════════════════════════════════════════

                         ┌─────────────────┐
                         │   SUBSYSTEMS    │
                         │ (VisionSubsystem│
                         │  DriveSubsystem │
                         │MatchStateTracker│
                         └────────┬────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
              ▼                   ▼                   ▼
    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
    │ SmartDashboard  │ │ Logger.record   │ │ DashboardSetup  │
    │ .putX()         │ │ Output()        │ │ .periodic()     │
    │                 │ │                 │ │                 │
    │ NetworkTables   │ │ AdvantageKit    │ │ Aggregates data │
    └────────┬────────┘ └────────┬────────┘ └────────┬────────┘
             │                   │                   │
             │                   │                   │
             ▼                   ▼                   ▼
    ┌─────────────────────────────────────────────────────────┐
    │                     NETWORKTABLES                        │
    │  (Match/*, Drive/*, Vision/*, Hub/*, System/*)          │
    └────────────────────────┬────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼                             ▼
    ┌─────────────────┐           ┌─────────────────┐
    │     ELASTIC     │           │  ADVANTAGESCOPE │
    │  (Live display) │           │  (Log replay)   │
    └─────────────────┘           └─────────────────┘
```

---

## NetworkTables Key Reference

All data is published to NetworkTables via SmartDashboard. Here are the keys organized by namespace:

### Match/* (2026 REBUILT Hub Status)

| Key | Type | Description |
|-----|------|-------------|
| `Match/HubActive` | Boolean | TRUE if our hub is currently active (can score) |
| `Match/HubStatus` | String | "ACTIVE", "INACTIVE", or "UNKNOWN" |
| `Match/NextStatus` | String | "→ ACTIVE", "→ INACTIVE", or "→ ???" |
| `Match/ShiftCountdown` | Number | Seconds until next shift change |
| `Match/TimeInPhase` | Number | Seconds elapsed in current phase |
| `Match/Phase` | String | "Auto", "Transition", "Shift 1-4", "Endgame" |
| `Match/Warning` | String | Warning message or empty string |
| `Match/HasWarning` | Boolean | TRUE if a warning is active |
| `Match/WarningType` | String | "CLEAR", "READY", "CLIMB", or "NONE" |
| `Match/FmsDataReceived` | Boolean | TRUE once FMS game data arrives |
| `Match/WeAreFirst` | String | "WE'RE INACTIVE FIRST" or "THEY'RE INACTIVE FIRST" |
| `Match/PracticeMode` | Boolean | TRUE when practice mode is active |

### Practice/* (Shift Timing Training)

| Key | Type | Description |
|-----|------|-------------|
| `Practice/Enabled` | Boolean | Toggle to start/stop practice mode (writable) |
| `Practice/WeAreInactiveFirst` | Boolean | Configure which scenario to practice (writable) |
| `Practice/Restart` | Boolean | Tap TRUE to restart from Shift 1 (writable) |
| `Practice/CycleNumber` | Number | How many times through all 4 shifts |
| `Practice/TotalElapsed` | Number | Total seconds since practice started |

### LED/*

| Key | Type | Description |
|-----|------|-------------|
| `LED/State` | String | Current LED state description |
| `LED/Pattern` | String | Current pattern type (SOLID, BLINK, etc.) |
| `LED/Color` | String | Primary color name |
| `LED/HasInterrupt` | Boolean | TRUE if temporary interrupt is active |
| `LED/Connected` | Boolean | Hardware connection status |

### Drive/*

| Key | Type | Description |
|-----|------|-------------|
| `Drive/SpeedMode` | String | "FULL", "75%", "HALF", or "SLOW" |
| `Drive/SpeedMultiplier` | Number | 0.0 to 1.0 |
| `Drive/FieldRelative` | Boolean | TRUE if field-relative driving |
| `Drive/Heading` | Number | 0-360 degrees |
| `Drive/PoseX` | Number | X position in meters |
| `Drive/PoseY` | Number | Y position in meters |
| `Drive/PoseRotation` | Number | Rotation in degrees |
| `Drive/TagCount` | Number | Total AprilTags visible |
| `Drive/HasMultiTag` | Boolean | TRUE if 2+ tags visible |
| `Drive/PoseInBounds` | Boolean | TRUE if pose is within field |
| `Drive/PoseSane` | Boolean | TRUE if pose passes sanity checks |

### Hub/*

| Key | Type | Description |
|-----|------|-------------|
| `Hub/DistanceInches` | Number | Distance to alliance hub in inches |
| `Hub/InRange` | Boolean | TRUE if in optimal shooting range |

### Vision/*

| Key | Type | Description |
|-----|------|-------------|
| `Vision/Healthy` | Boolean | TRUE if vision system is working |
| `Vision/HealthStatus` | String | Detailed status message |
| `Vision/Enabled` | Boolean | Kill switch (write to disable) |
| `Vision/AnyTargetsVisible` | Boolean | TRUE if any camera sees tags |
| `Vision/{Camera}/Connected` | Boolean | Per-camera connection status |
| `Vision/{Camera}/HasTargets` | Boolean | Per-camera target detection |
| `Vision/{Camera}/TargetCount` | Number | Tags seen by this camera |

### System/*

| Key | Type | Description |
|-----|------|-------------|
| `System/BatteryVoltage` | Number | Battery voltage |
| `System/BatteryStatus` | String | "GOOD", "OK", or "LOW" |
| `System/CPUTemp` | Number | RoboRIO CPU temperature |
| `System/Brownout` | Boolean | TRUE if brownout detected |

---

## Elastic Setup Guide

### Installing Elastic

1. Download Elastic from: https://github.com/Gold872/elastic-dashboard
2. Install and run Elastic
3. Connect to your robot (or simulation) via NetworkTables

### Creating Your Match Layout

In Elastic, create widgets that read the NetworkTables keys above:

**Recommended Match Layout:**

```
┌───────────────────────────────────────────────────────────────────────────┐
│  MATCH DASHBOARD - 2026 REBUILT                               Team 5684  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────────┐  ┌──────────────┐  ┌──────────────────────────┐ │
│  │                     │  │   SHIFT IN   │  │       PHASE              │ │
│  │     OUR HUB         │  │              │  │      Shift 2             │ │
│  │    [ACTIVE]         │  │     18       │  ├──────────────────────────┤ │
│  │                     │  │              │  │     FMS DATA             │ │
│  │  Match/HubActive    │  │ Match/Shift  │  │      [GREEN]             │ │
│  │                     │  │  Countdown   │  │                          │ │
│  └─────────────────────┘  └──────────────┘  └──────────────────────────┘ │
│                                                                           │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │                                                                       ││
│  │   ⚠️  CLEAR ZONE!                              Match/Warning          ││
│  │                                                                       ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                           │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌─────────┐│
│  │   AUTO     │ │  VISION    │ │  HUB DIST  │ │  IN RANGE  │ │  BATT   ││
│  │ [Chooser]  │ │  [GREEN]   │ │    72"     │ │  [GREEN]   │ │  12.4V  ││
│  │            │ │            │ │            │ │            │ │         ││
│  │Auto Selector│Vision/Healthy│Hub/Distance │ Hub/InRange │ System/  ││
│  │            │ │            │ │  Inches    │ │            │ Battery  ││
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘ └─────────┘│
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### Widget Configuration Tips

1. **Boolean indicators**: Use color coding
   - `Match/HubActive`: Green when TRUE, Red when FALSE
   - `Vision/Healthy`: Green when TRUE, Red when FALSE
   - `Match/HasWarning`: Orange when TRUE, Dark when FALSE

2. **Warning banner**: Make it LARGE and center it
   - Read from `Match/Warning`
   - Use `Match/WarningType` to set colors:
     - "CLEAR" = Orange
     - "READY" = Yellow
     - "CLIMB" = Red
     - "NONE" = Hidden/Dark

3. **Countdown**: Use a large number display
   - Read from `Match/ShiftCountdown`
   - Consider a radial gauge (0-30 range)

---

## AdvantageScope Usage

### Viewing Logs

1. Run your robot (real or simulation)
2. Log files are saved to USB drive on RoboRIO
3. Open log file in AdvantageScope

### Recommended AdvantageScope Views

**Pose Visualization:**
- Add `Drive/Pose` as a "Robot" object on the 2D field
- Add `Vision/Summary/TagPoses` as "Vision Target" objects
- Add `Vision/Summary/RobotPosesAccepted` as green "Ghost" objects
- Add `Vision/Summary/RobotPosesRejected` as red "Ghost" objects

**Match Analysis:**
- Graph `Match/ShiftCountdown` to see timing
- Graph `Hub/DistanceInches` to analyze positioning
- Overlay with `Match/HubActive` to correlate scoring windows

**Vision Debugging:**
- Compare `Drive/PoseX/Y` with vision estimates
- Look for jumps when `Vision/*/RobotPosesRejected` is populated
- Check `Vision/*/Ambiguity` values at problem moments

---

## 2026 REBUILT Match Dashboard

### Game Context

The REBUILT game has alternating hub activation:
- **AUTO (20s)**: Both hubs active
- **TRANSITION (10s)**: Both hubs active, FMS data arrives
- **SHIFTS 1-4 (25s each)**: Alternating hub activation
- **ENDGAME (30s)**: Both hubs active, focus on climbing

### Warning System

| Warning | NetworkTables Key Value | Driver Action |
|---------|------------------------|---------------|
| `Match/Warning = "CLEAR ZONE!"` | Hub closing in 9s | Flush fuel, exit zone |
| `Match/Warning = "GET READY!"` | Hub opening in 9s | Position to score |
| `Match/Warning = "ENDGAME SOON"` | 30s remaining | Prepare climb |
| `Match/Warning = "CLIMB NOW!"` | 15s remaining | Execute climb |
| `Match/Warning = "GO GO GO!"` | 5s remaining | Emergency climb |

### FMS Data

The game-specific message ('R' or 'B') indicates which alliance's hub is inactive during Shifts 1 & 3:
- Arrives ~3 seconds after Auto ends
- `Match/FmsDataReceived` turns TRUE when received
- `Match/WeAreFirst` shows strategic positioning info

---

## Practice Mode (Shift Timing Training)

Practice mode lets your team train on the 25-second shift timing without running full matches.

### Starting Practice Mode

1. Open Elastic dashboard
2. Set `Practice/WeAreInactiveFirst` to configure the scenario:
   - **TRUE** = We're inactive in Shifts 1 & 3 (practice defense positioning)
   - **FALSE** = We're active in Shifts 1 & 3 (practice scoring windows)
3. Toggle `Practice/Enabled` to **TRUE**
4. Practice starts immediately - no need to enable the robot!

### What Happens

```
PRACTICE MODE CYCLE (100 seconds, loops forever)
═══════════════════════════════════════════════════════════════════════════════

    Shift 1 (25s)     Shift 2 (25s)     Shift 3 (25s)     Shift 4 (25s)
  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
  │   INACTIVE    │ │    ACTIVE     │ │   INACTIVE    │ │    ACTIVE     │
  │  (if first)   │ │               │ │  (if first)   │ │               │
  └───────────────┘ └───────────────┘ └───────────────┘ └───────────────┘
        │                 │                 │                 │
        └─────────────────┴─────────────────┴─────────────────┘
                                    │
                              LOOPS BACK TO
                                SHIFT 1
```

### LEDs and Warnings Work Normally

- LEDs show the same patterns as a real match
- Warnings fire at the same timing (9s before shift, 3s before shift)
- Dashboard shows `[PRACTICE]` prefix on phase name

### Restarting Practice

- Set `Practice/Restart` to TRUE to jump back to Shift 1
- Useful for repeating a specific transition

### Stopping Practice

- Toggle `Practice/Enabled` to FALSE
- Returns to normal pre-match state

---

## Testing at Home (Simulation)

You can test the dashboard without hardware:

```bash
./gradlew simulateJava
```

Then:
1. Open Elastic and connect to `localhost`
2. Use the WPILib Simulation GUI to control match state
3. Use Practice Mode to test shift timing (toggle `Practice/Enabled`)
4. Or call `matchStateTracker.setFmsDataForTesting('R')` to simulate FMS data

---

## Key Files

| File | Purpose |
|------|---------|
| `DashboardSetup.java` | Publishes all data to NetworkTables |
| `MatchStateTracker.java` | Calculates hub status, phases, warnings, practice mode |
| `MatchConstants.java` | Timing values for match phases |
| `DriveSubsystem.java` | Publishes drive and hub distance data |
| `VisionSubsystem.java` | Publishes vision health and target data |
| `LEDSubsystem.java` | LED control with match state integration |
| `LEDSystem.md` | Complete LED system documentation |

---

## Related Documentation

- **[LEDSystem.md](LEDSystem.md)** - Complete LED system guide including hardware setup, interrupt system, and customization

---

*Document created for Team 5684 - Elastic + AdvantageScope dashboard guide for 2026 REBUILT*
