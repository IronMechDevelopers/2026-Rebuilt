# Vision Pipeline Explained

A comprehensive guide for FRC teams new to vision systems.

---

## Table of Contents

1. [The Big Picture](#the-big-picture)
2. [How the Robot Knows Where It Is](#how-the-robot-knows-where-it-is)
3. [Standard Deviation Explained](#standard-deviation-explained)
4. [Ambiguity Explained](#ambiguity-explained)
5. [The Complete Vision Pipeline](#the-complete-vision-pipeline)
6. [Multi-Tag vs Single-Tag Estimation](#multi-tag-vs-single-tag-estimation)
7. [Pose Fusion: Combining Vision and Odometry](#pose-fusion-combining-vision-and-odometry)
8. [Match Scenario Walkthrough](#match-scenario-walkthrough)
9. [Key Parameters Reference](#key-parameters-reference)

---

## The Big Picture

Your robot needs to know **where it is on the field** to:
- Drive to specific locations autonomously
- Aim at targets accurately
- Know which game pieces to pick up

Vision uses **AprilTags** (the QR-code-like squares around the field) as landmarks. Since we know exactly where each tag is placed on the field, when the camera sees a tag, we can calculate where the robot must be.

---

## How the Robot Knows Where It Is

There are two methods, and your code uses **both together**:

### Method 1: Odometry (Wheel Encoders + Gyro)

**How it works:** "I started here, I moved this far, so I must be here now"

```
START ──────────────────────────────────────> CALCULATED POSITION
  │                                                    │
  │  Wheel encoders track distance traveled            │
  │  Gyro tracks rotation                              │
  │  Math combines them into position                  │
  └────────────────────────────────────────────────────┘
```

| Pros | Cons |
|------|------|
| Works all the time | Drifts over time |
| Very fast (every 20ms) | Wheels can slip |
| No external dependencies | Small errors accumulate |

**In your code:** `SwerveDriveOdometry` in `DriveSubsystem.java`

### Method 2: Vision (Cameras + AprilTags)

**How it works:** "I see tag #5, it's at this angle and distance, so I must be here"

```
CAMERA SEES TAG ──> PHOTONVISION PROCESSES ──> ROBOT POSITION CALCULATED
        │                    │                          │
        │  Tag ID, distance  │  Uses field layout       │
        │  angle, ambiguity  │  and camera position     │
        └────────────────────┴──────────────────────────┘
```

| Pros | Cons |
|------|------|
| Absolute position (no drift!) | Only works when tags visible |
| Corrects accumulated errors | Can be wrong if miscalibrated |
| Multi-tag is very accurate | Affected by lighting/motion |

**In your code:** `VisionSubsystem.java`

### The Fusion: Best of Both Worlds

Your `SwerveDrivePoseEstimator` combines both methods using a **Kalman Filter**:

- When vision is unavailable: Trust odometry
- When vision sees tags: Blend both based on confidence levels
- Result: Accurate position that doesn't drift

---

## Standard Deviation Explained

### The Concept

**Standard Deviation (StdDev)** answers the question: *"How uncertain is this measurement?"*

Think of it like asking two friends for directions:
- **Friend A (reliable):** "The store is 2 blocks north" - Usually correct
- **Friend B (unreliable):** "The store is 2 blocks north" - Often wrong

Both say the same thing, but you'd trust Friend A more. **StdDev tells the robot which "friend" to trust.**

### The Numbers

```
LOW StdDev (0.1)  = "I'm very confident"  = HIGH trust
HIGH StdDev (5.0) = "This could be wrong" = LOW trust
```

### Visual Representation

```
    Low StdDev (0.1)              High StdDev (2.0)
    "Very confident"              "Not sure at all"

          ●                        ░░░░░░░░░░░
         ╱│╲                      ░░░░░░░░░░░░
      ──●─●─●──                   ░░░░●░░░░░░░
         ╲│╱                      ░░░░░░░░░░░░
          ●                        ░░░░░░░░░░░

    Small uncertainty             Large uncertainty
    area around position          area around position
```

### In Your Code

```java
// VisionConstants.java

// Multi-tag detection (very reliable)
kMultiTagHighTrustStdDevXY = 0.5;           // Trust a lot
kMultiTagHighTrustStdDevThetaDegrees = 10.0;

// Single-tag detection (less reliable)
kSingleTagBaseTrustStdDevXY = 0.9;          // Trust less
kSingleTagBaseTrustStdDevThetaDegrees = 30.0;
```

```java
// DriveSubsystem.java constructor

poseEstimator = new SwerveDrivePoseEstimator(
    // ... other parameters ...

    // Odometry trust: [x, y, rotation]
    VecBuilder.fill(0.1, 0.1, 0.1),  // Trust encoders/gyro quite a bit

    // Vision trust: [x, y, rotation] - overwritten per frame
    VecBuilder.fill(1.5, 1.5, 1.5)   // Default: trust vision less
);
```

### How Fusion Uses StdDev

When combining odometry and vision, the pose estimator does a **weighted average**:

```
Example:
  Odometry says:  X = 3.0m  (StdDev = 0.1)
  Vision says:    X = 3.2m  (StdDev = 0.5)

  Since odometry has lower StdDev (more trusted):
  Result ≈ 3.04m  (closer to odometry's answer)

Another example:
  Odometry says:  X = 3.0m  (StdDev = 0.1)
  Vision says:    X = 3.2m  (StdDev = 0.1)  ← Multi-tag, high trust

  Equal trust:
  Result ≈ 3.1m  (average of both)
```

---

## Ambiguity Explained

### The Problem

When a camera sees an AprilTag at certain angles, there can be **two mathematically valid poses** that look identical from the camera's perspective.

```
CAMERA VIEW OF APRILTAG

Straight-on (Low Ambiguity):      Angled (High Ambiguity):

┌─────────────────┐               ╱─────────────────╲
│  ■ ■     ■ ■ ■  │              ╱   ■ ■     ■ ■ ■   ╲
│    ■ ■ ■     ■  │             ╱      ■ ■ ■     ■    ╲
│  ■     ■   ■ ■  │             ╲    ■     ■   ■ ■   ╱
│  ■ ■ ■     ■    │              ╲   ■ ■ ■     ■    ╱
└─────────────────┘               ╲───────────────╱

Camera knows exactly              Camera sees two possible
where the tag is                  solutions - which is right?
Ambiguity: 0.02                   Ambiguity: 0.35
```

### What Ambiguity Values Mean

| Ambiguity | Quality | Action |
|-----------|---------|--------|
| 0.0 - 0.1 | Excellent | Always use |
| 0.1 - 0.2 | Good | Use (your threshold) |
| 0.2 - 0.4 | Questionable | Reject |
| 0.4+ | Bad | Definitely reject |

### In Your Code

```java
// VisionConstants.java
public static final double kMaxAmbiguity = 0.2;  // Reject above this

// VisionSubsystem.java - The filtering logic
if (estimate.targetsUsed.size() == 1) {
    double ambiguity = estimate.targetsUsed.get(0).getPoseAmbiguity();
    if (ambiguity > VisionConstants.kMaxAmbiguity) {
        continue;  // SKIP this detection - too risky!
    }
}
```

### Why Multi-Tag Has No Ambiguity Problem

When two or more tags are visible, the math has **only one solution**:

```
Single Tag: Could be Position A OR Position B
            (ambiguous)

Two Tags:   Only ONE position can see both tags
            at these exact angles and distances
            (unambiguous)
```

This is why your code tries multi-tag estimation first.

---

## The Complete Vision Pipeline

Here's the step-by-step flow of how vision data becomes robot position:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         VISION PROCESSING PIPELINE                          │
└─────────────────────────────────────────────────────────────────────────────┘

STEP 1: CAMERA CAPTURES IMAGE
════════════════════════════════════════════════════════════════════════════════
    Your OV9281 camera takes a picture (60-90 times per second)
    Global shutter means no motion blur distortion

         Hardware: OV9281 → USB → Raspberry Pi 5


STEP 2: PHOTONVISION PROCESSES (on Raspberry Pi)
════════════════════════════════════════════════════════════════════════════════
    PhotonVision software finds AprilTags in the image

    For each tag found, it calculates:
    ├── Tag ID (which tag? #1, #5, #12, etc.)
    ├── 3D Transform (position and rotation relative to camera)
    ├── Distance (how far away in meters)
    └── Ambiguity (how confident is this detection?)

         Runs on: Raspberry Pi 5
         Accessed via: http://photonvision.local:5800


STEP 3: DATA SENT TO ROBORIO (NetworkTables)
════════════════════════════════════════════════════════════════════════════════
    PhotonVision publishes results over the network
    Your robot code reads them via PhotonCamera class

         Code location: VisionSubsystem.java
         Method: camera.getAllUnreadResults()


STEP 4: PHOTONPOSEESTIMATOR CALCULATES ROBOT POSITION
════════════════════════════════════════════════════════════════════════════════
    Uses three pieces of information:

    A) Where is the tag on the field?
       → From AprilTagFieldLayout (WPILib provides this for each game)
       → Example: Tag #5 is at field position (5.0, 2.0, 1.5)

    B) Where is the tag relative to the camera?
       → From PhotonVision's detection
       → Example: Tag is 2m away, 30° to the left

    C) Where is the camera on the robot?
       → From your VisionConstants (the transform you measured!)
       → Example: Camera is 0.3m forward, 0.3m up from robot center

    Calculation:
    ┌────────────────────────────────────────────────────────────┐
    │  Field Position of Tag                                     │
    │  - Camera-to-Tag Transform (from PhotonVision)             │
    │  - Robot-to-Camera Transform (from VisionConstants)        │
    │  ═══════════════════════════════════════════════           │
    │  = Robot Position on Field                                 │
    └────────────────────────────────────────────────────────────┘

         Code: estimator.estimateCoprocMultiTagPose(pipelineResult)
               estimator.estimateLowestAmbiguityPose(pipelineResult)


STEP 5: YOUR CODE FILTERS THE RESULT
════════════════════════════════════════════════════════════════════════════════
    Not every detection should be trusted!

    Checks performed:
    ├── Is vision enabled? (SmartDashboard kill switch)
    ├── Is the robot spinning too fast? (motion blur risk)
    ├── Is ambiguity acceptable? (< 0.2 for single tag)
    └── Is distance reasonable? (implicit in StdDev scaling)

    If checks fail → Detection is thrown away
    If checks pass → Detection goes to fusion

         Code location: VisionSubsystem.java:200-273


STEP 6: POSE ESTIMATOR FUSES VISION + ODOMETRY
════════════════════════════════════════════════════════════════════════════════
    SwerveDrivePoseEstimator combines:

    ┌─────────────┐     ┌─────────────┐
    │  ODOMETRY   │     │   VISION    │
    │ (encoders + │     │ (AprilTags) │
    │    gyro)    │     │             │
    └──────┬──────┘     └──────┬──────┘
           │                   │
           │   StdDev: 0.1     │   StdDev: 0.5-1.5
           │   (high trust)    │   (variable trust)
           │                   │
           └─────────┬─────────┘
                     │
              ┌──────▼──────┐
              │   KALMAN    │
              │   FILTER    │
              │  (weighted  │
              │   average)  │
              └──────┬──────┘
                     │
              ┌──────▼──────┐
              │    FUSED    │
              │    POSE     │
              │  (best of   │
              │   both!)    │
              └─────────────┘

         Code: poseEstimator.addVisionMeasurement(pose, timestamp, stdDevs)


STEP 7: ROBOT USES THE POSITION
════════════════════════════════════════════════════════════════════════════════
    The fused position is used by:
    ├── PathPlanner (autonomous driving)
    ├── Auto-aim commands (rotation to face targets)
    ├── Distance calculations (am I close enough to score?)
    └── Dashboard display (driver awareness)

         Code: drive.getCurrentPose()
```

---

## Multi-Tag vs Single-Tag Estimation

Your code uses two estimation strategies, in order of preference:

### Strategy 1: Multi-Tag (Preferred)

```java
// VisionSubsystem.java:231
Optional<EstimatedRobotPose> result = estimator.estimateCoprocMultiTagPose(pipelineResult);
```

**When it works:** Camera sees 2+ AprilTags simultaneously

```
Camera View:
┌─────────────────────────────────────┐
│                                     │
│     ┌─────┐           ┌─────┐      │
│     │ #1  │           │ #5  │      │
│     └─────┘           └─────┘      │
│                                     │
└─────────────────────────────────────┘
```

**Why it's better:**
- Two reference points define position precisely
- Errors in one tag are corrected by the other
- No ambiguity problem (only one mathematical solution)
- Used trust: `kMultiTagHighTrustStdDevXY = 0.5`

### Strategy 2: Single-Tag (Fallback)

```java
// VisionSubsystem.java:234
result = estimator.estimateLowestAmbiguityPose(pipelineResult);
```

**When it's used:** Only 1 tag visible, or multi-tag failed

```
Camera View:
┌─────────────────────────────────────┐
│                                     │
│              ┌─────┐                │
│              │ #3  │                │
│              └─────┘                │
│                                     │
└─────────────────────────────────────┘
```

**Why it's less reliable:**
- Single point has multiple possible solutions
- Accuracy depends heavily on viewing angle
- That's why ambiguity filtering is critical!
- Used trust: `kSingleTagBaseTrustStdDevXY = 0.9` (scaled by distance)

### The "Lowest Ambiguity" Part

If multiple single tags are visible but multi-tag failed, `estimateLowestAmbiguityPose` picks the tag with the **lowest ambiguity score** (most confident detection).

---

## Pose Fusion: Combining Vision and Odometry

### The Kalman Filter Concept

The `SwerveDrivePoseEstimator` uses a Kalman filter. You don't need to understand the math, just the concept:

**It's a smart weighted average that:**
1. Predicts where the robot should be (using odometry)
2. Compares prediction to measurement (vision)
3. Adjusts based on how much it trusts each source

### Example Scenario

```
Current odometry position:  (3.0, 2.0)  with StdDev 0.1
Vision measurement:         (3.2, 2.1)  with StdDev 0.5

Question: What should the fused position be?

Since odometry StdDev (0.1) < vision StdDev (0.5):
  → Trust odometry MORE
  → Result will be CLOSER to odometry

Simplified calculation:
  Weight_odom = 1/0.1 = 10
  Weight_vision = 1/0.5 = 2

  X = (3.0 × 10 + 3.2 × 2) / (10 + 2) = 3.03
  Y = (2.0 × 10 + 2.1 × 2) / (10 + 2) = 2.02

Result: (3.03, 2.02) - Mostly trusts odometry
```

### Why This Matters

- **Vision glitch?** Odometry keeps you stable
- **Odometry drift?** Vision corrects it over time
- **Neither perfect?** Best estimate from both

---

## Match Scenario Walkthrough

Here's what happens during a typical autonomous period:

```
TIME 0.0s: MATCH STARTS
═══════════════════════════════════════════════════════════════════════
    Odometry:  (1.50, 5.00)  ← Starting position (set in code)
    Vision:    No tags visible yet (robot facing wrong way)

    RESULT:    Use odometry position: (1.50, 5.00)


TIME 0.5s: ROBOT MOVES, SEES 2 APRILTAGS
═══════════════════════════════════════════════════════════════════════
    Odometry:  (2.30, 4.80)  ← Wheels say we moved here
    Vision:    (2.40, 4.90)  ← Multi-tag detection

    Trust levels:
      Odometry StdDev: 0.1 (high trust)
      Vision StdDev:   0.5 (high trust - multi-tag!)

    RESULT:    Fused position: (2.35, 4.85)
               Vision corrects odometry slightly


TIME 1.0s: ROBOT TURNS FAST (400°/sec)
═══════════════════════════════════════════════════════════════════════
    Odometry:  (3.00, 4.50)
    Vision:    REJECTED - Angular velocity > 720°/sec threshold
               (Actually it's 400, but this is an example of rejection)

    RESULT:    Use odometry only: (3.00, 4.50)
               Vision ignored during fast rotation


TIME 1.5s: ROBOT SLOWS, SEES 1 APRILTAG
═══════════════════════════════════════════════════════════════════════
    Odometry:  (3.50, 4.20)  ← Has drifted from earlier spin
    Vision:    (3.60, 4.00)  ← Single tag, ambiguity = 0.15 (OK!)

    Trust levels:
      Odometry StdDev: 0.1
      Vision StdDev:   1.2 (single tag at 2m distance)

    RESULT:    Fused position: (3.52, 4.15)
               Small correction toward vision


TIME 2.0s: SEES TAG WITH HIGH AMBIGUITY
═══════════════════════════════════════════════════════════════════════
    Odometry:  (4.00, 3.80)
    Vision:    Ambiguity = 0.35 (> 0.2 threshold)

    RESULT:    Vision REJECTED
               Use odometry: (4.00, 3.80)


TIME 2.5s: ARRIVES AT SCORING POSITION, SEES 3 TAGS
═══════════════════════════════════════════════════════════════════════
    Odometry:  (4.45, 3.55)  ← Accumulated some drift
    Vision:    (4.50, 3.50)  ← Multi-tag, very accurate

    Trust levels:
      Odometry StdDev: 0.1
      Vision StdDev:   0.5 (multi-tag!)

    RESULT:    Fused position: (4.48, 3.52)
               Position is now highly accurate for scoring!
```

---

## Key Parameters Reference

### VisionConstants.java

| Parameter | Default | Description |
|-----------|---------|-------------|
| `kFrontCameraName` | "front" | Must match PhotonVision UI exactly |
| `kBackCameraName` | "back" | Must match PhotonVision UI exactly |
| `kFrontCameraToRobotX/Y/Z` | 0.0 | **MUST MEASURE!** Camera position on robot |
| `kFrontCameraPitchRadians` | 0.0 | Camera tilt angle (negative = looking down) |
| `kMaxVisionDistanceMeters` | 4.0 | Reject single tags farther than this |
| `kMaxAmbiguity` | 0.2 | Reject single tags with higher ambiguity |
| `kMultiTagHighTrustStdDevXY` | 0.5 | How much to trust multi-tag (lower = more) |
| `kSingleTagBaseTrustStdDevXY` | 0.9 | Base trust for single tag |
| `kVisionTrustScaleDenominator` | 30.0 | Distance scaling factor |
| `kMaxConsecutiveFailures` | 10 | Failures before marking vision unhealthy |
| `kVisionStaleTimeoutSeconds` | 2.0 | Seconds without data before "stale" warning |

### DriveConstants.java

| Parameter | Default | Description |
|-----------|---------|-------------|
| `kStateStdDevX/Y/Theta` | 0.1 | How much to trust odometry |
| `kVisionStdDevX/Y/Theta` | 1.5 | Default vision trust (overwritten per frame) |
| `kMaxAngularVelocityForVisionDegPerSec` | 720 | Reject vision when spinning faster |
| `kVisionLargeCorrectionThreshold_Competition` | 0.5 | Distance for "large" correction |
| `kVisionSmallCorrectionThreshold_Competition` | 1.0 | Max correction for single tag |

### Tuning Guidelines

```
SYMPTOM: Robot position "jumps" or "teleports" randomly
─────────────────────────────────────────────────────────
  CAUSE:   Vision is trusted too much, or bad detections getting through
  FIX:
    → Increase StdDevs (trust vision less)
    → Lower kMaxAmbiguity (stricter filtering)
    → Check camera calibration


SYMPTOM: Robot position drifts, vision doesn't correct it
─────────────────────────────────────────────────────────
  CAUSE:   Vision is not trusted enough, or not seeing tags
  FIX:
    → Decrease StdDevs (trust vision more)
    → Check dashboard - are cameras seeing tags?
    → Verify camera transforms are correct
    → Increase kMaxVisionDistanceMeters if tags are far


SYMPTOM: Vision works when still, fails when moving
─────────────────────────────────────────────────────────
  CAUSE:   Motion blur or exposure too high
  FIX:
    → Lower camera exposure in PhotonVision
    → Ensure using global shutter camera (OV9281 is good!)
    → Lower kMaxAngularVelocityForVisionDegPerSec


SYMPTOM: Position accurate near tags, wrong when far
─────────────────────────────────────────────────────────
  CAUSE:   Normal! Single-tag accuracy decreases with distance
  FIX:
    → This is expected behavior
    → Your distance-based StdDev scaling handles this
    → Consider tag placement strategy in auto paths
```

---

## Summary

| Concept | One-Sentence Explanation |
|---------|--------------------------|
| **Odometry** | Track position by counting wheel rotations |
| **Vision** | Calculate position by seeing AprilTags |
| **Pose Fusion** | Combine both methods for best accuracy |
| **Standard Deviation** | Number representing measurement uncertainty |
| **Ambiguity** | How confident is the camera about its detection |
| **Multi-Tag** | Seeing 2+ tags = very accurate, no ambiguity |
| **Single-Tag** | Seeing 1 tag = less accurate, needs filtering |

---

*Document created for Team 5684 - First-time vision implementation guide*
