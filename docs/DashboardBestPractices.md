# Dashboard Best Practices

A guide to organizing and using Shuffleboard and AdvantageScope effectively.

---

## Table of Contents

1. [Dashboard Philosophy](#dashboard-philosophy)
2. [Tab Organization by Audience](#tab-organization-by-audience)
3. [When to Use Each Tool](#when-to-use-each-tool)
4. [Design Principles](#design-principles)
5. [Data Flow Architecture](#data-flow-architecture)
6. [Tab Reference](#tab-reference)

---

## Dashboard Philosophy

```
THE THREE TOOLS - EACH HAS A PURPOSE
═══════════════════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────────────────┐
│                           SHUFFLEBOARD                                      │
│                      (Live Match Display)                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  PURPOSE:  Show critical info DURING matches                                │
│  WHEN:     Competition, testing, pit checks                                 │
│  STYLE:    Large widgets, green/red indicators, minimal clutter            │
│                                                                             │
│  GOOD FOR:                                                                  │
│  ├── Auto selector                                                          │
│  ├── Battery voltage                                                        │
│  ├── Vision status (working/broken)                                         │
│  ├── System health checks                                                   │
│  └── Quick toggle switches                                                  │
│                                                                             │
│  BAD FOR:                                                                   │
│  ├── Detailed graphs (too small to read)                                    │
│  ├── Lots of numbers (overwhelming)                                         │
│  └── Historical data (can't scroll back)                                    │
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
│  ├── 3D robot visualization                                                 │
│  └── Sharing logs with mentors/other teams                                  │
│                                                                             │
│  BAD FOR:                                                                   │
│  ├── Real-time driver feedback (use Shuffleboard)                           │
│  └── Quick pit checks (takes time to load)                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          SMARTDASHBOARD                                     │
│                        (Data Bus Only)                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  PURPOSE:  Transfer data between subsystems and dashboard                   │
│  WHEN:     Behind the scenes, not for direct viewing                        │
│  STYLE:    SmartDashboard.putX() / SmartDashboard.getX()                   │
│                                                                             │
│  GOOD FOR:                                                                  │
│  ├── Subsystems publishing data                                             │
│  ├── Toggle switches reading user input                                     │
│  └── Sharing data between disconnected components                           │
│                                                                             │
│  BAD FOR:                                                                   │
│  ├── Direct viewing (use Shuffleboard tabs)                                 │
│  └── Logging (use Logger.recordOutput)                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Tab Organization by Audience

Different people need different information. Organize tabs by WHO will use them.

### Match Tab (Drive Team)

**Audience:** Drivers and coach during competition matches

**Design Rules:**
- Maximum 6 large widgets
- Must be readable from 10 feet away
- Green = good, Red = bad (no ambiguity)
- Only show what MUST be known during a match

**Widgets:**
| Widget | Purpose |
|--------|---------|
| Auto Selector | Verify correct auto before match |
| Battery | Know if power is getting low |
| Vision Status | Know if auto-aim will work |
| Speed Mode | Know current speed setting |
| Field Relative | Know drive orientation mode |
| Tag Count | Quick vision feedback |

### Pit Tab (Pit Crew)

**Audience:** Pit crew doing pre-match checks

**Design Rules:**
- All system health at a glance
- Quick buttons for common operations
- Status indicators for each major system

**Widgets:**
| Widget | Purpose |
|--------|---------|
| System Status List | All health checks in one place |
| Camera Connection | Verify cameras working |
| Battery Voltage | Check charge level |
| Coast/Brake Buttons | Set mode for transport |
| Position Display | Verify pose estimation |

### Debug Tab (Software Team)

**Audience:** Programmers during testing and development

**Design Rules:**
- All the detailed data
- PID tuning widgets
- Test commands
- Not meant for competition

**Widgets:**
| Widget | Purpose |
|--------|---------|
| Full Pose Data | X, Y, heading, rates |
| PID Values | Tune controllers |
| Vision Details | Ambiguity, failures |
| Test Commands | Trigger specific behaviors |
| System Info | CPU temp, brownout |

### Vision Cal Tab (Vision Team)

**Audience:** Anyone calibrating or testing vision

**Design Rules:**
- Everything needed to verify vision works
- **Field View to visualize where robot thinks it is**
- **Sanity checks to catch bad camera transforms**
- Toggle switches for modes
- Per-camera detailed metrics

**Widgets:**
| Widget | Purpose |
|--------|---------|
| **Field View** | Shows robot position on field - THE key diagnostic tool |
| **POSE SANE?** | Red if pose is outside field or spinning wildly |
| **Fused Pose** | X, Y, Heading from vision+odometry fusion |
| **Sanity Checks** | In-bounds and spin rate checks |
| Vision Enable Toggle | Turn vision on/off |
| Classroom Mode Toggle | Relaxed thresholds for testing |
| Per-Camera Status | Connection, targets, latency, ambiguity |
| Best Target Info | Which tag is most reliable |

---

## When to Use Each Tool

### Use Shuffleboard When...

```
✅ Showing status during a match
✅ Driver needs to verify something before match
✅ Pit crew doing quick health check
✅ Testing with toggle switches or buttons
✅ Information needs to be seen at a glance
```

### Use AdvantageScope When...

```
✅ Figuring out why autonomous failed
✅ Comparing vision pose to odometry pose
✅ Tuning PID by looking at step response
✅ Sharing match logs with mentors
✅ Doing detailed post-match analysis
✅ Visualizing robot in 3D on field
```

### Use Logger.recordOutput() When...

```
✅ You want data in AdvantageScope
✅ Logging poses (Pose2d, Pose3d)
✅ Logging module states
✅ Any detailed telemetry for later analysis
✅ Data you don't need to see live but want to review
```

### Use SmartDashboard When...

```
✅ Subsystem needs to publish data for dashboard to read
✅ Toggle switch on dashboard needs to control subsystem
✅ Data needs to flow between disconnected components
❌ NOT for direct viewing (use Shuffleboard)
❌ NOT for logging (use Logger.recordOutput)
```

---

## Design Principles

### 1. Less is More (Match Tab)

```
BAD:                                    GOOD:
┌─────────────────────────────────┐    ┌─────────────────────────────────┐
│ X: 2.34  Y: 5.67  H: 45.2°     │    │                                 │
│ VX: 1.2  VY: 0.3  ω: 0.5      │    │   ┌───────────────────────┐     │
│ FL: 2.1  FR: 2.0  RL: 2.1     │    │   │     AUTO MODE         │     │
│ RR: 2.0  Gyro: 45.3  Rate: 0  │    │   │  [Score 3 Notes]      │     │
│ Bat: 12.4  CPU: 45  Mem: 67%  │    │   └───────────────────────┘     │
│ Tag1: 2.3m  Tag2: 4.1m  ...   │    │                                 │
│ Amb: 0.03  Lat: 32ms  ...     │    │   BATTERY    VISION            │
│ ...too much to read...         │    │   [12.4V]    [GREEN]           │
└─────────────────────────────────┘    └─────────────────────────────────┘

Driver can't find anything!            Driver sees what matters instantly!
```

### 2. Use Boolean Boxes for Status

```
HARD TO READ:                          EASY TO READ:

  Vision Status: "HEALTHY"              Vision: [████████]  (GREEN)
  Vision Status: "UNHEALTHY - 5 fail"   Vision: [████████]  (RED)

Text requires reading and parsing.     Color is instant recognition.
```

### 3. Group Related Information

```
SCATTERED:                             GROUPED:

  ┌──────┐ ┌──────┐ ┌──────┐         ┌─────────────────────────────┐
  │ X    │ │ Bat  │ │ Y    │         │      POSITION               │
  └──────┘ └──────┘ └──────┘         ├─────────────────────────────┤
  ┌──────┐ ┌──────┐ ┌──────┐         │ X: 2.34  Y: 5.67  H: 45°   │
  │ Gyro │ │ Head │ │ Vis  │         └─────────────────────────────┘
  └──────┘ └──────┘ └──────┘

  Eyes have to jump around.            Information is organized logically.
```

### 4. Log Everything, Display Selectively

```
LOGGING (Logger.recordOutput):          DISPLAY (Shuffleboard):
├── Every module velocity               ├── Vision healthy? (yes/no)
├── Every module angle                  ├── Battery voltage
├── Gyro rate                          ├── Auto mode selected
├── All vision detections              └── Speed multiplier
├── All rejected measurements
├── Command states
├── PID errors
└── Timestamps

Everything goes to log for             Only critical info on dashboard
later analysis in AdvantageScope       for real-time viewing
```

---

## Data Flow Architecture

```
DATA FLOW IN YOUR ROBOT CODE
═══════════════════════════════════════════════════════════════════════════════

                         ┌─────────────────┐
                         │   SUBSYSTEMS    │
                         │ (VisionSubsystem│
                         │  DriveSubsystem)│
                         └────────┬────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
              ▼                   ▼                   ▼
    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
    │ SmartDashboard  │ │ Logger.record   │ │ Direct to       │
    │ .putX()         │ │ Output()        │ │ Shuffleboard    │
    │                 │ │                 │ │ via lambda      │
    │ Data Bus        │ │ AdvantageKit    │ │                 │
    └────────┬────────┘ └────────┬────────┘ └────────┬────────┘
             │                   │                   │
             ▼                   ▼                   │
    ┌─────────────────┐ ┌─────────────────┐          │
    │ DashboardSetup  │ │ Log File        │          │
    │ reads via       │ │ (.wpilog)       │          │
    │ SmartDashboard  │ │                 │          │
    │ .getX()         │ │                 │          │
    └────────┬────────┘ └────────┬────────┘          │
             │                   │                   │
             ▼                   ▼                   ▼
    ┌─────────────────────────────────────────────────────────┐
    │                      SHUFFLEBOARD                        │
    │  (Match, Pit, Debug, Vision Cal tabs)                   │
    └─────────────────────────────────────────────────────────┘
                                  │
                                  │ (Log files loaded later)
                                  ▼
    ┌─────────────────────────────────────────────────────────┐
    │                     ADVANTAGESCOPE                       │
    │  (Post-match analysis, 3D visualization, graphs)        │
    └─────────────────────────────────────────────────────────┘
```

### Recommended Pattern

1. **Subsystems** publish to SmartDashboard for toggle switches and status
2. **Subsystems** use Logger.recordOutput for detailed telemetry
3. **DashboardSetup** reads SmartDashboard OR uses direct lambdas to subsystems
4. **AdvantageScope** replays log files for analysis

---

## Tab Reference

### Match Tab Layout

```
┌───────────────────────────────────────────────────────────────────────────┐
│  MATCH TAB                                                    Team XXXX  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────────────┐  ┌───────────┐  ┌───────────────────────┐  │
│  │                         │  │           │  │                       │  │
│  │      AUTO MODE          │  │  BATTERY  │  │       VISION          │  │
│  │   [Score 3 Notes]       │  │   12.4V   │  │      [GREEN]          │  │
│  │                         │  │    ⚡     │  │                       │  │
│  └─────────────────────────┘  └───────────┘  └───────────────────────┘  │
│                                                                           │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────────────────┐  │
│  │   SPEED   │  │ FIELD REL │  │   TAGS    │  │       HEADING         │  │
│  │   FULL    │  │  [GREEN]  │  │  2 (MULTI)│  │         45°           │  │
│  └───────────┘  └───────────┘  └───────────┘  └───────────────────────┘  │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### Pit Tab Layout

```
┌───────────────────────────────────────────────────────────────────────────┐
│  PIT TAB                                                      Team XXXX  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────────┐  ┌───────────────────┐  ┌─────────────────────┐ │
│  │   SYSTEM STATUS     │  │   PIT COMMANDS    │  │     POSITION        │ │
│  ├─────────────────────┤  ├───────────────────┤  ├─────────────────────┤ │
│  │ Battery:     12.4V  │  │ [Coast Mode    ]  │  │ X:      2.34 m      │ │
│  │ Vision:    [GREEN]  │  │ [Brake Mode    ]  │  │ Y:      5.67 m      │ │
│  │ Front Cam: [GREEN]  │  │ [Zero Heading  ]  │  │ Heading: 45.0°      │ │
│  │ Back Cam:  [GREEN]  │  │ [Wheels Straight] │  └─────────────────────┘ │
│  │ Status:    HEALTHY  │  └───────────────────┘                          │
│  └─────────────────────┘                        [Force Vision Reset    ] │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### Debug Tab Layout

```
┌───────────────────────────────────────────────────────────────────────────┐
│  DEBUG TAB                                                    Team XXXX  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────┐ │
│  │     POSE        │ │ HEADING LOCK PID│ │     VISION      │ │ SYSTEM  │ │
│  ├─────────────────┤ ├─────────────────┤ ├─────────────────┤ ├─────────┤ │
│  │ X:       2.34   │ │ kP:     5.0     │ │ Healthy: [YES]  │ │ Bat:12.4│ │
│  │ Y:       5.67   │ │ kI:     0.0     │ │ Failures: 0     │ │ CPU: 45 │ │
│  │ Head:    45.0   │ │ kD:     0.0     │ │ Targets: [YES]  │ │ Brown:NO│ │
│  │ Rate:    0.0    │ │ Error:  2.3°    │ │ Ambig:   0.03   │ └─────────┘ │
│  │ Speed:   1.0    │ └─────────────────┘ └─────────────────┘             │
│  │ FldRel: [YES]   │                                                     │
│  └─────────────────┘                                                     │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                        TEST COMMANDS                                 │ │
│  │ [X-Stance] [Full Speed] [Half Speed] [Quarter] [Toggle FR] [Zero H] │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### Vision Cal Tab Layout

```
┌───────────────────────────────────────────────────────────────────────────┐
│  VISION CAL TAB                                               Team XXXX  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  [Vision Enabled] [Classroom Mode]  HEALTHY  Status    POSE SANE?        │
│       ON              OFF           [GREEN]  HEALTHY    [GREEN]          │
│                                                                           │
│  ┌─────────────────────────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │                                 │  │ FUSED POSE  │  │ ODOM ONLY   │   │
│  │         FIELD VIEW              │  ├─────────────┤  ├─────────────┤   │
│  │                                 │  │ X:  2.34 m  │  │ Compare:    │   │
│  │    ┌───┐                        │  │ Y:  5.67 m  │  │  Dist: 0.1m │   │
│  │    │ ▲ │  ← Robot icon shows    │  │ H:  45.0°   │  │ Gyro Rate:  │   │
│  │    └───┘    where robot         │  └─────────────┘  │  12.3 °/s   │   │
│  │             thinks it is!       │                   │ Gyro OK:    │   │
│  │                                 │  ┌─────────────────│  [YES]      │   │
│  │    If robot spinning on wrong   │  │ SANITY CHECKS  └─────────────┘   │
│  │    side = BAD CAMERA TRANSFORM! │  ├─────────────────────────────────┤ │
│  │                                 │  │ In Field Bounds:    [YES]       │ │
│  └─────────────────────────────────┘  │ Not Spinning Wild:  [YES]       │ │
│                                       └─────────────────────────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐               │
│  │  FRONT CAMERA   │  │   BACK CAMERA   │  │ BEST TARGET │   [Reset]    │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────┤               │
│  │ Connected [YES] │  │ Connected [YES] │  │ Tag ID:  5  │  PhotonVision│
│  │ Has Tgts  [YES] │  │ Has Tgts  [NO ] │  │ Ambig: 0.03 │   :5800      │
│  │ Count:    2     │  │ Count:    0     │  │ Cam:  front │               │
│  │ Tags:    1, 5   │  │ Tags:   None    │  │ Fails:   0  │               │
│  │ Latency: 32 ms  │  │ Latency: -- ms  │  └─────────────┘               │
│  │ Ambig:   0.03   │  │ Ambig:   --     │                                │
│  └─────────────────┘  └─────────────────┘                                │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### Vision Cal Tab - Key Diagnostic Features

| Feature | What It Shows | When It's RED |
|---------|---------------|---------------|
| **Field View** | Robot position on field map | Robot on wrong side or spinning wildly |
| **POSE SANE?** | Overall sanity check | Pose outside field OR spinning > 720°/s |
| **In Field Bounds** | Is pose within field (16.5m x 8m) | Bad camera transform |
| **Not Spinning Wild** | Gyro rate < 720°/s | Vision causing pose jumps |
| **Fused Pose** | Combined vision + odometry | Compare to actual robot position |

### How to Debug with Vision Cal Tab

```
STEP 1: Place robot at known position (against field wall)
        └── Robot should be stationary

STEP 2: Look at Field View
        └── Does robot icon match where robot actually is?

STEP 3: Check POSE SANE? indicator
        └── Should be GREEN

STEP 4: If pose is WRONG:
        ├── Camera transforms are likely incorrect
        ├── Re-measure X, Y, Z in VisionConstants.java
        └── Check camera pitch angle

STEP 5: If robot is SPINNING in Field View:
        ├── Camera yaw is probably 180° off
        ├── For back camera: kBackCameraYawRadians = Math.PI
        └── For front camera: kFrontCameraYawRadians = 0.0

STEP 6: If pose JUMPS when seeing tags:
        ├── Check camera calibration quality
        ├── Increase vision StdDevs (trust vision less)
        └── Lower kMaxAmbiguity threshold
```

---

## Summary

| Tool | Use For | Don't Use For |
|------|---------|---------------|
| **Shuffleboard** | Live match display | Detailed logging |
| **AdvantageScope** | Post-match analysis | Real-time feedback |
| **SmartDashboard** | Data bus between components | Direct viewing |
| **Logger.recordOutput** | All detailed telemetry | Live display |

| Tab | Audience | Widget Count | Style |
|-----|----------|--------------|-------|
| Match | Drivers | 6 max | Large, simple |
| Pit | Pit Crew | ~12 | Health checks |
| Debug | Programmers | Many | Detailed |
| Vision Cal | Vision Team | ~20 | Field view + per-camera metrics |

---

*Document created for Team 564 - Dashboard organization guide*
