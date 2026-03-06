# Drive & Vision Subsystem Guide

## DriveSubsystem
The drive subsystem fuses inputs from encoders, the gyro, and the vision system.

### How Pose Estimation Works
1. **Odometry:** Calculates position based on wheel rotation. Fast, but drifts over time.
2. **Vision:** Updates position based on AprilTags. Fixes drift, but can be noisy.
3. **Fusion:** `SwerveDrivePoseEstimator` combines both.

### Brownout Protection
If voltage drops below 6.5V, the system flags a warning. Brownouts cause the RoboRIO to reboot. 
*Solution:* Change battery or check wiring if this happens often.

---

## VisionSubsystem
We use PhotonVision running on a coprocessor (Limelight/Orange Pi).

### Trust Logic (Standard Deviations)
We give a "Trust Score" to vision data.
* **Multi-Tag:** Trusted highly. Corrections are applied immediately.
* **Single-Tag (Close):** Trusted moderately.
* **Single-Tag (Far):** Trusted poorly. Ignored if > 4 meters away.

### Tuning
If the robot "jitters" when seeing a tag, increase `kVisionStdDevX/Y` in `Constants.java`.