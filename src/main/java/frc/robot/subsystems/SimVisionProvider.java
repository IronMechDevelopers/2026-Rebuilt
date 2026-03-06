package frc.robot.subsystems;

import java.util.ArrayList;
import java.util.List;

import org.littletonrobotics.junction.Logger;
import org.photonvision.EstimatedRobotPose;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.constants.VisionConstants;

/**
 * Simulated vision provider for testing vision pipeline without hardware.
 *
 * <p>This provider simulates a camera that can see AprilTags within a configurable
 * range and field of view. It generates realistic pose estimates with distance-based
 * ambiguity to test the full filtering pipeline.
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li>Configurable detection range (default: 9 feet / 2.74m)</li>
 *   <li>Configurable field of view (default: 70 degrees)</li>
 *   <li>Distance-based ambiguity simulation</li>
 *   <li>Optional noise injection for realism</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 * // In VisionSetup - uses reference pose from DriveSubsystem automatically
 * VisionProvider simVision = new SimVisionProvider(fieldLayout);
 * </pre>
 */
public class SimVisionProvider implements VisionProvider {

    // =====================================================================══
    // CONFIGURATION - Adjustable simulation parameters
    // =====================================================================══

    /** Maximum detection range in meters (9 feet = 2.74m) */
    private static final double DEFAULT_MAX_RANGE_METERS = Units.feetToMeters(9.0);

    /** Camera horizontal field of view in degrees */
    private static final double DEFAULT_FOV_DEGREES = 70.0;

    /** Add small random noise to simulated poses (more realistic) */
    private static final boolean ADD_NOISE = true;

    /** Noise standard deviation in meters (at 1m distance) */
    private static final double NOISE_STDDEV_METERS = 0.02;

    // =====================================================================══
    // STATE
    // =====================================================================══

    private final AprilTagFieldLayout fieldLayout;
    private final double maxRangeMeters;
    private final double fovDegrees;

    // Simulated camera transform (front-facing by default)
    private final Transform3d robotToCamera;

    // Tracking for visualization
    private Pose3d[] acceptedTagPoses = new Pose3d[0];
    private Pose3d[] rejectedTagPoses = new Pose3d[0];
    private Pose2d[] visibleTagPoses = new Pose2d[0];

    // Health tracking
    private boolean visionHealthy = true;

    // =====================================================================══
    // TRUE POSE SUPPLIER - Provides ground truth for vision estimates
    // Set by DriveSubsystem after construction via setTruePoseSupplier()
    // =====================================================================══

    /** Supplies the TRUE robot pose (ground truth from SimDrivetrain) */
    private java.util.function.Supplier<Pose2d> truePoseSupplier = null;

    /**
     * Creates a simulated vision provider with default settings.
     *
     * <p>Uses the referencePose passed to getEstimatedGlobalPoses() as the
     * robot's "true" position in simulation. This avoids circular dependencies.
     *
     * @param fieldLayout The AprilTag field layout
     */
    public SimVisionProvider(AprilTagFieldLayout fieldLayout) {
        this(fieldLayout, DEFAULT_MAX_RANGE_METERS, DEFAULT_FOV_DEGREES);
    }

    /**
     * Creates a simulated vision provider with custom range and FOV.
     *
     * @param fieldLayout The AprilTag field layout
     * @param maxRangeMeters Maximum detection range in meters
     * @param fovDegrees Camera field of view in degrees
     */
    public SimVisionProvider(
            AprilTagFieldLayout fieldLayout,
            double maxRangeMeters,
            double fovDegrees) {

        this.fieldLayout = fieldLayout;
        this.maxRangeMeters = maxRangeMeters;
        this.fovDegrees = fovDegrees;

        // Default: front-facing camera at robot center
        this.robotToCamera = new Transform3d(
            new Translation3d(0.0, 0.0, 0.5), // 0.5m up (typical camera height)
            new Rotation3d(0, 0, 0) // Facing forward
        );

        // Initialize dashboard
        SmartDashboard.putNumber("SimVision/MaxRangeFeet", Units.metersToFeet(maxRangeMeters));
        SmartDashboard.putNumber("SimVision/FOVDegrees", fovDegrees);
        SmartDashboard.putBoolean("SimVision/Active", true);

        DriverStation.reportWarning(
            String.format("SimVisionProvider active - Range: %.1f ft, FOV: %.0f°",
                Units.metersToFeet(maxRangeMeters), fovDegrees),
            false
        );
    }

    /**
     * Sets the supplier for the TRUE robot pose (ground truth).
     * Call this from DriveSubsystem after SimDrivetrain is created.
     *
     * <p>When tags are visible, SimVisionProvider will return estimates based on
     * this true pose, allowing the fused pose to be corrected toward it.
     *
     * @param supplier Supplies the true robot pose from SimDrivetrain
     */
    public void setTruePoseSupplier(java.util.function.Supplier<Pose2d> supplier) {
        this.truePoseSupplier = supplier;
        DriverStation.reportWarning("SimVisionProvider: True pose supplier set", false);
    }

    @Override
    public List<EstimatedRobotPose> getEstimatedGlobalPoses(Pose2d referencePose) {
        List<EstimatedRobotPose> estimates = new ArrayList<>();
        List<Pose3d> visibleTags3d = new ArrayList<>();
        List<Pose2d> visibleTags2d = new ArrayList<>();
        List<PhotonTrackedTarget> targetsUsed = new ArrayList<>();

        // =====================================================================══
        // DETERMINE ROBOT POSITION FOR FOV CALCULATIONS
        // Use TRUE pose if available (camera is physically there)
        // Fall back to reference pose if true pose supplier not set
        // =====================================================================══
        Pose2d truePose = (truePoseSupplier != null) ? truePoseSupplier.get() : referencePose;
        Pose2d robotPose = truePose;  // Camera is physically at true position
        double robotHeading = robotPose.getRotation().getRadians();

        // Log what we're using
        SmartDashboard.putBoolean("SimVision/HasTruePoseSupplier", truePoseSupplier != null);

        // Allow runtime adjustment of range
        double effectiveRange = SmartDashboard.getNumber("SimVision/MaxRangeFeet", Units.metersToFeet(maxRangeMeters));
        effectiveRange = Units.feetToMeters(effectiveRange);

        // Check each tag in the field layout
        for (var tagEntry : fieldLayout.getTags()) {
            int tagId = tagEntry.ID;
            Pose3d tagPose3d = tagEntry.pose;
            Pose2d tagPose2d = tagPose3d.toPose2d();

            // Calculate distance to tag
            double distance = robotPose.getTranslation().getDistance(tagPose2d.getTranslation());

            // Check if within range
            if (distance > effectiveRange) {
                continue;
            }

            // Check if tag is FACING the robot (tag's front side must be visible)
            // The tag's +Z axis points outward from the tag face
            // We need the robot to be in front of the tag (on the +Z side)
            Rotation3d tagRotation = tagPose3d.getRotation();

            // Get the tag's facing direction (where the tag is looking)
            // AprilTag +Z axis points out of the tag face
            double tagFacingX = -Math.sin(tagRotation.getZ()) * Math.cos(tagRotation.getY());
            double tagFacingY = Math.cos(tagRotation.getZ()) * Math.cos(tagRotation.getY());

            // Vector from tag to robot
            double toRobotX = robotPose.getX() - tagPose2d.getX();
            double toRobotY = robotPose.getY() - tagPose2d.getY();

            // Dot product: positive means robot is in front of tag
            double dotProduct = tagFacingX * toRobotX + tagFacingY * toRobotY;
            if (dotProduct < 0) {
                // Robot is behind the tag (can't see the front face)
                continue;
            }

            // Check if tag is in front of robot (within camera FOV)
            double angleToTag = Math.atan2(
                tagPose2d.getY() - robotPose.getY(),
                tagPose2d.getX() - robotPose.getX()
            );
            double relativeAngle = angleToTag - robotHeading;

            // Normalize angle to [-PI, PI]
            while (relativeAngle > Math.PI) relativeAngle -= 2 * Math.PI;
            while (relativeAngle < -Math.PI) relativeAngle += 2 * Math.PI;

            // Check if within FOV (half on each side)
            double halfFovRadians = Math.toRadians(fovDegrees / 2.0);
            if (Math.abs(relativeAngle) > halfFovRadians) {
                continue;
            }

            // Tag is visible! Add to list
            visibleTags3d.add(tagPose3d);
            visibleTags2d.add(tagPose2d);

            // Create a simulated PhotonTrackedTarget
            // Calculate simulated ambiguity based on distance (closer = lower ambiguity)
            double ambiguity = calculateSimulatedAmbiguity(distance);

            PhotonTrackedTarget target = createSimulatedTarget(tagId, distance, ambiguity);
            targetsUsed.add(target);
        }

        // Update visible tag poses for visualization
        visibleTagPoses = visibleTags2d.toArray(new Pose2d[0]);

        // Log what we're seeing
        SmartDashboard.putNumber("SimVision/VisibleTagCount", visibleTags3d.size());

        // If we see at least one tag, create an estimated pose
        if (!targetsUsed.isEmpty()) {
            // =====================================================================══
            // VISION ESTIMATE = TRUE POSE
            // When tags are visible, report where the robot ACTUALLY is (truePose).
            // This is what real AprilTag localization does - it calculates from tag geometry.
            // The fused pose (which drifts like odometry) will be corrected toward this.
            // =====================================================================══

            // Add optional noise to simulated pose (realistic measurement noise)
            Pose3d estimatedPose3d = addNoiseTopose(new Pose3d(truePose), targetsUsed.get(0));

            EstimatedRobotPose estimate = new EstimatedRobotPose(
                estimatedPose3d,
                Timer.getFPGATimestamp(),
                targetsUsed,
                null // strategy not needed for simulation
            );

            estimates.add(estimate);

            // Track accepted tags (all visible tags in simulation are "accepted" at this stage)
            acceptedTagPoses = visibleTags3d.toArray(new Pose3d[0]);
            rejectedTagPoses = new Pose3d[0];

            // Log for debugging
            SmartDashboard.putNumber("SimVision/EstimatedX", estimatedPose3d.getX());
            SmartDashboard.putNumber("SimVision/EstimatedY", estimatedPose3d.getY());
            SmartDashboard.putNumber("SimVision/TrueX", truePose.getX());
            SmartDashboard.putNumber("SimVision/TrueY", truePose.getY());
            SmartDashboard.putNumber("SimVision/Ambiguity", targetsUsed.get(0).getPoseAmbiguity());
            Logger.recordOutput("SimVision/TruePose", truePose);
        } else {
            // No tags visible - no vision estimate to provide
            acceptedTagPoses = new Pose3d[0];
            rejectedTagPoses = new Pose3d[0];
        }

        return estimates;
    }

    /**
     * Calculates simulated ambiguity based on distance.
     * Closer tags have lower ambiguity (more reliable).
     *
     * @param distanceMeters Distance to tag in meters
     * @return Ambiguity value (0.0 to 1.0)
     */
    private double calculateSimulatedAmbiguity(double distanceMeters) {
        // Ambiguity increases with distance
        // At 1m: ~0.05 (very good)
        // At 2m: ~0.10 (good)
        // At 3m: ~0.20 (borderline)
        // At 4m+: ~0.30+ (unreliable)
        double baseAmbiguity = 0.03;
        double distanceScale = 0.07; // Ambiguity increase per meter

        double ambiguity = baseAmbiguity + (distanceMeters * distanceScale);

        // Add small random variation
        if (ADD_NOISE) {
            ambiguity += (Math.random() - 0.5) * 0.05;
        }

        return Math.max(0.01, Math.min(1.0, ambiguity)); // Clamp to valid range
    }

    /**
     * Creates a simulated PhotonTrackedTarget.
     */
    private PhotonTrackedTarget createSimulatedTarget(int tagId, double distance, double ambiguity) {
        // Create transform from camera to target
        Transform3d cameraToTarget = new Transform3d(
            new Translation3d(distance, 0, 0),
            new Rotation3d()
        );

        // PhotonTrackedTarget requires non-null corner lists
        List<org.photonvision.targeting.TargetCorner> emptyCorners = new ArrayList<>();

        return new PhotonTrackedTarget(
            0.0, // yaw
            0.0, // pitch
            0.0, // area
            0.0, // skew
            tagId,
            -1, // detected object class id (not used)
            -1f, // detection confidence (not used)
            cameraToTarget, // best camera to target
            cameraToTarget, // alt camera to target
            ambiguity,
            emptyCorners, // minAreaRectCorners
            emptyCorners  // detectedCorners
        );
    }

    /**
     * Adds realistic noise to the estimated pose.
     */
    private Pose3d addNoiseTopose(Pose3d truePose, PhotonTrackedTarget target) {
        if (!ADD_NOISE) {
            return truePose;
        }

        // Noise scales with distance (farther = more noise)
        double distance = target.getBestCameraToTarget().getTranslation().getNorm();
        double noiseScale = NOISE_STDDEV_METERS * (1.0 + distance * 0.5);

        double noiseX = (Math.random() - 0.5) * 2 * noiseScale;
        double noiseY = (Math.random() - 0.5) * 2 * noiseScale;
        double noiseTheta = (Math.random() - 0.5) * Math.toRadians(2.0); // Up to 2 degree noise

        return new Pose3d(
            truePose.getX() + noiseX,
            truePose.getY() + noiseY,
            truePose.getZ(),
            new Rotation3d(
                truePose.getRotation().getX(),
                truePose.getRotation().getY(),
                truePose.getRotation().getZ() + noiseTheta
            )
        );
    }

    @Override
    public Matrix<N3, N1> getEstimationStdDevs(EstimatedRobotPose estimatedPose) {
        var targets = estimatedPose.targetsUsed;
        int numTags = targets.size();

        // Calculate average distance to targets
        double avgDist = 0;
        for (PhotonTrackedTarget tgt : targets) {
            avgDist += tgt.getBestCameraToTarget().getTranslation().getNorm();
        }
        if (numTags > 0) avgDist /= numTags;

        // Multi-tag: high trust
        if (numTags >= 2) {
            double confidenceScale = 1.0 + (avgDist / 4.0);
            return VecBuilder.fill(
                VisionConstants.kMultiTagHighTrustStdDevXY * confidenceScale,
                VisionConstants.kMultiTagHighTrustStdDevXY * confidenceScale,
                Math.toRadians(VisionConstants.kMultiTagHighTrustStdDevThetaDegrees)
            );
        }

        // Single tag: distance-based trust
        if (avgDist > VisionConstants.kMaxVisionDistanceMeters) {
            return VecBuilder.fill(1000, 1000, 1000);
        }

        double distSq = avgDist * avgDist;
        double confidenceScale = 1 + (distSq / VisionConstants.kVisionTrustScaleDenominator);

        return VecBuilder.fill(
            VisionConstants.kSingleTagBaseTrustStdDevXY * confidenceScale,
            VisionConstants.kSingleTagBaseTrustStdDevXY * confidenceScale,
            Math.toRadians(VisionConstants.kSingleTagBaseTrustStdDevThetaDegrees) * confidenceScale
        );
    }

    @Override
    public boolean isVisionHealthy() {
        return visionHealthy;
    }

    @Override
    public Pose2d[] getVisibleTagPoses() {
        return visibleTagPoses;
    }

    @Override
    public Pose3d[] getAcceptedTagPoses() {
        return acceptedTagPoses;
    }

    @Override
    public Pose3d[] getRejectedTagPoses() {
        return rejectedTagPoses;
    }
}
