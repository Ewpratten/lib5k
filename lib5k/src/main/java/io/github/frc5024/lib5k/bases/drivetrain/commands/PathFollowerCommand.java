package io.github.frc5024.lib5k.bases.drivetrain.commands;

import java.io.FileWriter;
import java.io.IOException;

import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.CommandBase;

import io.github.frc5024.common_drive.DriveTrainBase;
import io.github.frc5024.common_drive.types.ChassisSide;
import io.github.frc5024.lib5k.bases.drivetrain.AbstractDriveTrain;
import io.github.frc5024.lib5k.bases.drivetrain.Chassis;
import io.github.frc5024.lib5k.hardware.ni.roborio.fpga.FPGAClock;
import io.github.frc5024.lib5k.logging.RobotLogger;
import io.github.frc5024.lib5k.logging.RobotLogger.Level;
import io.github.frc5024.lib5k.utils.FileManagement;
import io.github.frc5024.lib5k.utils.RobotMath;
import io.github.frc5024.purepursuit.pathgen.Path;
import io.github.frc5024.purepursuit.Follower;

/**
 * PathFollowCommand is a {@link CommandBase} object generated by
 * {@link DriveTrainBase}, and can be used to follow paths with the drivetrain.
 * In the background, it also logs the robot's current position vs the goal
 * position for match analysis.
 */
public class PathFollowerCommand extends CommandBase {

    // Logger
    private RobotLogger logger = RobotLogger.getInstance();

    // Path follower
    private Follower follower;

    // DriveTrain
    private AbstractDriveTrain driveTrain;
    private Chassis.Side frontSide = Chassis.Side.kFront;

    // Epsilon
    private double epsRadius;

    // Logfile
    private FileWriter logFile;
    private double initTime;

    // Max speed
    private double maxSpeed = 1.0;

    /**
     * Create a PathFollowCommand. This should not be called from user code. To
     * create one of these, call DriveTrainBase.createPathingCommand() instead.
     * 
     * @param driveTrain DriveTrain to control
     * @param path       Path to follow
     * @param epsRadius  Radius around the final pose for trigger isFinished()
     */
    public PathFollowerCommand(AbstractDriveTrain driveTrain, Path path, double epsRadius) {

        // Some WPILib magic
        addRequirements(driveTrain);

        // Configure the follower
        follower = new Follower(path, 0.2, 0.1, driveTrain.getWidthMeters());
        this.epsRadius = epsRadius;

        // Store the drivetrain
        this.driveTrain = driveTrain;

    }

    /**
     * Configure path following side
     * 
     * @param frontSide Which side of the robot should be the "front"
     * @return This Object
     */
    public PathFollowerCommand setFrontSide(Chassis.Side frontSide) {
        this.frontSide = frontSide;
        return this;
    }

    /**
     * A builder-style method for setting the lookahead. Lookahead is the radius in
     * front of the robot that is searched for a new point in the path. The bigger
     * this is, the more shortcuts will be taken while path following.
     * 
     * @param lookaheadMeters How far to look ahead for new goal poses
     * @return This Object
     */
    public PathFollowerCommand withLookahead(double lookaheadMeters) {
        this.follower.setLookaheadDistance(lookaheadMeters);
        return this;
    }

    /**
     * A builder-style method for setting the max speed
     * 
     * @param speedPercent Percent speed to cap at
     * @return This Object
     */
    public PathFollowerCommand withMaxSpeed(double speedPercent) {
        this.maxSpeed = speedPercent;
        return this;
    }

    @Override
    public void initialize() {
        // Reset the follower
        follower.reset();
        logger.log("Reset path follower");

        // Attempt to open a log file
        try {
            logger.log("Opening a CSV logfile to save path progress to");
            this.logFile = FileManagement
                    .createFileWriter(String.format("PathFollowCommand_%.2f.csv", FPGAClock.getFPGASeconds()));

            // Write file header
            this.logFile.append("Timestamp (seconds), Robot X, Robot Y, Robot Theta, Goal X, Goal Y\n");
        } catch (IOException e) {
            logger.log("Failed to open CSV logfile. Not going to log data");
        }

        // Set the init timestamp
        initTime = FPGAClock.getFPGASeconds();

        // Set the drivetrain side
        driveTrain.setFrontSide(frontSide);

        // Set the speed cap
        driveTrain.setMaxSpeedPercent(maxSpeed);
    }

    @Override
    public void execute() {

        // Get the robot's current position
        Pose2d currentPose = driveTrain.getPose();

        // Get the next goal pose
        Translation2d goalPose = follower.getNextPoint(currentPose);

        // Drive to that pose
        // Using a fake epsilon here because we override the check in isFinished.
        driveTrain.setGoalPose(goalPose, new Translation2d(0.01, 0.01));

        // Try to write to the logfile
        if (logFile != null) {

            // Get the current timestamp
            double curTime = FPGAClock.getFPGASeconds();
            double dt = curTime - initTime;

            // Write line to the logfile
            try {
                logFile.append(String.format("%.2f, %.2f, %.2f, %.2f, %.2f, %.2f%n", dt,
                        currentPose.getTranslation().getX(), currentPose.getTranslation().getY(),
                        currentPose.getRotation().getDegrees(), goalPose.getX(), goalPose.getY()));
            } catch (IOException e) {
                logger.log("Failed to write line to logFile");
            }
        }

    }

    @Override
    public void end(boolean interrupted) {

        if (interrupted) {
            logger.log("Path following was interrupted.");
        } else {
            logger.log("Robot successfully reached goal pose: %s", follower.getFinalPose());
        }

        // Stop the robot
        driveTrain.reset();

        // Save the logfile
        if (logFile != null) {
            logger.log("Saving CSV logfile");
            try {
                logFile.flush();
                logFile.close();
            } catch (IOException e) {
                logger.log("Failed to close logFile", Level.kWarning);
            }
            logFile = null;
        }

    }

    @Override
    public boolean isFinished() {

        // Get the robot's position
        Translation2d robotPosition = driveTrain.getPose().getTranslation();

        // Get the goal position
        Translation2d goalPosition = follower.getFinalPose();

        // We override the built-in finished checker in the drivetrain to make sure we
        // are at the FINAL pose. not an arbitrary goal along the way.
        return RobotMath.epsilonEquals(robotPosition, goalPosition, new Translation2d(epsRadius, epsRadius));
    }

}