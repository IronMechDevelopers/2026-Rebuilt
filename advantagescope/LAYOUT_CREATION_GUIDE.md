# Creating AdvantageScope Layouts

The layout JSON files (`drive-team.json` and `analysis.json`) need to be created using the AdvantageScope application.
Writing these files by hand is error-prone and time-consuming.

## Steps to Create Layouts

### Prerequisites
1. Deploy the robot code with all the new telemetry logging
2. Install AdvantageScope from: https://github.com/Mechanical-Advantage/AdvantageScope/releases

### Option 1: Connect to Live Robot

1. **Start robot code**
   - Deploy code to robot or run in simulation
   - Enable the robot

2. **Launch AdvantageScope**
   - Open the application
   - File → Connect to Robot
   - Enter robot IP: `10.56.84.2` (or your team number)

3. **Create Drive Team Layout**
   - Add a new tab (click "+" at bottom)
   - Add these widgets:

**Top Section (Shift Awareness):**
- **Text Display** → Source: `Match/Phase`
  - Font: 36pt, Bold
  - Position: Top-left
- **Text Display** → Source: `Match/Warning`
  - Font: 72pt, Bold
  - Color: Use conditional formatting (red for "CLIMB NOW!", yellow for "GET READY!")
  - Position: Full width below phase
- **Number Display** → Source: `Match/ShiftCountdown`
  - Font: 96pt, Bold
  - Show as integer
  - Position: Top-right, large square
- **Boolean Indicator (LED)** → Source: `Match/HubActive`
  - Size: 200x200px
  - True color: Green, False color: Red
  - Label: "OUR HUB"
- **Text Display** → Source: `Match/NextStatus`
  - Font: 48pt
  - Background: Yellow
  - Label: "NEXT:"

**Middle Section (Field):**
- **3D Field** → Add multiple sources:
  - Robot pose: `Odometry/Robot`
  - Camera angle: 45° isometric
  - Size: 60% of screen width

**Bottom Section (Status):**
- **Gauge** → Source: `RobotController/BatteryVoltage`
  - Range: 0-13V
  - Red zone: <11.5, Yellow: 11.5-12, Green: >12
- **Boolean Indicator** → Source: `Vision/Healthy`
  - True: Green, False: Red
- **Number Display** → Source: `Drive/DistanceToHub`
  - Units: meters
  - Decimal places: 1
- **Boolean Indicator** → Source: `Drive/InHubRange`
  - True: Green, False: Gray

4. **Save the layout**
   - File → Save Layout As
   - Name: `drive-team.json`
   - Save to: `advantagescope/` directory

### Option 2: Use a Recorded Log

If you have a recorded match log (`.wpilog` file):

1. **Open log in AdvantageScope**
   - File → Open Log
   - Select a `.wpilog` file from robot or simulation

2. **Follow steps 3-4 above** to create layouts

### Creating Analysis Layout

For the analysis layout, add these additional widgets:

**Timeline Graphs (Synchronized):**
- **Line Graph** → Add sources:
  - `Match/ShiftCountdown`
  - `Match/PhaseIndex`
  - Enable time sync across all graphs

- **Line Graph** → Vision performance:
  - `Vision/PPS/Received`
  - `Vision/PPS/Accepted`
  - Y-axis: 0-30

- **Line Graph** → Distance tracking:
  - `Drive/DistanceToHub`
  - `Drive/InHubRange` (boolean rendered as 0/1)

**Field with Multiple Poses:**
- **3D Field** → Add all pose sources:
  - `Odometry/Robot` (primary - gold)
  - `Odometry/OdometryOnly` (comparison - blue, dashed)
  - `Odometry/VisionPose` (vision - green, ghost/transparent)
  - `Odometry/AcceptedTags` (green AprilTag 3D models)
  - `Odometry/RejectedTags` (red AprilTag 3D models)

**Driver Input Graphs:**
- **Line Graph** → Controller inputs:
  - `DriverInputs/LeftX`
  - `DriverInputs/LeftY`
  - `DriverInputs/RightX`
  - `DriverInputs/RightY`
  - Y-axis: -1 to 1

**System Health:**
- **Line Graph** → System monitoring:
  - `RobotController/BatteryVoltage`
  - `CAN/PercentUtilization`

Save as `analysis.json` in `advantagescope/` directory.

## Quick Test

After creating layouts:

1. **Test with simulation:**
   ```bash
   ./gradlew simulateJava
   ```

2. **Connect AdvantageScope to simulation:**
   - File → Connect to Robot → Enter `localhost` or `127.0.0.1`

3. **Load your layout:**
   - File → Open Layout → Select your JSON file

4. **Enable Practice Mode:**
   - Use Shuffleboard to enable "Practice Mode"
   - Watch shifts cycle in AdvantageScope

5. **Verify all widgets update**
   - Countdown should decrement
   - Hub status should toggle
   - Warnings should appear

## Tips

- **Use grid snapping** for clean layouts (View → Grid → Enable Snapping)
- **Group related widgets** using containers
- **Save frequently** while creating layouts
- **Test with real match data** if possible (record a practice match)
- **Get feedback from drive team** on readability and usefulness
- **Iterate!** Layouts are easy to modify - don't try to perfect on first try

## Troubleshooting

**Widget shows "No Data":**
- Check NetworkTables key is spelled correctly (case-sensitive!)
- Verify robot code is logging that key
- Check robot is enabled (some data only exists when enabled)

**Layout looks wrong when shared:**
- Save as absolute paths might cause issues
- Use relative paths when possible
- Share the JSON file + instructions to load it

**Performance issues:**
- Too many widgets can slow down AdvantageScope
- Reduce graph history length if laggy
- Close unused tabs

---

**Note:** Once you've created these layouts, delete this guide file or move it to a docs folder.
The actual JSON layout files will be the ones used by the drive team.
