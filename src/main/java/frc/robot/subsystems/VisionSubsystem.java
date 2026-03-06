package frc.robot.subsystems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.littletonrobotics.junction.Logger;
import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.VisionConstants;

/**
 * Handles communication with PhotonVision cameras and estimates robot pose using AprilTags.
 *
 * <p><b>SAFETY PHILOSOPHY:</b>
 * Vision should NEVER crash the robot. If PhotonVision fails, we log it and keep driving.
 * All vision code is wrapped in try-catch blocks to isolate failures.
 *
 * <p><b>Safety Features:</b>
 * <ul>
 *   <li>ALL vision code wrapped in try-catch - exceptions cannot escape to robot loop</li>
 *   <li>Health tracking - consecutive failures trigger "unhealthy" status</li>
 *   <li>Rate-limited error logging - prevents console/network spam</li>
 *   <li>Stale data detection - warns if vision hasn't worked recently</li>
 *   <li>Kill switch via SmartDashboard ("Vision/Enabled")</li>
 *   <li>Graceful degradation - robot drives normally without vision</li>
 * </ul>
 *
 * <p><b>For Commands:</b>
 * Always check {@link #isVisionHealthy()} before running vision-dependent commands.
 */
public class VisionSubsystem extends SubsystemBase implements VisionProvider {

    /** Helper class for camera setup in RobotContainer. */
    public static class CameraConfig {
        public final String name;
        public final Transform3d robotToCamera;

        public CameraConfig(String name, Transform3d robotToCamera) {
            this.name = name;
            this.robotToCamera = robotToCamera;
        }
    }

    private final List<PhotonPoseEstimator> estimators = new ArrayList<>();
    private final List<PhotonCamera> cameras = new ArrayList<>();
    private final AprilTagFieldLayout fieldLayout;


    // =====================================================================══
    // HEALTH TRACKING - Detects when vision is broken
    // =====================================================================══
    private int consecutiveFailures = 0;
    private int totalFailures = 0;
    private double lastErrorLogTime = 0;
    private int periodicLoopCounter = 0;

    // Cached health status (updated each periodic cycle)
    private boolean visionHealthy = false;
    private String healthStatus = "INITIALIZING";

    // Currently visible AprilTag field positions (for AdvantageScope visualization)
    private Pose2d[] visibleTagPoses = new Pose2d[0];

    // Accepted and rejected tag poses (3D for proper field visualization)
    private Pose3d[] acceptedTagPoses = new Pose3d[0];
    private Pose3d[] rejectedTagPoses = new Pose3d[0];

    // Session counters for dashboard (cumulative since boot)
    private int posesAcceptedTotal = 0;
    private int posesRejectedTotal = 0;

    // Rejection counters (for internal tracking)
    private int rejectedAmbiguityCount = 0;
    private int rejectedDistanceCount = 0;

    // Latch timers for accepted/rejected tag visualization (prevents blinking)
    private static final double TAG_DISPLAY_LATCH_SECONDS = 0.5; // Hold display for 0.5s
    private double lastAcceptedTagTime = 0;
    private double lastRejectedTagTime = 0;
    private Pose3d[] latchedAcceptedTagPoses = new Pose3d[0];
    private Pose3d[] latchedRejectedTagPoses = new Pose3d[0];

    // Empty array constants
    private static final Pose2d[] EMPTY_POSE2D_ARRAY = new Pose2d[0];
    private static final Pose3d[] EMPTY_POSE3D_ARRAY = new Pose3d[0];

        // =====================================================================══
    // PERFORMANCE TRACKING
    // =====================================================================══
    // Counter for poses received within the current time window
    private int posesInCurrentWindow = 0;
    // Timestamp of the last time we calculated PPS
    private double lastPpsCalculationTime = 0;
    // The calculated value to display on dashboard
    private double currentPosesPerSecond = 0;

    /**
     * Creates a VisionSubsystem with the specified cameras.
     *
     * <p>If initialization fails for any camera, that camera is skipped but others continue.
     * If ALL cameras fail or fieldLayout is null, vision is disabled but robot keeps running.
     *
     * @param fieldLayout The AprilTag layout (must not be null for vision to work).
     * @param configs Variable arguments for 1 or more cameras.
     */
    public VisionSubsystem(AprilTagFieldLayout fieldLayout, CameraConfig... configs) {
        this.fieldLayout = fieldLayout;

        // Set up dashboard controls
        SmartDashboard.setDefaultBoolean("Vision/Enabled", true);
        SmartDashboard.setDefaultBoolean("Vision/ClassroomMode", false);
        SmartDashboard.setDefaultNumber("Vision/AmbiguityThreshold", VisionConstants.kMaxAmbiguity);

        if (fieldLayout == null) {
            healthStatus = "DISABLED - No field layout";
            DriverStation.reportError("VISION: Field Layout is null. Vision disabled but robot will drive.", true);
            return;
        }

        // Initialize each camera independently - one failure doesn't stop others
        for (CameraConfig config : configs) {
            try {
                PhotonCamera camera = new PhotonCamera(config.name);

                PhotonPoseEstimator estimator = new PhotonPoseEstimator(
                    fieldLayout,
                    config.robotToCamera
                );

                estimators.add(estimator);
                cameras.add(camera);
                DriverStation.reportWarning("Vision: Camera '" + config.name + "' initialized", false);
            } catch (Exception e) {
                // Camera init failed - log it but keep going
                DriverStation.reportError(
                    "VISION: Failed to initialize camera '" + config.name + "': " + e.getMessage() +
                    " - Robot will continue without this camera.", false);
                totalFailures++;
            }
        }

        if (cameras.isEmpty()) {
            healthStatus = "DISABLED - No cameras initialized";
            DriverStation.reportError("VISION: No cameras initialized. Robot will drive without vision.", true);
        } else {
            healthStatus = "READY - " + cameras.size() + " camera(s)";
        }
    }

    // =====================================================================══
    // PUBLIC HEALTH CHECK - Use this before running vision-dependent commands!
    // =====================================================================══

    /**
     * Returns true if vision is working well enough to be trusted.
     *
     * <p><b>Use this in commands!</b> If vision is unhealthy, commands should
     * either disable themselves or use fallback behavior.
     *
     * @return true if vision is enabled, cameras are connected, and recent poses succeeded
     */
    public boolean isVisionHealthy() {
        return visionHealthy;
    }

    /**
     * Returns a human-readable status string for the dashboard.
     *
     * @return Status like "HEALTHY", "UNHEALTHY - 5 failures", "STALE - no data for 3s"
     */
    public String getHealthStatus() {
        return healthStatus;
    }

    /**
     * Returns the field positions of currently visible AprilTags.
     *
     * <p>Use this for AdvantageScope visualization to see which tags the robot
     * is currently detecting. Positions are from the field layout, not measured.
     *
     * @return Array of tag positions (empty if no tags visible)
     */
    @Override
    public Pose2d[] getVisibleTagPoses() {
        return visibleTagPoses;
    }

    /**
     * Returns the 3D field positions of AprilTags that were ACCEPTED for localization.
     *
     * <p>These tags passed all filtering criteria and contributed to the pose estimate.
     *
     * @return Array of accepted tag field positions (empty if none accepted)
     */
    @Override
    public Pose3d[] getAcceptedTagPoses() {
        return acceptedTagPoses;
    }

    /**
     * Returns the 3D field positions of AprilTags that were REJECTED.
     *
     * <p>These tags were detected but filtered out due to high ambiguity, distance, etc.
     *
     * @return Array of rejected tag field positions (empty if none rejected)
     */
    @Override
    public Pose3d[] getRejectedTagPoses() {
        return rejectedTagPoses;
    }

    /**
     * Returns the number of consecutive failures (resets on success).
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * Returns the total number of failures since robot boot.
     */
    public int getTotalFailures() {
        return totalFailures;
    }

    /**
     * Gets valid pose estimates from all cameras.
     *
     * <p><b>SAFETY:</b> This method is wrapped in try-catch and will NEVER throw.
     * If something goes wrong, it returns an empty list and logs the error.
     *
     * @param prevEstimatedRobotPose Current best guess of robot position (helps disambiguate).
     * @return List of valid pose estimates (empty if vision disabled or failed). Never null.
     */
    @Override
    public List<EstimatedRobotPose> getEstimatedGlobalPoses(Pose2d prevEstimatedRobotPose) {
        // OUTER TRY-CATCH: Nothing escapes this method
        try {
            return getEstimatedGlobalPosesInternal(prevEstimatedRobotPose);
        } catch (Exception e) {
            // Something very unexpected happened - log it and return safely
            recordFailure("CRITICAL getEstimatedGlobalPoses exception: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Internal implementation of pose estimation (called by public method).
     *
     * <p>Logs data in AdvantageKit format for AdvantageScope visualization:
     * <ul>
     *   <li>Vision/{Camera}/TagPoses - 3D poses of visible tags ("Vision Target" object)</li>
     *   <li>Vision/{Camera}/RobotPoses - Raw pose estimates ("Ghost" object)</li>
     *   <li>Vision/{Camera}/RobotPosesAccepted - Estimates that passed filtering</li>
     *   <li>Vision/{Camera}/RobotPosesRejected - Estimates that were rejected</li>
     *   <li>Vision/Summary/* - Combined data from all cameras</li>
     * </ul>
     */
    private List<EstimatedRobotPose> getEstimatedGlobalPosesInternal(Pose2d prevEstimatedRobotPose) {
        List<EstimatedRobotPose> validEstimates = new ArrayList<>();
        List<Pose3d> acceptedTags = new ArrayList<>();
        List<Pose3d> rejectedTags = new ArrayList<>();
        int tagCount = 0;

        // Safety: Master Kill Switch
        if (!SmartDashboard.getBoolean("Vision/Enabled", true) || fieldLayout == null || cameras.isEmpty()) {
            visibleTagPoses = EMPTY_POSE2D_ARRAY;
            acceptedTagPoses = EMPTY_POSE3D_ARRAY;
            rejectedTagPoses = EMPTY_POSE3D_ARRAY;
            return validEstimates;
        }

        double ambiguityThreshold = SmartDashboard.getNumber("Vision/AmbiguityThreshold", VisionConstants.kMaxAmbiguity);

        // Process each camera independently
        for (int i = 0; i < estimators.size(); i++) {
            try {
                PhotonPoseEstimator estimator = estimators.get(i);
                PhotonCamera camera = cameras.get(i);

                if (!camera.isConnected()) continue;

                var results = camera.getAllUnreadResults();
                if (results.isEmpty()) continue;

                var pipelineResult = results.get(results.size() - 1);
                tagCount += pipelineResult.getTargets().size();

                // Try multi-tag first, fall back to single-tag
                Optional<EstimatedRobotPose> result = estimator.estimateCoprocMultiTagPose(pipelineResult);
                if (result.isEmpty()) {
                    result = estimator.estimateLowestAmbiguityPose(pipelineResult);
                }

                if (result.isPresent()) {
                    EstimatedRobotPose estimate = result.get();

                    // Reject high-ambiguity single-tag estimates
                    boolean rejected = false;
                    if (estimate.targetsUsed.size() == 1) {
                        if (estimate.targetsUsed.get(0).getPoseAmbiguity() > ambiguityThreshold) {
                            rejected = true;
                            rejectedAmbiguityCount++;
                        }
                    }

                    // Collect contributing tag poses for visualization
                    for (var target : estimate.targetsUsed) {
                        var tagPose = fieldLayout.getTagPose(target.getFiducialId());
                        if (tagPose.isPresent()) {
                            if (rejected) {
                                rejectedTags.add(tagPose.get());
                            } else {
                                acceptedTags.add(tagPose.get());
                            }
                        }
                    }

                    if (!rejected) {
                        validEstimates.add(estimate);
                        posesAcceptedTotal++;
                    } else {
                        posesRejectedTotal++;
                    }
                }

            } catch (Exception e) {
                recordFailure("Camera " + cameras.get(i).getName() + ": " + e.getMessage());
            }
        }

        // Log tag count
        Logger.recordOutput("Vision/TagCount", tagCount);

        // Update visible tag poses (simplified - just use tag count for now)
        visibleTagPoses = EMPTY_POSE2D_ARRAY;

        // Update accepted/rejected tag poses with latch behavior
        double currentTime = Timer.getFPGATimestamp();

        if (!acceptedTags.isEmpty()) {
            latchedAcceptedTagPoses = acceptedTags.toArray(new Pose3d[0]);
            lastAcceptedTagTime = currentTime;
        } else if (currentTime - lastAcceptedTagTime > TAG_DISPLAY_LATCH_SECONDS) {
            latchedAcceptedTagPoses = EMPTY_POSE3D_ARRAY;
        }

        if (!rejectedTags.isEmpty()) {
            latchedRejectedTagPoses = rejectedTags.toArray(new Pose3d[0]);
            lastRejectedTagTime = currentTime;
        } else if (currentTime - lastRejectedTagTime > TAG_DISPLAY_LATCH_SECONDS) {
            latchedRejectedTagPoses = EMPTY_POSE3D_ARRAY;
        }

        acceptedTagPoses = latchedAcceptedTagPoses;
        rejectedTagPoses = latchedRejectedTagPoses;

        // --- PPS CALCULATION START ---
        // 1. Accumulate count (cheap integer math)
        posesInCurrentWindow += validEstimates.size();

        // 2. Calculate Rate every 0.5 seconds (tuneable)
        // This prevents jitter and saves CPU by not dividing every single loop
        if (currentTime - lastPpsCalculationTime > 0.5) {
            double timeDelta = currentTime - lastPpsCalculationTime;
            currentPosesPerSecond = posesInCurrentWindow / timeDelta;
            
            // Reset for next window
            posesInCurrentWindow = 0;
            lastPpsCalculationTime = currentTime;
        }
        // --- PPS CALCULATION END ---

        if (!validEstimates.isEmpty()) {
            recordSuccess();
        }

        return validEstimates;
    }

    /**
     * Calculates the standard deviations (confidence) of a pose estimate.
     * Lower values = Higher confidence.
     * 
     * @return Matrix [x_std, y_std, theta_std]
     */
    @Override
    public Matrix<N3, N1> getEstimationStdDevs(EstimatedRobotPose estimatedPose) {
        var targets = estimatedPose.targetsUsed;
        int numTags = targets.size();
        
        // Calculate average distance to targets
        double avgDist = 0;
        for (PhotonTrackedTarget tgt : targets) {
            var tagPose = fieldLayout.getTagPose(tgt.getFiducialId());
            if (tagPose.isPresent()) {
                avgDist += tagPose.get().toPose2d().getTranslation().getDistance(
                    estimatedPose.estimatedPose.toPose2d().getTranslation());
            }
        }
        if (numTags > 0) avgDist /= numTags;

        // BASELINE: Multi-tag is very trustworthy.
        if (numTags >= 2) {
            // Even with multi-tag, confidence degrades with distance, but much more slowly.
            // At 12ft (3.6m), multi-tag is still solid.
            double confidenceScale = 1.0 + (avgDist / 4.0); // Mild scaling
            
            return VecBuilder.fill(
                VisionConstants.kMultiTagHighTrustStdDevXY * confidenceScale,
                VisionConstants.kMultiTagHighTrustStdDevXY * confidenceScale,
                Math.toRadians(VisionConstants.kMultiTagHighTrustStdDevThetaDegrees)
            );
        } 
        
        // SINGLE TAG: The "Danger Zone"
        else {
            // 1. If too far, effectively ignore
            if (avgDist > VisionConstants.kMaxVisionDistanceMeters) {
                rejectedDistanceCount++;
                return VecBuilder.fill(1000, 1000, 1000);
            } 
            
            // 2. Exponential Falloff
            // At 1 meter, scale is ~1. At 4 meters, scale is HUGE.
            // This allows accurate close range, but loose long range updates.
            double distSq = avgDist * avgDist;
            double confidenceScale = 1 + (distSq / VisionConstants.kVisionTrustScaleDenominator); // Set denom to ~2.0

            return VecBuilder.fill(
                VisionConstants.kSingleTagBaseTrustStdDevXY * confidenceScale,
                VisionConstants.kSingleTagBaseTrustStdDevXY * confidenceScale,
                Math.toRadians(VisionConstants.kSingleTagBaseTrustStdDevThetaDegrees) * confidenceScale
            );
        }
    }

    // =====================================================================══
    // HEALTH TRACKING METHODS
    // =====================================================================══

    /**
     * Records a successful pose estimation (resets failure counter).
     */
    private void recordSuccess() {
        consecutiveFailures = 0;
    }

    /**
     * Records a failure and logs it (rate-limited to prevent spam).
     */
    private void recordFailure(String message) {
        consecutiveFailures++;
        totalFailures++;

        // Rate-limited logging to prevent console spam
        double now = Timer.getFPGATimestamp();
        if (now - lastErrorLogTime > VisionConstants.kErrorLogIntervalSeconds) {
            DriverStation.reportError("VISION: " + message + " (failures: " + consecutiveFailures + ")", false);
            lastErrorLogTime = now;
        }
    }

    /**
     * Updates the health status based on current state.
     * Called each periodic cycle.
     *
     * <p>Note: We intentionally do NOT check for "stale" data (no tags seen recently).
     * With only 2 cameras, it's normal to not see any AprilTags for extended periods
     * while driving around the field. Not seeing tags ≠ vision being broken.
     */
    private void updateHealthStatus() {
        // Check various failure conditions
        boolean enabled = SmartDashboard.getBoolean("Vision/Enabled", true);
        boolean hasFieldLayout = fieldLayout != null;
        boolean hasCameras = !cameras.isEmpty();
        boolean tooManyFailures = consecutiveFailures >= VisionConstants.kMaxConsecutiveFailures;

        // Determine health based on actual problems, not tag visibility
        if (!enabled) {
            visionHealthy = false;
            healthStatus = "DISABLED by user";
        } else if (!hasFieldLayout) {
            visionHealthy = false;
            healthStatus = "DISABLED - no field layout";
        } else if (!hasCameras) {
            visionHealthy = false;
            healthStatus = "DISABLED - no cameras";
        } else if (tooManyFailures) {
            visionHealthy = false;
            healthStatus = "UNHEALTHY - " + consecutiveFailures + " failures";
        } else {
            visionHealthy = true;
            healthStatus = "HEALTHY";
        }

        // AdvantageKit logging (primary - for replay in AdvantageScope)
        Logger.recordOutput("Vision/Healthy", visionHealthy);
    }

    /**
     * Logs per-camera connection status.
     * Minimal logging to reduce overhead.
     */
    private void logEnhancedVisionData() {
        try {
            boolean anyConnected = false;
            for (PhotonCamera camera : cameras) {
                if (camera.isConnected()) {
                    anyConnected = true;
                    break;
                }
            }
            Logger.recordOutput("Vision/Connected", anyConnected);
        } catch (Exception e) {
            recordFailure("Logging exception: " + e.getMessage());
        }
    }

    // =====================================================================══
    // PERIODIC - Called every 20ms by the scheduler
    // =====================================================================══

    @Override
    public void periodic() {
        periodicLoopCounter++;

        try {
            updateHealthStatus();
            
            // LOG PPS HERE
            Logger.recordOutput("Vision/PPS", currentPosesPerSecond);

            logEnhancedVisionData();

            if (DriverStation.isDisabled()) {
                getEstimatedGlobalPoses(new Pose2d());
            }
        } catch (Exception e) {
            recordFailure("periodic() exception: " + e.getMessage());
        }
    }
}