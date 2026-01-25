package frc.robot.subsystems;

import java.util.List;

import org.photonvision.EstimatedRobotPose;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;

/**
 * Interface for vision-based pose estimation systems.
 *
 * <p>This abstraction allows DriveSubsystem to work with different vision implementations
 * (PhotonVision, Limelight, mock vision for testing, or no vision at all).
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link VisionSubsystem} - Real PhotonVision cameras with AprilTags
 *   <li>{@link NoVisionProvider} - Safe fallback that returns no measurements
 * </ul>
 *
 * <p><b>Game-Agnostic</b> - This interface can be reused year-to-year regardless of field layout.
 */
public interface VisionProvider {

    /**
     * Returns true if vision is working and can be trusted.
     *
     * <p>Use this to check if vision-dependent commands should run.
     * If this returns false, commands should either:
     * <ul>
     *   <li>Disable themselves gracefully</li>
     *   <li>Use fallback behavior (e.g., driver manual control)</li>
     *   <li>Show a warning on the dashboard</li>
     * </ul>
     *
     * @return true if vision is healthy, false if disabled/broken/stale
     */
    default boolean isVisionHealthy() {
        return false; // Default to unhealthy - implementations must override
    }

    /**
     * Get estimated robot poses from vision system based on current reference pose.
     *
     * <p>The reference pose helps the vision system resolve ambiguous detections by choosing
     * the solution closest to where we think the robot currently is.
     *
     * @param referencePose The current best estimate of robot position (from odometry/pose estimator)
     * @return List of estimated poses from all cameras. Returns empty list if no targets seen or
     *         vision is disabled. Never returns null.
     */
    List<EstimatedRobotPose> getEstimatedGlobalPoses(Pose2d referencePose);

    /**
     * Calculate standard deviations (trust/uncertainty) for a vision measurement.
     *
     * <p>Lower values = higher trust. Standard deviations are used by the pose estimator
     * to weight how much to trust this measurement vs. encoder/gyro data.
     *
     * <p>Typical factors affecting trust:
     * <ul>
     *   <li>Number of tags seen (multi-tag = higher trust)
     *   <li>Distance to tags (closer = higher trust)
     *   <li>Ambiguity of detection (lower ambiguity = higher trust)
     * </ul>
     *
     * @param estimatedPose The pose estimate to calculate standard deviations for
     * @return Matrix[3x1] of standard deviations [x_stddev, y_stddev, theta_stddev]
     *         in meters and radians
     */
    Matrix<N3, N1> getEstimationStdDevs(EstimatedRobotPose estimatedPose);

    /**
     * Get the field positions of currently visible AprilTags.
     *
     * <p>Returns the known field positions (from the field layout) of tags that
     * are currently being detected by any camera. Useful for visualization in
     * AdvantageScope to verify which tags the robot is seeing.
     *
     * @return Array of Pose2d for each visible tag's field position.
     *         Returns empty array if no tags visible.
     */
    default Pose2d[] getVisibleTagPoses() {
        return new Pose2d[0];
    }
}
