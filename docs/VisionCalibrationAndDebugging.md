# Vision Calibration, Testing, and Debugging Guide

A practical guide for setting up and troubleshooting your FRC vision system.

---

## Table of Contents

1. [Hardware Setup Checklist](#hardware-setup-checklist)
2. [PhotonVision Installation](#photonvision-installation)
3. [Camera Calibration](#camera-calibration)
4. [Measuring Camera Transforms](#measuring-camera-transforms)
5. [Pipeline Configuration](#pipeline-configuration)
6. [Testing Procedure](#testing-procedure)
7. [Dashboard Interpretation](#dashboard-interpretation)
8. [Common Problems and Solutions](#common-problems-and-solutions)
9. [Competition Checklist](#competition-checklist)

---

## Hardware Setup Checklist

### Required Components

| Component | Specification | Notes |
|-----------|---------------|-------|
| Coprocessor | Raspberry Pi 5 | 4GB+ RAM recommended |
| Camera(s) | OV9281 (720p) | Global shutter, USB connection |
| USB Cable | USB 3.0 capable | Short cable (< 1m) reduces issues |
| Power | 5V 5A USB-C | Official Pi 5 power supply recommended |
| Cooling | Heatsink + Fan | Vision processing is CPU-intensive |
| MicroSD | 32GB+ Class 10 | Faster is better |

### Physical Setup

```
WIRING DIAGRAM
═══════════════════════════════════════════════════════════════════

                    ┌─────────────────────┐
    5V Power ──────►│   Raspberry Pi 5    │
                    │                     │
    Ethernet ◄─────►│  (PhotonVision)     │◄────── USB Camera (Front)
    (to robot       │                     │
     radio)         │                     │◄────── USB Camera (Back)
                    └─────────────────────┘


MOUNTING CONSIDERATIONS
═══════════════════════════════════════════════════════════════════

    ✅ DO:
    ├── Mount cameras rigidly (no vibration)
    ├── Protect lenses from impacts
    ├── Keep cables secured and strain-relieved
    ├── Allow airflow around Pi for cooling
    └── Use Loctite on mounting screws

    ❌ DON'T:
    ├── Mount cameras where they can be hit
    ├── Leave cables loose (can snag)
    ├── Block camera view with robot parts
    └── Position cameras where lens can fog
```

### OV9281 Camera Specifications

```
Resolution:     1280 x 720 (720p)
Sensor Type:    Global Shutter (no rolling shutter distortion!)
Frame Rate:     Up to 120 FPS
FOV:            ~70° diagonal (varies by lens)
Interface:      USB 2.0/3.0
Recommended:    60-90 FPS for FRC
```

---

## PhotonVision Installation

### Step 1: Download the Image

1. Go to: https://docs.photonvision.org/
2. Download the Raspberry Pi 5 image
3. Use Balena Etcher to flash to MicroSD card

### Step 2: First Boot

1. Insert SD card into Pi 5
2. Connect Ethernet to robot radio/switch
3. Power on the Pi
4. Wait 2-3 minutes for first boot

### Step 3: Access the Interface

Open a browser and navigate to:
```
http://photonvision.local:5800
```

If that doesn't work, try:
```
http://10.TE.AM.11:5800
```
(Replace TE.AM with your team number, e.g., 10.56.94.11 for team 5684)

### Step 4: Configure Network (Important!)

In PhotonVision UI → Settings → Networking:

```
┌─────────────────────────────────────────────────────────────────┐
│ NETWORK SETTINGS                                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  IP Assignment:     ○ DHCP  ● Static                           │
│                                                                 │
│  Static IP:         10.TE.AM.11                                │
│  Subnet Mask:       255.255.255.0                              │
│  Gateway:           10.TE.AM.1                                 │
│                                                                 │
│  Team Number:       [5684]                                     │
│                                                                 │
│  NetworkTables Server: roboRIO-5684-FRC.local                  │
│                        (or 10.TE.AM.2)                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Camera Calibration

**This is the most critical step!** Poor calibration = poor vision performance.

### What Calibration Does

Calibration tells PhotonVision about your specific camera's lens distortion. Every camera lens bends light slightly differently, which affects distance/angle calculations.

### Print a Calibration Target

1. In PhotonVision UI → Cameras → Calibration
2. Click "Download Target"
3. Print on **letter size paper** (8.5" x 11")
4. **DO NOT scale to fit** - print at 100%
5. Mount on flat, rigid surface (cardboard/clipboard)

### Calibration Procedure

```
STEP 1: ACCESS CALIBRATION
════════════════════════════════════════════════════════════════════
    PhotonVision UI → Select Camera → "Calibration" tab

    Settings:
    ├── Resolution: 1280x720 (match your pipeline!)
    ├── Board Type: Chessboard
    ├── Pattern Size: 8x8 (or whatever matches your print)
    ├── Square Size: [measure your squares in mm!]
    └── Marker Size: N/A for chessboard


STEP 2: CAPTURE IMAGES (15-20 minimum)
════════════════════════════════════════════════════════════════════

    Move the checkerboard to capture images from DIFFERENT:

    ┌─────────────────────────────────────────────────────────────┐
    │                                                             │
    │   ANGLES:         Near camera    Far from camera           │
    │                   ┌───┐          ┌───┐                     │
    │   ╱───╲          │▓▓▓│          │▓▓▓│                     │
    │  │▓▓▓▓│ tilted   │▓▓▓│          └───┘                     │
    │   ╲───╱          │▓▓▓│                                     │
    │                   └───┘                                     │
    │                                                             │
    │   POSITIONS:      Left side     Center     Right side      │
    │                   ┌───┐         ┌───┐         ┌───┐        │
    │                   │▓▓▓│         │▓▓▓│         │▓▓▓│        │
    │                   └───┘         └───┘         └───┘        │
    │                                                             │
    │   ROTATIONS:                                                │
    │                   ┌───┐    ╱───╲    ┌───────┐              │
    │                   │▓▓▓│   │▓▓▓▓│   │▓▓▓▓▓▓▓│              │
    │                   │▓▓▓│    ╲───╱    └───────┘              │
    │                   └───┘   angled    landscape              │
    │                                                             │
    └─────────────────────────────────────────────────────────────┘

    For each position:
    1. Hold board steady
    2. Click "Take Snapshot"
    3. Verify green checkmarks appear on corners
    4. Move to next position


STEP 3: CALCULATE CALIBRATION
════════════════════════════════════════════════════════════════════

    After 15-20 images, click "Calibrate"

    Wait for calculation (may take 30-60 seconds)

    CHECK THE RESULT:
    ┌─────────────────────────────────────────────────────────────┐
    │                                                             │
    │  Mean Reprojection Error: [0.32 px]                        │
    │                                                             │
    │  INTERPRET:                                                 │
    │    < 0.5 px  = EXCELLENT ✅ (this is your target!)         │
    │    0.5-1.0   = ACCEPTABLE ⚠️                               │
    │    > 1.0 px  = REDO CALIBRATION ❌                         │
    │                                                             │
    └─────────────────────────────────────────────────────────────┘

    If error is too high:
    ├── Delete bad images (ones with partial board detection)
    ├── Add more images from different angles
    ├── Ensure board is flat (no warping)
    └── Recalibrate


STEP 4: SAVE CALIBRATION
════════════════════════════════════════════════════════════════════

    Click "Save" to store calibration

    The calibration is stored per-resolution!
    If you change resolution, you need to recalibrate.
```

### Tips for Good Calibration

```
✅ DO:
├── Fill the entire frame with the board in some shots
├── Get corners and edges of the image
├── Include tilted angles (45°)
├── Hold the board VERY still when capturing
├── Ensure good lighting (no shadows on board)
└── Measure square size accurately (use calipers!)

❌ DON'T:
├── Use a warped or bent calibration board
├── Take all images from the same distance
├── Rush - quality matters more than quantity
├── Use motion blur images (wait for camera to stabilize)
└── Skip the edges of the frame
```

---

## Measuring Camera Transforms

Your robot code needs to know exactly where each camera is mounted. This is **critical** for accurate pose estimation.

### The Coordinate System

```
ROBOT COORDINATE SYSTEM (WPILib Standard)
═══════════════════════════════════════════════════════════════════

                        +X (Forward)
                           ↑
                           │
                           │
              +Y (Left) ←──┼──→ -Y (Right)
                           │
                           │
                           ↓
                        -X (Backward)

                    +Z = UP toward ceiling
                    -Z = DOWN toward floor

    Origin (0, 0, 0) = Center of robot at floor level
                       (intersection of wheel diagonals)
```

### How to Measure

```
STEP 1: FIND ROBOT CENTER
═══════════════════════════════════════════════════════════════════

    For swerve drive: intersection of diagonal lines between wheels

            FL ●─────────────────● FR
               ╲                 ╱
                ╲               ╱
                 ╲     ●       ╱    ← Robot center (0, 0)
                  ╲   ╱│╲    ╱
                   ╲ ╱ │ ╲  ╱
                    ╱  │  ╲╱
                   ╱   │   ╲
            RL ●───────────────● RR


STEP 2: MEASURE X (Forward/Backward)
═══════════════════════════════════════════════════════════════════

    From robot center to camera lens:

    ├── Positive (+) = Camera is in FRONT of center
    └── Negative (-) = Camera is BEHIND center

    Example: Front camera 0.3m forward → X = +0.3
    Example: Back camera 0.25m backward → X = -0.25


STEP 3: MEASURE Y (Left/Right)
═══════════════════════════════════════════════════════════════════

    From robot center to camera lens:

    ├── Positive (+) = Camera is to the LEFT
    └── Negative (-) = Camera is to the RIGHT
    └── Zero (0) = Camera is centered

    Example: Camera offset 0.1m to the left → Y = +0.1


STEP 4: MEASURE Z (Height)
═══════════════════════════════════════════════════════════════════

    From floor to camera lens:

    ├── Always positive (camera is above floor)

    Example: Camera mounted 0.4m high → Z = +0.4


STEP 5: MEASURE ROTATION
═══════════════════════════════════════════════════════════════════

    PITCH (tilt up/down):
    ├── 0 = Level with ground
    ├── Negative = Looking DOWN (most common for AprilTags)
    └── Positive = Looking UP

    Example: Camera tilted 15° down → Pitch = -15° = -0.26 radians

    YAW (pan left/right):
    ├── 0 = Facing forward
    ├── +90° = Facing left
    ├── -90° = Facing right
    └── 180° (π) = Facing backward

    Example: Back camera facing backward → Yaw = 180° = 3.14159 radians

    ROLL (tilted sideways):
    ├── Almost always 0 (unless camera mounted at angle)
```

### Example Measurements

```java
// VisionConstants.java - Example for a typical setup

// FRONT CAMERA - Mounted front-center, 0.3m forward, 0.4m high, tilted 15° down
public static final double kFrontCameraToRobotX = 0.30;          // 30cm forward
public static final double kFrontCameraToRobotY = 0.0;           // centered
public static final double kFrontCameraToRobotZ = 0.40;          // 40cm up
public static final double kFrontCameraPitchRadians = -0.26;     // 15° down
public static final double kFrontCameraYawRadians = 0.0;         // facing forward
public static final double kFrontCameraRollRadians = 0.0;

// BACK CAMERA - Mounted rear-center, 0.25m backward, 0.35m high, tilted 10° down
public static final double kBackCameraToRobotX = -0.25;          // 25cm backward
public static final double kBackCameraToRobotY = 0.0;            // centered
public static final double kBackCameraToRobotZ = 0.35;           // 35cm up
public static final double kBackCameraPitchRadians = -0.17;      // 10° down
public static final double kBackCameraYawRadians = Math.PI;      // facing backward (180°)
public static final double kBackCameraRollRadians = 0.0;
```

### Verifying Your Measurements

After entering values, test by:
1. Place robot at a known field position
2. Point camera at an AprilTag
3. Check if reported position matches reality
4. If off by a consistent amount, adjust your transforms

---

## Pipeline Configuration

### Access Pipeline Settings

PhotonVision UI → Select Camera → "Pipeline" tab

### Recommended Settings for OV9281 + AprilTags

```
┌─────────────────────────────────────────────────────────────────┐
│ PIPELINE SETTINGS                                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Pipeline Type:        AprilTag                                 │
│                                                                 │
│  ─── CAMERA SETTINGS ───                                        │
│  Resolution:           1280 x 720                               │
│  FPS:                  60 (or 90 if Pi can handle it)          │
│  Exposure:             6ms (adjust for lighting)                │
│  Gain:                 Auto or 75 (adjust for lighting)        │
│                                                                 │
│  ─── APRILTAG SETTINGS ───                                      │
│  Tag Family:           36h11 (FRC standard)                    │
│  Decimate:             2 (balance of speed/accuracy)           │
│  Blur:                 0 (OV9281 doesn't need it)              │
│  Threads:              4 (use all Pi 5 cores)                  │
│  Refine Edges:         ON                                       │
│  Pose Estimation:      Multi-Tag (if available)                │
│                                                                 │
│  ─── 3D SETTINGS ───                                            │
│  3D:                   ENABLED ← Important!                    │
│  Target Model:         AprilTag 6.5" (FRC size)               │
│                                                                 │
│  ─── OUTPUT ───                                                 │
│  Stream Resolution:    640x480 (saves bandwidth)               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Exposure Tuning

```
EXPOSURE GUIDELINES
═══════════════════════════════════════════════════════════════════

The OV9281 is global shutter, so motion blur isn't a huge concern.
But lower exposure still helps:

    Bright venue (outdoor/well-lit):   2-4 ms
    Normal venue (typical gym):         4-8 ms
    Dim venue:                          8-15 ms

HOW TO TUNE:
1. Point camera at AprilTag
2. Move robot/camera side to side at match speed
3. If tag detection drops out → exposure too high
4. If image is too dark → exposure too low

SIGNS OF CORRECT EXPOSURE:
├── Tags detected consistently during motion
├── Image not too dark or washed out
└── Detection rate stays above 90% while moving
```

### Camera Naming

**Critical:** Rename cameras to match your code!

1. PhotonVision UI → Cameras → Click camera name
2. Rename to exactly: `front` or `back`
3. Click Save
4. Verify it matches `VisionConstants.java`:
   ```java
   public static final String kFrontCameraName = "front";
   public static final String kBackCameraName = "back";
   ```

---

## Testing Procedure

### Test 1: Basic Connectivity

```
CHECKLIST:
□ Can access PhotonVision UI at http://photonvision.local:5800
□ Both cameras show in the camera list
□ Camera streams are visible in the UI
□ No error messages in PhotonVision logs
```

### Test 2: AprilTag Detection

```
CHECKLIST:
□ Point camera at AprilTag
□ Green rectangle appears around tag in PhotonVision UI
□ Tag ID displays correctly
□ 3D axes overlay shows on tag (if 3D enabled)
□ Detection persists when camera moves slightly
```

### Test 3: NetworkTables Communication

```
CHECKLIST:
□ Robot code is running (enabled or disabled)
□ Open OutlineViewer or Shuffleboard
□ Navigate to NetworkTables → photonvision
□ Verify camera entries exist (front, back)
□ Verify data is updating (timestamps changing)
```

### Test 4: Dashboard Verification

```
CHECKLIST:
□ Deploy robot code
□ Open Shuffleboard/SmartDashboard
□ Navigate to Vision tab
□ Verify:
    ├── Vision/front/Connected = true
    ├── Vision/back/Connected = true
    ├── Vision/Healthy = true (when seeing tags)
    └── Vision/AnyTargetsVisible updates correctly
```

### Test 5: Pose Estimation Accuracy

```
PROCEDURE:
════════════════════════════════════════════════════════════════════

1. Set up a TEST ENVIRONMENT:
   ├── Use AprilTagFields.k2026RebuiltWelded layout OR
   └── Create custom test tags with known positions

2. Place robot at KNOWN position:
   ├── Measure position on field with tape measure
   ├── Example: X=2.0m, Y=3.0m, Heading=0°

3. Enable robot (or use disabled mode - vision still runs)

4. Compare REPORTED position to ACTUAL position:
   ├── Check Dashboard: Drive/Pose
   ├── Should match within ~10cm when seeing multiple tags
   └── Single tag accuracy varies more with distance

5. If position is WRONG:
   ├── Check camera transforms (most common issue!)
   ├── Verify camera calibration
   └── Ensure correct field layout is loaded


TEST POSITIONS (do all 4 corners + center):

    ┌───────────────────────────────────────┐
    │  ●                               ●    │
    │  Test 1                       Test 2  │
    │                                       │
    │                 ●                     │
    │              Test 5                   │
    │              (center)                 │
    │                                       │
    │  ●                               ●    │
    │  Test 3                       Test 4  │
    └───────────────────────────────────────┘
```

### Test 6: Dynamic Testing

```
PROCEDURE:
════════════════════════════════════════════════════════════════════

1. Open AdvantageScope (recommended) or Shuffleboard

2. Start recording log data

3. Drive robot around while watching:
   ├── Vision/Healthy status
   ├── Estimated pose (should track smoothly)
   └── Vision/ConsecutiveFailures (should stay low)

4. Test specific scenarios:
   ├── Slow driving while facing tags ← Should work great
   ├── Fast rotation (360° spin) ← Vision may pause, that's OK
   ├── Driving away from tags ← Odometry takes over
   └── Driving toward tags ← Vision should correct drift

5. In AdvantageScope, compare:
   ├── Drive/Pose (fused position)
   ├── Drive/OdometryPose (encoder-only position)
   └── Vision/BestEstimatedPose2d (vision-only position)

   They should generally agree, with odometry drifting
   and vision correcting it.
```

---

## Dashboard Interpretation

### Vision Tab Fields

```
┌─────────────────────────────────────────────────────────────────┐
│ VISION DASHBOARD FIELDS                                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ CRITICAL STATUS:                                                │
│ ├── Vision/Healthy          true/false                         │
│ │   └── Should be TRUE when tags visible                       │
│ ├── Vision/HealthStatus     "HEALTHY" / "STALE" / etc.        │
│ │   └── Human-readable status                                  │
│ ├── Vision/Available        true/false                         │
│ │   └── Cameras connected AND enabled                          │
│ └── Vision/AnyTargetsVisible  true/false                       │
│     └── At least one tag seen right now                        │
│                                                                 │
│ PER-CAMERA STATUS (Vision/front/*, Vision/back/*):             │
│ ├── Connected              true/false (camera USB connected)   │
│ ├── HasTargets             true/false (seeing tags)            │
│ ├── TargetCount            0, 1, 2... (number of tags)        │
│ ├── TagIDs                 "1, 5, 12" (which tags visible)    │
│ ├── LatencyMs              30.5 (processing time)              │
│ ├── LatencyOK              true/false (< 100ms)               │
│ ├── BestAmbiguity          0.05 (lowest ambiguity tag)        │
│ ├── AmbiguityOK            true/false (< 0.2)                 │
│ └── ClosestDistance        2.3 (meters to nearest tag)        │
│                                                                 │
│ BEST TARGET INFO:                                               │
│ ├── Vision/BestTarget/TagID       5                            │
│ ├── Vision/BestTarget/Ambiguity   0.03                         │
│ └── Vision/BestTarget/Camera      "front"                      │
│                                                                 │
│ FAILURE TRACKING:                                               │
│ ├── Vision/ConsecutiveFailures    0 (resets on success)       │
│ └── Vision/TotalFailures          12 (since boot)             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### What the Numbers Mean

```
LATENCY (LatencyMs):
├── < 30ms    = Excellent (real-time feel)
├── 30-50ms   = Good (typical)
├── 50-100ms  = Acceptable (your threshold)
└── > 100ms   = Too slow (may cause overshoot)

AMBIGUITY (BestAmbiguity):
├── < 0.05    = Excellent (very confident)
├── 0.05-0.15 = Good
├── 0.15-0.20 = Acceptable (your threshold)
└── > 0.20    = Rejected (not used)

DISTANCE (ClosestDistance):
├── < 1m      = Very accurate
├── 1-2m      = Good accuracy
├── 2-4m      = Moderate accuracy (your limit)
└── > 4m      = Single-tag rejected

CONSECUTIVE FAILURES:
├── 0-3       = Normal operation
├── 4-9       = Something might be wrong
└── 10+       = Vision marked UNHEALTHY
```

---

## Common Problems and Solutions

### Problem: "Camera not detected"

```
SYMPTOMS:
├── Camera not in PhotonVision UI
├── Vision/front/Connected = false
└── No camera stream visible

SOLUTIONS:
1. Check USB cable connection
2. Try different USB port
3. Check USB cable (try a known-good cable)
4. Reboot Raspberry Pi
5. Check Pi power supply (camera needs power)
6. Verify camera works on a laptop first
```

### Problem: "Tags not detected"

```
SYMPTOMS:
├── Camera stream shows but no tag outlines
├── Vision/AnyTargetsVisible = false
└── TargetCount always 0

SOLUTIONS:
1. Check pipeline is set to "AprilTag" not "Colored Shape"
2. Verify Tag Family is "36h11"
3. Check exposure (too dark or too bright?)
4. Ensure 3D mode is enabled
5. Check camera is focused (OV9281 is usually fixed focus)
6. Verify you're looking at actual AprilTags (not just squares)
7. Clean the camera lens
```

### Problem: "High latency"

```
SYMPTOMS:
├── LatencyMs > 100
├── LatencyOK = false
└── Robot feels "laggy" when using vision

SOLUTIONS:
1. Reduce resolution (try 640x480 for testing)
2. Reduce FPS in pipeline settings
3. Ensure Pi 5 has adequate cooling
4. Check network bandwidth (use wired Ethernet!)
5. Disable camera stream when not debugging
6. Reduce decimate value (but increases CPU usage)
```

### Problem: "Position jumps around"

```
SYMPTOMS:
├── Robot position "teleports" on dashboard
├── Erratic driving in auto
└── Vision/ConsecutiveFailures stays at 0

SOLUTIONS:
1. Increase StdDevs (trust vision less):
   kMultiTagHighTrustStdDevXY: 0.5 → 0.7
   kSingleTagBaseTrustStdDevXY: 0.9 → 1.2

2. Lower ambiguity threshold:
   kMaxAmbiguity: 0.2 → 0.15

3. Check camera calibration:
   Reprojection error should be < 0.5

4. Verify camera transforms:
   Wrong transforms cause systematic position errors

5. Look for reflective surfaces:
   Windows, shiny metal, screens can create ghost tags
```

### Problem: "Vision doesn't correct drift"

```
SYMPTOMS:
├── Odometry drifts but vision doesn't fix it
├── Position slowly moves away from reality
└── Vision shows tags but pose doesn't update

SOLUTIONS:
1. Verify vision is ENABLED:
   SmartDashboard → Vision/Enabled = true

2. Check StdDevs aren't too high:
   If vision StdDev >> odometry StdDev, vision is ignored

3. Verify tags are actually being detected:
   Check Vision/AnyTargetsVisible

4. Check ClassroomMode setting:
   Competition mode has stricter thresholds

5. Verify camera transforms are correct:
   Bad transforms make vision "untrusted" automatically
```

### Problem: "Works when still, fails when moving"

```
SYMPTOMS:
├── Good detection when robot stationary
├── Detection drops out during motion
└── Position jumps when robot stops

SOLUTIONS:
1. Lower camera exposure:
   Even global shutter benefits from low exposure

2. Check mounting rigidity:
   Vibration can blur images

3. Reduce angular velocity threshold:
   kMaxAngularVelocityForVisionDegPerSec: 720 → 450

4. Verify camera FPS is high enough:
   60+ FPS recommended

5. Check network latency:
   Wired Ethernet only (no WiFi!)
```

### Problem: "Vision healthy but position wrong"

```
SYMPTOMS:
├── Vision/Healthy = true
├── Tags detected correctly
├── But position is consistently offset

SOLUTIONS:
1. MOST COMMON: Camera transforms are wrong!
   Re-measure X, Y, Z, Pitch, Yaw carefully

2. Field layout mismatch:
   Ensure using correct year's field layout
   AprilTagFields.k2026RebuiltWelded

3. Camera calibration error:
   Re-calibrate with careful procedure

4. Tag orientation:
   Make sure physical tags are placed correctly on field
```

---

## Competition Checklist

### Day Before Competition

```
□ Verify PhotonVision version is stable (not RC/beta)
□ Check camera calibration is complete and saved
□ Test vision at practice field if available
□ Charge backup Raspberry Pi (if you have one)
□ Ensure all cables are secured with zip ties
□ Pack spare USB cables and SD card
```

### Pit Setup

```
□ Verify cameras physically undamaged after transport
□ Power on Pi and verify PhotonVision UI accessible
□ Check both cameras show connected
□ Do quick tag detection test (point at any AprilTag)
□ Verify Dashboard shows Vision/Healthy = true
```

### Before Each Match

```
□ Vision/Enabled = true (not accidentally disabled)
□ Vision/ClassroomMode = false (competition settings)
□ Both cameras connected (check Dashboard)
□ Quick check: point at nearby tag, verify detection
□ Robot at correct starting position for auto
```

### If Vision Fails During Match

```
YOUR CODE ALREADY HANDLES THIS:
├── VisionSubsystem gracefully degrades
├── DriveSubsystem continues with odometry
├── Commands with requireVisionHealthy() stop safely

AFTER MATCH:
1. Check Vision/TotalFailures for error count
2. Review logs in AdvantageScope
3. Check camera connections
4. Test in pit before next match
```

### End of Day

```
□ Review any vision issues from the day
□ Check for loose connections from impacts
□ Verify calibration still valid (re-test if needed)
□ Save logs for later analysis
□ Charge Pi (if using battery backup)
```

---

## Quick Reference Card

Print this and keep in your pit!

```
╔═══════════════════════════════════════════════════════════════════╗
║                    VISION QUICK REFERENCE                         ║
╠═══════════════════════════════════════════════════════════════════╣
║                                                                   ║
║  PhotonVision UI:    http://photonvision.local:5800              ║
║  Camera Names:       "front", "back"                              ║
║                                                                   ║
║  HEALTHY VISION:                                                  ║
║  ├── Vision/Healthy = true                                        ║
║  ├── Latency < 100ms                                              ║
║  ├── Ambiguity < 0.2                                              ║
║  └── Consecutive Failures < 10                                    ║
║                                                                   ║
║  QUICK FIXES:                                                     ║
║  ├── No detection → Check pipeline = AprilTag                    ║
║  ├── High latency → Lower resolution/FPS, check cooling         ║
║  ├── Position jumps → Increase StdDevs, check calibration       ║
║  └── Position offset → Re-measure camera transforms              ║
║                                                                   ║
║  EMERGENCY:                                                       ║
║  └── Set Vision/Enabled = false to disable vision                ║
║      Robot will drive on odometry only                            ║
║                                                                   ║
╚═══════════════════════════════════════════════════════════════════╝
```

---

*Document created for Team 5684 - Vision calibration and debugging guide*
