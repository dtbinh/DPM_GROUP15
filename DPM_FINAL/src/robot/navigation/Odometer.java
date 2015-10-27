/*
 * File: Odometer.java
 * Written by: Sean Lawlor
 * ECSE 211 - Design Principles and Methods, Head TA
 * Fall 2011
 * Ported to EV3 by: Francois Ouellet Delorme
 * Fall 2015
 * 
 * Class which controls the odometer for the robot
 * 
 * Odometer defines cooridinate system as such...
 * 
 * 					90Deg:pos y-axis
 * 							|
 * 							|
 * 							|
 * 							|
 * 180Deg:neg x-axis------------------0Deg:pos x-axis
 * 							|
 * 							|
 * 							|
 * 							|
 * 					270Deg:neg y-axis
 * 
 * The odometer is initalized to 90 degrees, assuming the robot is facing up the positive y-axis
 * 
 */
package robot.navigation;
import lejos.utility.Timer;
import lejos.utility.TimerListener;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import robot.constants.Constants;
import robot.constants.Position;

/**
 * The odometer class serves to report the location of our robot at any given time, it will run as its own thread
 */
public class Odometer implements TimerListener {

	private Timer timer;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	private EV3MediumRegulatedMotor armMotor;
	private final int DEFAULT_TIMEOUT_PERIOD = 20;
	private double leftRadius, rightRadius, TRACK;
	private double x, y, theta;
	private double[] oldDH, dDH;

	//This is a singleton class
	private static Odometer instance = new Odometer(true);
	public static synchronized Odometer getInstance(){
		return instance;
	}

	// constructor
	private Odometer (boolean autostart) {
		Motors mtrs = Motors.getInstance();
		
		this.leftMotor = mtrs.getLeftMotor();
		this.rightMotor = mtrs.getRightMotor();
		this.armMotor = mtrs.getArmMotor();
		
		// default values, modify for your robot
		this.rightRadius = Constants.WHEEL_RADIUS;
		this.leftRadius = Constants.WHEEL_RADIUS;
		this.TRACK = Constants.TRACK;
		
		this.x = 0.0;
		this.y = 0.0;
		this.theta = 90.0;
		this.oldDH = new double[2];
		this.dDH = new double[2];
		
		if (autostart) {
			int interval = Constants.ODOMETER_UPDATE_INTERVAL;
			// if the timeout interval is given as <= 0, default to 20ms timeout 
			this.timer = new Timer((interval <= 0) ? interval : DEFAULT_TIMEOUT_PERIOD, this);
			this.timer.start();
		} else
			this.timer = null;
	}
	
	// functions to start/stop the timerlistener
	public void stop() {
		if (this.timer != null)
			this.timer.stop();
	}
	public void start() {
		if (this.timer != null)
			this.timer.start();
	}
	
	/*
	 * Calculates displacement and heading as title suggests
	 */
	private void getDisplacementAndHeading(double[] data) {
		int leftTacho, rightTacho;
		leftTacho = leftMotor.getTachoCount();
		rightTacho = rightMotor.getTachoCount();

		data[0] = (leftTacho * leftRadius + rightTacho * rightRadius) * Math.PI / 360.0;
		data[1] = (rightTacho * rightRadius - leftTacho * leftRadius) / TRACK;
	}
	
	/*
	 * Recompute the odometer values using the displacement and heading changes
	 */
	public void timedOut() {
		this.getDisplacementAndHeading(dDH);
		dDH[0] -= oldDH[0];
		dDH[1] -= oldDH[1];

		// update the position in a critical region
		synchronized (this) {
			theta += dDH[1];
			theta = fixDegAngle(theta);

			x += dDH[0] * Math.cos(Math.toRadians(theta));
			y += dDH[0] * Math.sin(Math.toRadians(theta));
		}

		oldDH[0] += dDH[0];
		oldDH[1] += dDH[1];
	}

	// return X value
	public double getX() {
		synchronized (this) {
			return x;
		}
	}

	// return Y value
	public double getY() {
		synchronized (this) {
			return y;
		}
	}

	// return theta value
	public double getAng() {
		synchronized (this) {
			return theta;
		}
	}
	
	// set x,y,theta------- WHY U DO THIS ??
	public void setPosition(double[] position, boolean[] update) {
		synchronized (this) {
			if (update[0])
				x = position[0];
			if (update[1])
				y = position[1];
			if (update[2])
				theta = position[2];
		}
	}
	// UPDATED.. INDIVIDUAL SETTERS
	public void setX(final double x) {
		synchronized (this) {
			this.x = x;
		}
	}
	public void setY(final double y) {
		synchronized (this) {
			this.y = y;
		}
	}

	public synchronized Position getPosition(){
		return new Position(x,y,theta);
	}

	public double[] getArrayPosition() {
		synchronized (this) {
			return new double[] { x, y, theta };
		}
	}
	
	// accessors to motors
	public EV3LargeRegulatedMotor [] getMotors() {
		return new EV3LargeRegulatedMotor[] {this.leftMotor, this.rightMotor};
	}
	public EV3MediumRegulatedMotor getArm() {
		return this.armMotor;
	}
	public EV3LargeRegulatedMotor getLeftMotor() {
		return this.leftMotor;
	}
	public EV3LargeRegulatedMotor getRightMotor() {
		return this.rightMotor;
	}
	
	// This method adds the USlocalizer's deltatheta to the current odometer theta to correct it
	public void correctTheta(final double theta) 
	{
		synchronized (this) {
			this.theta += theta;
			
			while(this.theta < 0)   this.theta += 360;								// ensures WRAPROUND (angle does not go out of bounds)
			while(this.theta > 360) this.theta -= 360;
		}
	}

	// static 'helper' methods
	public static double fixDegAngle(double angle) {
		if (angle < 0.0)
			angle = 360.0 + (angle % 360.0);

		return angle % 360.0;
	}

	public static double minimumAngleFromTo(double a, double b) {
		double d = fixDegAngle(b - a);

		if (d < 180.0)
			return d;
		else
			return d - 360.0;
	}

}
