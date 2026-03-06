package frc.robot.subsystems;

import java.util.Collections;
import java.util.List;

import org.photonvision.EstimatedRobotPose;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.wpilibj.DriverStation;
import frc.robot.constants.DriveConstants;

/**
 * Null Object implementation of VisionProvider.
 *
 * <p>This provider returns no vision measurements, allowing the robot to operate using
 * only encoders and gyroscope for pose estimation.
 *
 * <p><b>Use cases:</b>
 * <ul>
 *   <li>Camera hardware is not available or disconnected
 *   <li>Testing drive code without vision dependencies
 *   <li>Practice robots without cameras
 *   <li>Fallback during camera/coprocessor brownouts
 * </ul>
 *
 * <p><b>Game-Agnostic</b> - Can be reused year-to-year.
 *
 * <p><b>Thread-safe</b> - All methods are stateless and safe to call from multiple threads.
 */
public class NoVisionProvider implements VisionProvider {
    private boolean hasLoggedWarning = false;

    /**
     * Creates a NoVisionProvider that returns no measurements.
     *
     * <p>Logs a warning once to inform that vision is disabled.
     */
    public NoVisionProvider() {
        DriverStation.reportWarning(
            "NoVisionProvider active - robot will use odometry only (no vision corrections)",
            false
        );
        hasLoggedWarning = true;
    }

    /**
     * Returns an empty list - no vision measurements available.
     *
     * @param referencePose Ignored (not used when no vision is present)
     * @return Empty list (never null)
     */
    @Override
    public List<EstimatedRobotPose> getEstimatedGlobalPoses(Pose2d referencePose) {
        return Collections.emptyList();
    }

    /**
     * Returns maximum uncertainty standard deviations.
     *
     * <p>Since there's no vision measurement to evaluate, this returns very high standard
     * deviations (effectively infinite uncertainty). In practice, this method should never
     * be called since getEstimatedGlobalPoses() returns an empty list.
     *
     * @param estimatedPose Ignored (no vision measurements exist)
     * @return Very high standard deviations (no trust) from DriveConstants.kVisionNoTrustStdDev
     */
    @Override
    public Matrix<N3, N1> getEstimationStdDevs(EstimatedRobotPose estimatedPose) {
        // Return very high standard deviations (no trust)
        // This method should not be called in practice since we return no poses
        return VecBuilder.fill(
            DriveConstants.kVisionNoTrustStdDev,
            DriveConstants.kVisionNoTrustStdDev,
            DriveConstants.kVisionNoTrustStdDev
        );
    }
}
