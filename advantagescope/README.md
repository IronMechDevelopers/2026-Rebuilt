# AdvantageScope Dashboard Layouts

This directory contains AdvantageScope layout files for the 2026 REBUILT robot. AdvantageScope is a powerful tool for visualizing robot telemetry during live operation and post-match replay analysis.

## Quick Start

### 1. Install AdvantageScope
Download and install from: https://github.com/Mechanical-Advantage/AdvantageScope/releases

**Supported platforms:** Windows, macOS, Linux

### 2. Choose Your Layout

We provide two pre-configured dashboard layouts:

#### **drive-team.json** (Live Competition)
**Purpose:** Use during matches for real-time monitoring
**Best for:** Drive team, drive coach, human player

**What it shows:**
- Large shift countdown timer
- Hub status (ACTIVE/INACTIVE)
- Next shift status (plan ahead!)
- Warning banner (CLEAR ZONE!, GET READY!, etc.)
- Robot position on field
- Battery voltage and vision health
- Distance to hub and scoring range

**How to use:**
1. Connect laptop to robot network
2. Open AdvantageScope
3. File → Connect to Robot → Enter `10.56.84.2`
4. File → Open Layout → Select `drive-team.json`

#### **analysis.json** (Post-Match Review)
**Purpose:** Detailed match analysis and debugging
**Best for:** Entire team after matches, programmers

**What it shows:**
- Synchronized timeline graphs (scrub through match)
- Multiple robot pose sources (fused, odometry-only, vision)
- Driver input analysis
- Vision performance metrics (PPS, accepted/rejected poses)
- Battery and CAN bus health over time
- AprilTag acceptance/rejection visualization

**How to use:**
1. Download log file from robot USB drive (path: `/U/*.wpilog`)
2. Open AdvantageScope
3. File → Open Log → Select `.wpilog` file
4. File → Open Layout → Select `analysis.json`

## Creating Custom Layouts

### Manual Layout Creation

Since AdvantageScope layouts are complex JSON files, it's easier to create them using the AdvantageScope UI rather than writing JSON by hand.

**Steps to create a custom layout:**

1. **Launch AdvantageScope and connect to robot/log**
   - Live: File → Connect to Robot → Enter robot IP
   - Replay: File → Open Log → Select `.wpilog` file

2. **Add widgets from the sidebar**
   - Click the "+" button to add a new tab/view
   - Right-click in a view → Add Widget
   - Choose widget type (Text Display, Number Display, Line Graph, 3D Field, etc.)

3. **Configure widget sources**
   - Each widget needs a NetworkTables key as its data source
   - Common sources for our robot:
     - `Match/Phase` - Current match phase
     - `Match/ShiftCountdown` - Countdown to next shift
     - `Match/Warning` - Warning message
     - `Match/HubActive` - Boolean: Is our hub active?
     - `Match/NextStatus` - Next hub status
     - `Odometry/Robot` - Robot pose (fused)
     - `Drive/DistanceToHub` - Distance to hub
     - `Vision/Healthy` - Vision system status
     - `RobotController/BatteryVoltage` - Battery voltage

4. **Arrange and style widgets**
   - Drag to reposition
   - Drag corners/edges to resize
   - Right-click → Properties to adjust font size, colors, ranges

5. **Save your layout**
   - File → Save Layout As → Choose location in `advantagescope/` directory
   - Name it descriptively (e.g., `driver-view.json`, `coach-detailed.json`)

### Recommended Layouts for This Year's Game

Since shifts are critical in 2026 REBUILT, your layouts should emphasize:

1. **Current shift status** - Large, immediately visible
2. **Shift countdown** - Huge font (96pt+), always visible
3. **Next shift status** - What's coming after current shift ends
4. **Warning banner** - Full-width, colored background
5. **Field position** - Where are we vs where should we be?

## NetworkTables Keys Reference

All robot telemetry is published to NetworkTables under these keys:

### Match State (from MatchStateTracker)
| Key | Type | Description |
|-----|------|-------------|
| `Match/Phase` | String | Current phase (AUTO, TRANSITION, SHIFT 1-4, ENDGAME) |
| `Match/HubStatus` | String | Our hub status (ACTIVE/INACTIVE/UNKNOWN) |
| `Match/NextStatus` | String | Hub status after next shift |
| `Match/HubActive` | Boolean | Quick boolean check for hub active |
| `Match/ShiftCountdown` | Number | Seconds until next shift (ceiling) |
| `Match/TimeUntilShift` | Number | Seconds until next shift (precise) |
| `Match/Warning` | String | Warning message for drive team |
| `Match/FmsDataReceived` | Boolean | Has FMS data arrived? |
| `Match/ShiftPattern` | String[] | Full match timeline (7 elements) |
| `Match/PhaseIndex` | Number | Phase ordinal value for graphing |
| `Match/PracticeMode` | Boolean | Is practice mode active? |

### Drive System (from DriveSubsystem)
| Key | Type | Description |
|-----|------|-------------|
| `Odometry/Robot` | Pose2d | Fused pose (odometry + vision) |
| `Odometry/OdometryOnly` | Pose2d | Pure encoder odometry (drifts) |
| `Odometry/TruePose` | Pose2d | Ground truth (simulation only) |
| `Odometry/VisionPose` | Pose2d | Latest raw vision estimate |
| `Odometry/AcceptedTags` | Pose3d[] | AprilTags used for localization |
| `Odometry/RejectedTags` | Pose3d[] | AprilTags filtered out |
| `Drive/MeasuredStates` | SwerveModuleState[] | Module velocities and angles |
| `Drive/MeasuredSpeeds` | ChassisSpeeds | Robot velocity (vx, vy, omega) |
| `Drive/DistanceToHub` | Number | Distance to scoring hub (meters) |
| `Drive/InHubRange` | Boolean | Within scoring range? |
| `Gyro/YawDeg` | Number | Robot heading in degrees |

### Vision System (from VisionSubsystem)
| Key | Type | Description |
|-----|------|-------------|
| `Vision/Healthy` | Boolean | Is vision system working? |
| `Vision/TagCount` | Number | AprilTags currently detected |
| `Vision/PPS/Received` | Number | Poses per second received |
| `Vision/PPS/Accepted` | Number | Poses per second accepted |
| `Vision/Connected` | Boolean | Are cameras connected? |

### Controller Inputs (from RobotContainer)
| Key | Type | Description |
|-----|------|-------------|
| `DriverInputs/LeftX`, `LeftY`, `RightX`, `RightY` | Number | Joystick axes |
| `DriverInputs/LeftTrigger`, `RightTrigger` | Number | Analog triggers |
| `DriverInputs/A`, `B`, `X`, `Y`, etc. | Boolean | Button states |
| `CoDriverInputs/*` | Various | Co-driver controller inputs |

### System Health
| Key | Type | Description |
|-----|------|-------------|
| `RobotController/BatteryVoltage` | Number | Battery voltage (volts) |
| `CAN/PercentUtilization` | Number | CAN bus utilization (0-100%) |
| `CAN/BusOffCount`, `TxFullCount` | Number | CAN error counters |

## Recording Logs

Logs are automatically recorded by AdvantageKit whenever the robot is enabled.

**Log storage:**
- **Real robot:** USB drive (`/U/` path)
- **Simulation:** Project directory (`logs/` folder)

**Log format:** `.wpilog` files (standard WPILib format)

**Naming:** Logs are timestamped automatically (e.g., `Log_2026-03-15_14-23-45.wpilog`)

**After competition:**
1. Remove USB drive from roboRIO
2. Copy `.wpilog` files to computer
3. Open in AdvantageScope for analysis

## Troubleshooting

### Problem: No data showing in AdvantageScope
**Solutions:**
- Check NetworkTables connection (green status indicator in bottom-right)
- Verify robot is enabled and code is running
- Try disconnecting and reconnecting (File → Disconnect, then File → Connect to Robot)
- Check robot IP address is correct: `10.56.84.2` (or `10.TE.AM.2` for your team)

### Problem: Can't connect to robot
**Solutions:**
- Verify laptop is on robot network (WiFi or Ethernet)
- Ping robot: `ping 10.56.84.2` in terminal/command prompt
- Check robot is powered on and radio is blinking green
- Try USB connection directly to roboRIO instead of WiFi

### Problem: Layout looks wrong or widgets are blank
**Solutions:**
- Ensure robot code is deployed and running
- Check that NetworkTables keys exist (use NT Viewer in AdvantageScope)
- Verify AdvantageKit logging is enabled in Robot.java
- Some keys only exist during specific match phases (e.g., shift data in teleop)

### Problem: Field visualization not showing robot
**Solutions:**
- Check `Odometry/Robot` key exists in NetworkTables
- Verify pose is being published (look at NT Viewer)
- Make sure field image is loaded (should auto-load for FRC fields)
- Try reloading the layout file

### Problem: Log file won't open
**Solutions:**
- Ensure file is `.wpilog` format (not `.rlog` or other)
- Check file isn't corrupted (should be >100 KB if match was recorded)
- Try opening with File → Open Log (not dragging into window)
- Verify you have latest version of AdvantageScope

## Best Practices for Drive Team

### Before Match
1. **Connect to robot early** - Give AdvantageScope time to sync
2. **Load drive-team.json layout** - Don't scramble during queue
3. **Check all indicators show green/active:**
   - Vision Healthy = TRUE
   - Battery Voltage ≥ 12.0V
   - Field shows robot position updating

### During Match
1. **Watch the countdown** - Primary focus should be shift timer
2. **Listen for warnings** - "CLEAR ZONE!" / "GET READY!" appear 9 seconds before shifts
3. **Check Next Status** - Plan where to position before current shift ends
4. **Monitor distance to hub** - Are we in scoring range?
5. **Quick battery check** - If voltage drops below 11.5V, play more defensively

### After Match
1. **Download log immediately** - Before USB drive fills up
2. **Review with team** - Load analysis.json and discuss strategy
3. **Check vision performance** - Were we rejecting too many tags?
4. **Analyze shift reactions** - Did we move early enough?

## Training with Practice Mode

The robot has a built-in **Practice Mode** for training shift timing without FMS connection.

**How to enable:**
1. Open Shuffleboard → Software tab
2. Toggle "Practice Mode" to TRUE
3. Robot will cycle through shifts continuously (25s each)
4. Use this to train drive team on shift awareness!

**Practice mode features:**
- Cycles: Shift 1 → Shift 2 → Shift 3 → Shift 4 → (repeat)
- No AUTO/TRANSITION/ENDGAME phases
- Warning banners still appear at 9 seconds before shift
- Perfect for driver training in pit or shop

**Training drill:**
- Run Practice Mode for 10 minutes
- Drive coach calls out shifts 15-20 seconds early
- Drivers practice moving between hubs before shifts occur
- Goal: React proactively, not reactively!

## Support and Resources

**AdvantageScope Documentation:**
https://github.com/Mechanical-Advantage/AdvantageScope/blob/main/docs/INDEX.md

**AdvantageKit Documentation:**
https://github.com/Mechanical-Advantage/AdvantageKit/blob/main/docs/WHAT-IS-ADVANTAGEKIT.md

**FRC Documentation:**
https://docs.wpilib.org/

**Team-Specific Questions:**
Ask the programming team or check the team Discord/Slack

---

**Last Updated:** 2026 Season
**Maintained By:** Programming Team
**Robot:** 5684 2026 REBUILT
