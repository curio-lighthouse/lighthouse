package com.lighthouse.Data;

import android.graphics.Color;
import android.graphics.Paint;

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
     * The scale rate which is used to scale the LiDAR distance values.
     */
    private final float lidarViewScaleRate;

    private final Paint customPaint = new Paint();

    private String hexColorValue = "#212121";

    /**
     * Constructor.
     * @param dataPoint DataPoint object containing data from the LiDAR device.
     * @param lidarViewScaleRate The scale rate which is used to scale the LiDAR distance values.
     */
    public GraphPoint(DataPoint dataPoint, float lidarViewScaleRate) {
        super(dataPoint.getDistance(), dataPoint.getIntensity(), dataPoint.getAngle(), dataPoint.getRPM());
        this.lidarViewScaleRate = lidarViewScaleRate;
        calculateXYCoordinatesFromAngleAndDistance();

        customPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        customPaint.setColor(Color.parseColor(hexColorValue));
//        // TODO: Allow the developer to configure this value
        customPaint.setStrokeWidth(3);
        customPaint.setAlpha(255);
    }

    /**
     * Calculates the coordinates based off of the unit circle.
     */
    private void calculateXYCoordinatesFromAngleAndDistance() {
        xCoordinate = (float) ((distance/lidarViewScaleRate) * Math.cos(Math.toRadians(angle)));
        yCoordinate = (float) ((distance/lidarViewScaleRate) * Math.sin(Math.toRadians(angle)));
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

    public Paint getCustomPaint() {
        return customPaint;
    }

    public void setPaintAlpha(int alpha) {
        customPaint.setAlpha(alpha);
    }

    public void setPaintColor(String hexColorValue) {
        customPaint.setColor(Color.parseColor("#FFFFFF"));
    }

}
