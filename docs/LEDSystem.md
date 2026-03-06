# LED System Guide

A hardware-agnostic LED system for communicating match state to the drive team.

---

## Table of Contents

1. [Overview](#overview)
2. [Default Behavior (2026 REBUILT)](#default-behavior-2026-rebuilt)
3. [Hardware Setup](#hardware-setup)
4. [Using Interrupts](#using-interrupts)
5. [Customization](#customization)

---

## Overview

The LED system provides visual feedback to the drive team based on match state. It's designed to be:

- **Hardware-agnostic**: Swap between REV Blink, CANdle, or direct RoboRIO control
- **Match-aware**: Automatically changes based on game phase and hub status
- **Interruptible**: Temporary overrides for events like "shooter ready"

### Architecture

```
┌─────────────────┐
│  LEDSubsystem   │  ← Manages state, handles interrupts
└────────┬────────┘
         │ uses
         ▼
┌─────────────────┐
│ MatchStateTracker│  ← Provides phase/hub info
└─────────────────┘
         │
         ▼
┌─────────────────┐
│ LEDController   │  ← Hardware interface
│   (interface)   │
└────────┬────────┘
         │
    ┌────┴────┬─────────────┐
    ▼         ▼             ▼
┌────────┐ ┌────────┐ ┌──────────┐
│  REV   │ │  CTR   │ │Addressable│
│ Blink  │ │ CANdle │ │   LED    │
└────────┘ └────────┘ └──────────┘
```

---

## Default Behavior (2026 REBUILT)

The LEDs automatically display match state without any driver input:

| Match Phase | Time | LED Pattern | Description |
|-------------|------|-------------|-------------|
| **Pre-Match** | Before start | Chasing Alliance Color | Blue/Gold if alliance unknown |
| **Autonomous** | 2:30 → 2:10 | Solid Alliance Color | Red or Blue |
| **Transition** | 2:10 → 2:00 | Solid Purple | Waiting for FMS data |
| | | Blink Active Color | Blinks color of who starts ACTIVE |
| **Active Shift** | Start | Solid Alliance Color | Your hub is active |
| | Mid-shift | Blink Alliance Color | Warning: shift ending soon |
| | Final 3s | Blink White | Urgent: shift about to end |
| **Inactive Shift** | Start | Solid Yellow | Your hub is inactive |
| | Mid-shift | Blink Yellow | Warning: shift ending soon |
| | Final 3s | Blink White | Urgent: shift about to end |
| **Endgame** | 35s left | Blink Purple | Endgame approaching |
| | 16s left | Blink White | CLIMB NOW! |
| **Post-Match** | 0:00+ | Rainbow | Celebration mode |

### Warning Timeline

```
SHIFT TIMELINE (25 seconds)
=====================================================================══════════

Start                        Mid-warning              Final warning    End
  │                              │                          │           │
  ▼                              ▼                          ▼           ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ SOLID (Alliance/Yellow)  │  BLINK (Alliance/Yellow)  │  BLINK WHITE  │ NEXT
├──────────────────────────┼────────────────────────────┼───────────────┤
│       16 seconds         │        6 seconds          │   3 seconds   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Hardware Setup

### Option 1: Direct RoboRIO (AddressableLED)

**Pros**: Simple, no extra hardware cost
**Cons**: Uses CPU cycles for animations, limited to one strip

```java
// In RobotContainer.java
LEDController ledController = new AddressableLEDController(
    0,      // PWM port
    60      // Number of LEDs
);

LEDSubsystem leds = new LEDSubsystem(ledController, matchStateTracker);
```

**Wiring:**
- Data → PWM port 0
- Power → 5V (external supply for >30 LEDs)
- Ground → Common ground

### Option 2: REV Blink

**Pros**: Simple, robust
**Cons**: Only preset patterns, no custom colors

```java
LEDController ledController = new REVBlinkController(0); // PWM port
LEDSubsystem leds = new LEDSubsystem(ledController, matchStateTracker);
```

### Option 3: CTR CANdle

**Pros**: CAN communication, built-in animations, multiple strips
**Cons**: Requires Phoenix library, more expensive

```java
// Uncomment CANdle code in CANdleController.java first!
LEDController ledController = new CANdleController(
    0,      // CAN ID
    68      // Total LEDs (8 onboard + 60 strip)
);

LEDSubsystem leds = new LEDSubsystem(ledController, matchStateTracker);
```

### Option 4: Simulation/Testing

```java
LEDController ledController = new NoOpLEDController(true); // true = verbose logging
LEDSubsystem leds = new LEDSubsystem(ledController, matchStateTracker);
```

---

## Using Interrupts

Interrupts temporarily override the default state for a short duration:

```java
// Flash green when shooter is ready
leds.flashShooterReady();

// Flash when game piece acquired
leds.flashGamePieceAcquired();

// Custom interrupt (state, duration in seconds)
leds.setInterrupt(LEDState.SHOOTER_READY, 0.5);

// Force a state until manually cleared
leds.forceState(LEDState.ERROR);
leds.resumeAutomatic(); // Return to match-based state
```

### Built-in Interrupt States

| Method | State | Duration | Use Case |
|--------|-------|----------|----------|
| `flashShooterReady()` | Fast green blink | 0.5s | Shooter at speed |
| `flashGamePieceAcquired()` | Purple/cyan blink | 0.75s | Intake has piece |
| `flashError()` | Hot pink strobe | 1.0s | Error condition |

---

## Customization

### Adding Custom Colors

In `LEDColor.java`:

```java
// Team colors
public static final LEDColor TEAM_PRIMARY = new LEDColor(0, 100, 255, "Team Blue");
public static final LEDColor TEAM_SECONDARY = new LEDColor(255, 215, 0, "Team Gold");
```

### Adding Custom States

In `LEDState.java`:

```java
// Custom state for your mechanism
public static final LEDState INTAKE_RUNNING = new LEDState(
    LEDPattern.PULSE, LEDColor.ORANGE, "Intake Running"
);
```

### Adding Custom Patterns

1. Add pattern to `LEDPattern.java` enum
2. Implement rendering in `AddressableLEDController.update()`
3. Map to Blink pattern in `REVBlinkController.mapStateToPattern()` (if using Blink)

### Adjusting Timing

In `LEDSubsystem.java`:

```java
private static final double MID_SHIFT_WARNING_TIME = 9.0;   // Seconds before shift end
private static final double FINAL_SHIFT_WARNING_TIME = 3.0; // Final warning
private static final double ENDGAME_PURPLE_TIME = 35.0;     // Endgame warning start
private static final double ENDGAME_WHITE_TIME = 16.0;      // Urgent climb warning
```

---

## NetworkTables Keys

The LED subsystem publishes status for dashboards:

| Key | Type | Description |
|-----|------|-------------|
| `LED/State` | String | Current state description |
| `LED/Pattern` | String | Current pattern type |
| `LED/Color` | String | Primary color name |
| `LED/HasInterrupt` | Boolean | True if interrupt is active |
| `LED/Connected` | Boolean | Hardware connection status |

---

## Troubleshooting

### LEDs not working

1. Check PWM/CAN wiring
2. Verify port numbers in code
3. Check power supply (LEDs draw significant current)
4. Try `NoOpLEDController` with verbose=true to verify logic

### Colors wrong

1. Check LED strip type (RGB vs GRB)
2. Verify color values in `LEDColor.java`
3. Some strips have different color ordering

### Animations stuttering

1. Reduce number of LEDs
2. Increase animation period constants
3. Consider using CANdle (offloads animation to hardware)

---

## Files Reference

| File | Purpose |
|------|---------|
| `LEDSubsystem.java` | Main subsystem, match state logic |
| `LEDController.java` | Hardware interface |
| `LEDState.java` | State definitions (pattern + color) |
| `LEDPattern.java` | Pattern types enum |
| `LEDColor.java` | Color definitions |
| `AddressableLEDController.java` | Direct RoboRIO implementation |
| `REVBlinkController.java` | REV Blink implementation |
| `CANdleController.java` | CTR CANdle implementation |
| `NoOpLEDController.java` | Simulation/testing |

---

*Document created for Team 5684 - LED System Guide for 2026 REBUILT*
