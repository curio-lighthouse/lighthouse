package com.lighthouse.Data;

/**
 * Object used to represent a point on the LidarDisplay canvas.
 */
public class GraphPoint extends DataPoint {

    /**
     * X axis coordinate on the canvas.
     */
    private float xCoordinate;

    /**
     * Y axis coordinate on the canvas.
     */
    private float yCoordinate;

    /**
     * Constructor.
     * @param dataPoint DataPoint object containing data from the LIDAR device.
     */
    public GraphPoint(DataPoint dataPoint) {
        super(dataPoint.getDistance(), dataPoint.getIntensity(), dataPoint.getAngle(), dataPoint.getRPM());
        calculateXYCoordinatesFromAngleAndDistance();
    }

    /**
     * Calculates the coordinates based off of the unit circle.
     */
    private void calculateXYCoordinatesFromAngleAndDistance() {
        xCoordinate = (float) (distance * Math.cos(Math.toRadians(angle)));
        yCoordinate = (float) (distance * Math.sin(Math.toRadians(angle)));
    }

    /**
     * Returns the x coordinate value.
     * @return x coordinate value
     */
    public float getxCoordinate() {
        return xCoordinate;
    }

    /**
     * Returns the y coordinate value.
     * @return y coordinate value
     */
    public float getyCoordinate() {
        return yCoordinate;
    }
}
