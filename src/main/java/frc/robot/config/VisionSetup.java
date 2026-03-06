// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.config;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotBase;
import frc.robot.constants.VisionConstants;
import frc.robot.subsystems.NoVisionProvider;
import frc.robot.subsystems.SimVisionProvider;
import frc.robot.subsystems.VisionProvider;
import frc.robot.subsystems.VisionSubsystem;

/**
 * =====================================================================══════
 *                              VISION SETUP
 * =====================================================================══════
 *
 * This file sets up the vision cameras and AprilTags.
 *
 * WHAT THIS FILE DOES:
 *   - Creates the vision system (cameras that see AprilTags)
 *   - Loads the field layout (where AprilTags are on the field)
 *   - Configures camera positions on the robot
 *
 * WHAT TO UPDATE EACH YEAR:
 *   - loadFieldLayout(): Change to current season's field
 *   - Camera transforms if camera positions change
 *
 * Related files:
 *   - VisionConstants: Camera positions and settings
 *   - VisionSubsystem: The actual vision processing code
 */
public class VisionSetup {

  // =========================================================================
  // MAIN SETUP METHOD
  // =========================================================================

  /**
   * Creates vision provider with automatic fallback if vision unavailable.
   *
   * <p>In simulation mode, this creates a SimVisionProvider that simulates
   * camera detection of AprilTags. The simulated camera can "see" tags
   * within 9 feet in front of the robot.
   *
   * <p>On a real robot, this creates the normal VisionSubsystem with PhotonVision.
   *
   * <p>If cameras aren't working, this returns a NoVisionProvider that does nothing.
   *
   * @return VisionSubsystem (real), SimVisionProvider (simulation), or NoVisionProvider (fallback)
   */
  public static VisionProvider createVisionProvider() {
    try {
      // Step 1: Load field layout for current game
      AprilTagFieldLayout fieldLayout = loadFieldLayout();

      if (fieldLayout == null) {
        DriverStation.reportWarning(
          "Field layout not loaded - using NoVisionProvider",
          false
        );
        return new NoVisionProvider();
      }

      // Step 2: Check if we're in simulation mode
      if (!RobotBase.isReal()) {
        DriverStation.reportWarning(
          "Simulation detected - using SimVisionProvider (9 ft range, 70° FOV)",
          false
        );
        return new SimVisionProvider(fieldLayout);
      }

      // TODO: VISION DISABLED FOR FIRST COMP - Re-enable for next competition
      // Vision cameras were not ready in time. All code remains in place.
      // To re-enable: Remove this block and uncomment the vision subsystem creation below.
      DriverStation.reportWarning(
        "Vision temporarily disabled - using NoVisionProvider",
        false
      );
      return new NoVisionProvider();

      // Step 3: Real robot - Create camera configurations for front and back cameras
      // Transform3d frontCameraTransform = createFrontCameraTransform();
      // Transform3d backCameraTransform = createBackCameraTransform();

      // Step 4: Create VisionSubsystem with BOTH cameras
      // return new VisionSubsystem(
      //   fieldLayout,
      //   new VisionSubsystem.CameraConfig(VisionConstants.kFrontCameraName, frontCameraTransform),
      //   new VisionSubsystem.CameraConfig(VisionConstants.kBackCameraName, backCameraTransform)
      // );

    } catch (Exception e) {
      DriverStation.reportError(
        "Failed to initialize vision: " + e.getMessage(),
        false
      );
      return new NoVisionProvider();
    }
  }

  // =========================================================================
  // FIELD LAYOUT
  // =========================================================================

  /**
   * Loads AprilTag field layout for current game.
   *
   * <p><b>UPDATE EACH YEAR:</b> Replace with current season's field layout.
   * <p>Available fields are defined in {@code AprilTagFields} enum.
   * <p>Examples: k2024Crescendo, k2025Reefscape, k2026RebuiltWelded, etc.
   *
   * @return Field layout or null if unavailable
   */
  private static AprilTagFieldLayout loadFieldLayout() {
    try {
      // 2026 REBUILT field layout
      return AprilTagFieldLayout.loadField(AprilTagFields.k2026RebuiltWelded);
    } catch (Exception e) {
      DriverStation.reportWarning(
        "Could not load AprilTag field layout: " + e.getMessage() +
        " - Vision will be disabled",
        false
      );
      return null;
    }
  }

  // =========================================================================
  // CAMERA TRANSFORMS
  // =========================================================================
  // These tell the robot where each camera is mounted.
  // Measurements are from robot center to camera.

  /**
   * Creates front camera-to-robot transform from VisionConstants.
   *
   * <p><b>UPDATE VisionConstants EACH YEAR</b> with measured camera position/orientation.
   *
   * @return Transform3d from robot center to front camera
   */
  public static Transform3d createFrontCameraTransform() {
    return new Transform3d(
      new Translation3d(
        VisionConstants.kFrontCameraToRobotX,
        VisionConstants.kFrontCameraToRobotY,
        VisionConstants.kFrontCameraToRobotZ
      ),
      new Rotation3d(
        VisionConstants.kFrontCameraRollRadians,
        VisionConstants.kFrontCameraPitchRadians,
        VisionConstants.kFrontCameraYawRadians
      )
    );
  }

  /**
   * Creates back camera-to-robot transform from VisionConstants.
   *
   * <p><b>UPDATE VisionConstants EACH YEAR</b> with measured camera position/orientation.
   *
   * @return Transform3d from robot center to back camera
   */
  public static Transform3d createBackCameraTransform() {
    return new Transform3d(
      new Translation3d(
        VisionConstants.kBackCameraToRobotX,
        VisionConstants.kBackCameraToRobotY,
        VisionConstants.kBackCameraToRobotZ
      ),
      new Rotation3d(
        VisionConstants.kBackCameraRollRadians,
        VisionConstants.kBackCameraPitchRadians,
        VisionConstants.kBackCameraYawRadians
      )
    );
  }
}
