package io.github.frc5024.lib5k.examples.autonomous_path_following;

import edu.wpi.first.wpilibj.util.Units;
import io.github.frc5024.lib5k.control_loops.ExtendedPIDController;

public class RobotConfig {

    // Drivetrain CAN ids
    public static final int DRIVETRAIN_FRONT_LEFT_ID = 1;
    public static final int DRIVETRAIN_REAR_LEFT_ID = 2;
    public static final int DRIVETRAIN_FRONT_RIGHT_ID = 3;
    public static final int DRIVETRAIN_REAR_RIGHT_ID = 4;

    // Drivetrain sensor TPR
    public static final int DRIVETRAIN_ENCODER_TPR = 1400;

    // Drivetrain control loops
    public static final ExtendedPIDController DRIVETRAIN_ROTATION_CONTROLLER = new ExtendedPIDController(0.0088, 0.01,
            0);
    public static final ExtendedPIDController DRIVETRAIN_DISTANCE_CONTROLLER = new ExtendedPIDController(0.478, 0,
            0.008);

    // Robot sizing
    public static final double ROBOT_WIDTH = Units.inchesToMeters(26.0);
    public static final double DRIVETRAIN_WHEEL_RADIUS = Units.inchesToMeters(6.0) / 2.0;
}