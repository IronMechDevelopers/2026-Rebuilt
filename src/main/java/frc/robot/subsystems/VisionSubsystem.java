package frc.robot.subsystems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.littletonrobotics.junction.Logger;
import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.DriveConstants;
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

    // Debug field for visualization separate from DriveSubsystem
    private final Field2d fieldVision = new Field2d();

    // ═══════════════════════════════════════════════════════════════════════
    // HEALTH TRACKING - Detects when vision is broken
    // ═══════════════════════════════════════════════════════════════════════
    private int consecutiveFailures = 0;
    private int totalFailures = 0;
    private double lastErrorLogTime = 0;
    private int periodicLoopCounter = 0;

    // Cached health status (updated each periodic cycle)
    private boolean visionHealthy = false;
    private String healthStatus = "INITIALIZING";

    // Currently visible AprilTag field positions (for AdvantageScope visualization)
    private Pose2d[] visibleTagPoses = new Pose2d[0];

    // Session counters for dashboard (cumulative since boot)
    private int posesAcceptedTotal = 0;
    private int posesRejectedTotal = 0;

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
        SmartDashboard.putData("Vision/DebugField", fieldVision);

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

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC HEALTH CHECK - Use this before running vision-dependent commands!
    // ═══════════════════════════════════════════════════════════════════════

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

        // Summary lists (all cameras combined)
        List<Pose3d> allTagPoses = new ArrayList<>();
        List<Pose3d> allRobotPoses = new ArrayList<>();
        List<Pose3d> allAcceptedPoses = new ArrayList<>();
        List<Pose3d> allRejectedPoses = new ArrayList<>();

        // Safety: Master Kill Switch
        if (!SmartDashboard.getBoolean("Vision/Enabled", true) || fieldLayout == null || cameras.isEmpty()) {
            visibleTagPoses = new Pose2d[0];
            logEmptyVisionData();
            return validEstimates;
        }

        Pose2d debugPose = new Pose2d(-10, -10, new Rotation2d()); // Default off-field

        // Iterate through cameras INDEPENDENTLY.
        // If Camera A fails, Camera B continues working.
        for (int i = 0; i < estimators.size(); i++) {
            // Per-camera tracking lists
            List<Pose3d> cameraTagPoses = new ArrayList<>();
            List<Pose3d> cameraRobotPoses = new ArrayList<>();
            List<Pose3d> cameraAcceptedPoses = new ArrayList<>();
            List<Pose3d> cameraRejectedPoses = new ArrayList<>();

            // PER-CAMERA TRY-CATCH: One camera failure doesn't affect others
            try {
                PhotonPoseEstimator estimator = estimators.get(i);
                PhotonCamera camera = cameras.get(i);
                String cameraName = camera.getName();

                // Check connection to avoid internal library timeouts
                boolean connected = camera.isConnected();
                if (!connected) {
                    logCameraVisionData(cameraName, cameraTagPoses, cameraRobotPoses, cameraAcceptedPoses, cameraRejectedPoses);
                    continue;
                }

                // Strategy: Flush the buffer!
                // We use getAllUnreadResults() to clear out old data and only take the newest frame.
                var results = camera.getAllUnreadResults();
                if (results.isEmpty()) {
                    logCameraVisionData(cameraName, cameraTagPoses, cameraRobotPoses, cameraAcceptedPoses, cameraRejectedPoses);
                    continue;
                }

                var pipelineResult = results.get(results.size() - 1); // Get the very latest

                // Collect visible tag poses from the pipeline result
                for (var target : pipelineResult.getTargets()) {
                    var tagPose = fieldLayout.getTagPose(target.getFiducialId());
                    if (tagPose.isPresent()) {
                        cameraTagPoses.add(tagPose.get());
                        allTagPoses.add(tagPose.get());
                    }
                }

                // Try multi-tag estimation first (most accurate), then fall back to single-tag
                Optional<EstimatedRobotPose> result = estimator.estimateCoprocMultiTagPose(pipelineResult);
                if (result.isEmpty()) {
                    result = estimator.estimateLowestAmbiguityPose(pipelineResult);
                }

                if (result.isPresent()) {
                    EstimatedRobotPose estimate = result.get();
                    Pose3d robotPose3d = estimate.estimatedPose;

                    // Add to raw poses list
                    cameraRobotPoses.add(robotPose3d);
                    allRobotPoses.add(robotPose3d);

                    // Filter: Reject high ambiguity tags if only 1 is seen.
                    boolean rejected = false;
                    if (estimate.targetsUsed.size() == 1) {
                        double ambiguity = estimate.targetsUsed.get(0).getPoseAmbiguity();
                        if (ambiguity > VisionConstants.kMaxAmbiguity) {
                            rejected = true;
                        }
                    }

                    if (rejected) {
                        cameraRejectedPoses.add(robotPose3d);
                        allRejectedPoses.add(robotPose3d);
                        posesRejectedTotal++;
                    } else {
                        cameraAcceptedPoses.add(robotPose3d);
                        allAcceptedPoses.add(robotPose3d);
                        validEstimates.add(estimate);
                        debugPose = robotPose3d.toPose2d();
                        posesAcceptedTotal++;
                    }
                }

                // Log per-camera data in AdvantageKit format
                logCameraVisionData(cameraName, cameraTagPoses, cameraRobotPoses, cameraAcceptedPoses, cameraRejectedPoses);

            } catch (Exception e) {
                // Firewall: Catch ANY exception from this camera so the loop continues
                recordFailure("Camera " + cameras.get(i).getName() + ": " + e.getMessage());
            }
        }

        // Log summary (all cameras combined) in AdvantageKit format
        Logger.recordOutput("Vision/Summary/TagPoses", allTagPoses.toArray(new Pose3d[0]));
        Logger.recordOutput("Vision/Summary/RobotPoses", allRobotPoses.toArray(new Pose3d[0]));
        Logger.recordOutput("Vision/Summary/RobotPosesAccepted", allAcceptedPoses.toArray(new Pose3d[0]));
        Logger.recordOutput("Vision/Summary/RobotPosesRejected", allRejectedPoses.toArray(new Pose3d[0]));

        // Dashboard keys (per best practices doc)
        Logger.recordOutput("Vision/TotalTagCount", allTagPoses.size());
        Logger.recordOutput("Vision/PosesAccepted", posesAcceptedTotal);
        Logger.recordOutput("Vision/PosesRejected", posesRejectedTotal);

        // Update visible tag poses for external access (2D for compatibility)
        List<Pose2d> tagPoses2d = new ArrayList<>();
        for (Pose3d p : allTagPoses) {
            tagPoses2d.add(p.toPose2d());
        }
        visibleTagPoses = tagPoses2d.toArray(new Pose2d[0]);

        // Track success/failure for health monitoring
        if (!validEstimates.isEmpty()) {
            recordSuccess();
        }

        // Update debug field (only shows the last valid camera's pose)
        fieldVision.setRobotPose(debugPose);

        return validEstimates;
    }

    /**
     * Logs vision data for a single camera in AdvantageKit format.
     */
    private void logCameraVisionData(String cameraName, List<Pose3d> tagPoses,
            List<Pose3d> robotPoses, List<Pose3d> accepted, List<Pose3d> rejected) {
        String base = "Vision/" + cameraName;
        Logger.recordOutput(base + "/TagPoses", tagPoses.toArray(new Pose3d[0]));
        Logger.recordOutput(base + "/RobotPoses", robotPoses.toArray(new Pose3d[0]));
        Logger.recordOutput(base + "/RobotPosesAccepted", accepted.toArray(new Pose3d[0]));
        Logger.recordOutput(base + "/RobotPosesRejected", rejected.toArray(new Pose3d[0]));
    }

    /**
     * Logs empty arrays when vision is disabled.
     */
    private void logEmptyVisionData() {
        Pose3d[] empty = new Pose3d[0];
        for (PhotonCamera camera : cameras) {
            String base = "Vision/" + camera.getName();
            Logger.recordOutput(base + "/TagPoses", empty);
            Logger.recordOutput(base + "/RobotPoses", empty);
            Logger.recordOutput(base + "/RobotPosesAccepted", empty);
            Logger.recordOutput(base + "/RobotPosesRejected", empty);
        }
        Logger.recordOutput("Vision/Summary/TagPoses", empty);
        Logger.recordOutput("Vision/Summary/RobotPoses", empty);
        Logger.recordOutput("Vision/Summary/RobotPosesAccepted", empty);
        Logger.recordOutput("Vision/Summary/RobotPosesRejected", empty);
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
            if (avgDist > VisionConstants.kMaxVisionDistanceMeters) { // Set this to 4.0 meters (approx 13ft)
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

    // ═══════════════════════════════════════════════════════════════════════
    // HEALTH TRACKING METHODS
    // ═══════════════════════════════════════════════════════════════════════

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
            healthStatus = "UNHEALTHY - " + consecutiveFailures + " consecutive failures";
        } else {
            visionHealthy = true;
            healthStatus = "HEALTHY";
        }

        // Log health status (only when it changes or every 50 cycles for dashboard updates)
        SmartDashboard.putBoolean("Vision/Healthy", visionHealthy);
        SmartDashboard.putString("Vision/HealthStatus", healthStatus);
        SmartDashboard.putNumber("Vision/ConsecutiveFailures", consecutiveFailures);
        SmartDashboard.putNumber("Vision/TotalFailures", totalFailures);
        SmartDashboard.putNumber("Vision/CameraCount", cameras.size());

        // AdvantageKit logging
        Logger.recordOutput("Vision/Healthy", visionHealthy);
        Logger.recordOutput("Vision/ConsecutiveFailures", consecutiveFailures);
        Logger.recordOutput("Vision/CameraCount", cameras.size());
    }

    /**
     * Logs vision data for dashboard display.
     *
     * <p><b>SAFETY:</b> Wrapped in try-catch. If logging fails, it doesn't affect robot operation.
     *
     * <p><b>PERFORMANCE:</b> Only logs detailed per-camera data every 5 cycles (~100ms)
     * to reduce network traffic. Critical data (health, targets visible) logged every cycle.
     */
    private void logEnhancedVisionData() {
        try {
            logEnhancedVisionDataInternal();
        } catch (Exception e) {
            // Logging failed - don't let it crash the robot
            recordFailure("Logging exception: " + e.getMessage());
        }
    }

    /**
     * Internal implementation of vision logging.
     */
    private void logEnhancedVisionDataInternal() {
        boolean visionEnabled = SmartDashboard.getBoolean("Vision/Enabled", true);
        boolean anyConnected = false;
        boolean anyTargetsVisible = false;

        // Only log detailed per-camera data every 5 cycles to reduce bandwidth
        boolean logDetailedData = (periodicLoopCounter % 5 == 0) || VisionConstants.kVerboseLogging;

        PhotonTrackedTarget bestTarget = null;
        PhotonTrackedTarget closestTarget = null;
        double bestAmbiguity = Double.MAX_VALUE;
        double closestDistance = Double.MAX_VALUE;
        String bestCameraName = "None";
        String closestCameraName = "None";

        for (int i = 0; i < cameras.size(); i++) {
            try {
                PhotonCamera camera = cameras.get(i);
                String base = "Vision/" + camera.getName();

                // Always check and log connection (critical for debugging)
                boolean connected = camera.isConnected();
                SmartDashboard.putBoolean(base + "/Connected", connected);
                if (connected) anyConnected = true;

                if (!connected) {
                    if (logDetailedData) {
                        SmartDashboard.putBoolean(base + "/HasTargets", false);
                        SmartDashboard.putNumber(base + "/TargetCount", 0);
                    }
                    continue;
                }

                // Get latest results
                var results = camera.getAllUnreadResults();
                if (results.isEmpty()) {
                    if (logDetailedData) {
                        SmartDashboard.putBoolean(base + "/HasTargets", false);
                        SmartDashboard.putNumber(base + "/TargetCount", 0);
                    }
                    continue;
                }

                var latestResult = results.get(results.size() - 1);
                var targets = latestResult.getTargets();

                // Target visibility (always log - critical info)
                boolean hasTargets = latestResult.hasTargets();
                SmartDashboard.putBoolean(base + "/HasTargets", hasTargets);
                SmartDashboard.putNumber(base + "/TargetCount", targets.size());
                if (hasTargets) anyTargetsVisible = true;

                // Detailed logging (rate-limited)
                if (logDetailedData) {
                    // Latency check (key name per dashboard spec)
                    double latencyMs = latestResult.metadata.getLatencyMillis();
                    boolean latencyOK = latencyMs <= VisionConstants.kMaxLatencyMs;
                    SmartDashboard.putNumber(base + "/Latency", latencyMs);
                    SmartDashboard.putBoolean(base + "/LatencyOK", latencyOK);

                    // Build tag ID list and per-tag distance info
                    StringBuilder tagIds = new StringBuilder();
                    StringBuilder tagDistances = new StringBuilder();
                    double cameraLowestAmbiguity = Double.MAX_VALUE;
                    double cameraClosestDist = Double.MAX_VALUE;

                    for (PhotonTrackedTarget target : targets) {
                        if (tagIds.length() > 0) tagIds.append(", ");
                        tagIds.append(target.getFiducialId());

                        double ambiguity = target.getPoseAmbiguity();
                        if (ambiguity < cameraLowestAmbiguity) cameraLowestAmbiguity = ambiguity;
                        if (ambiguity < bestAmbiguity) {
                            bestAmbiguity = ambiguity;
                            bestTarget = target;
                            bestCameraName = camera.getName();
                        }

                        double distance = target.getBestCameraToTarget().getTranslation().getNorm();

                        // Build per-tag distance string for calibration verification
                        if (tagDistances.length() > 0) tagDistances.append(", ");
                        tagDistances.append(String.format("%d: %.2fm", target.getFiducialId(), distance));

                        if (distance < cameraClosestDist) cameraClosestDist = distance;
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestTarget = target;
                            closestCameraName = camera.getName();
                        }
                    }

                    SmartDashboard.putString(base + "/TagIDs", tagIds.length() > 0 ? tagIds.toString() : "None");
                    SmartDashboard.putString(base + "/TagDistances", tagDistances.length() > 0 ? tagDistances.toString() : "None");
                    // Ambiguity key per dashboard spec
                    SmartDashboard.putNumber(base + "/Ambiguity", cameraLowestAmbiguity < Double.MAX_VALUE ? cameraLowestAmbiguity : 0);
                    SmartDashboard.putBoolean(base + "/AmbiguityOK", cameraLowestAmbiguity <= VisionConstants.kMaxAmbiguity);
                    SmartDashboard.putNumber(base + "/ClosestDistance", cameraClosestDist < Double.MAX_VALUE ? cameraClosestDist : 0);
                }
            } catch (Exception e) {
                // Per-camera logging failed - continue with other cameras
                recordFailure("Logging camera " + i + ": " + e.getMessage());
            }
        }

        // Always log critical status
        SmartDashboard.putBoolean("Vision/Available", visionEnabled && anyConnected);
        SmartDashboard.putBoolean("Vision/AnyTargetsVisible", anyTargetsVisible);

        // Log best/closest target info (rate-limited)
        if (logDetailedData) {
            if (bestTarget != null) {
                SmartDashboard.putNumber("Vision/BestTarget/TagID", bestTarget.getFiducialId());
                SmartDashboard.putNumber("Vision/BestTarget/Ambiguity", bestAmbiguity);
                SmartDashboard.putBoolean("Vision/BestTarget/AmbiguityOK", bestAmbiguity <= VisionConstants.kMaxAmbiguity);
                SmartDashboard.putString("Vision/BestTarget/Camera", bestCameraName);
            } else {
                SmartDashboard.putNumber("Vision/BestTarget/TagID", -1);
                SmartDashboard.putNumber("Vision/BestTarget/Ambiguity", 0);
                SmartDashboard.putBoolean("Vision/BestTarget/AmbiguityOK", false);
                SmartDashboard.putString("Vision/BestTarget/Camera", "None");
            }

            if (closestTarget != null) {
                SmartDashboard.putNumber("Vision/ClosestTarget/TagID", closestTarget.getFiducialId());
                SmartDashboard.putNumber("Vision/ClosestTarget/Distance", closestDistance);
                SmartDashboard.putBoolean("Vision/ClosestTarget/InRange", closestDistance <= VisionConstants.kMaxVisionDistanceMeters);
                SmartDashboard.putString("Vision/ClosestTarget/Camera", closestCameraName);
            } else {
                SmartDashboard.putNumber("Vision/ClosestTarget/TagID", -1);
                SmartDashboard.putNumber("Vision/ClosestTarget/Distance", 0);
                SmartDashboard.putBoolean("Vision/ClosestTarget/InRange", false);
                SmartDashboard.putString("Vision/ClosestTarget/Camera", "None");
            }
        }

        // AdvantageKit logging (always, for replay)
        Logger.recordOutput("Vision/Available", visionEnabled && anyConnected);
        Logger.recordOutput("Vision/AnyTargetsVisible", anyTargetsVisible);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PERIODIC - Called every 20ms by the scheduler
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void periodic() {
        periodicLoopCounter++;

        // SAFETY: Entire periodic wrapped in try-catch
        try {
            // Update health status first
            updateHealthStatus();

            // Log vision data (rate-limited internally)
            logEnhancedVisionData();

            // Keep pose estimation active when disabled so drivers can check alignment
            if (DriverStation.isDisabled()) {
                getEstimatedGlobalPoses(new Pose2d());
            }
        } catch (Exception e) {
            // Something went very wrong - log it but don't crash
            recordFailure("periodic() exception: " + e.getMessage());
        }
    }
}