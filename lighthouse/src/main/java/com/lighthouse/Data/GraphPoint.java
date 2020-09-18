package com.lighthouse.Data;

public class GraphPoint extends DataPoint {

    private float xCoordinate;
    private float yCoordinate;

    public GraphPoint(DataPoint dataPoint) {
        super(dataPoint.getDistance(), dataPoint.getIntensity(), dataPoint.getAngle(), dataPoint.getRPM());
        calculateXYCoordinatesFromAngleAndDistance();
    }

    private void calculateXYCoordinatesFromAngleAndDistance() {
        xCoordinate = (float) (distance * Math.cos(Math.toRadians(angle)));
        yCoordinate = (float) (distance * Math.sin(Math.toRadians(angle)));
    }

    public float getxCoordinate() {
        return xCoordinate;
    }

    public float getyCoordinate() {
        return yCoordinate;
    }
}
