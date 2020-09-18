package com.lighthouse.Data;

/**
 * Represents a point of data from the LIDAR device.
 */
public class DataPoint {

    /**
     * Distance value.
     */
    protected final float distance;

    /**
     * Intensity value.
     */
    protected final float intensity;

    /**
     * Angle for the reading.
     */
    protected final int angle;

    /**
     * RPM during the reading.
     */
    protected final int RPM;

    /**
     * Constructor.
     * @param distance Distance value.
     * @param intensity Intensity value.
     * @param angle Angle for the reading.
     * @param RPM RPM during the reading.
     */
    public DataPoint(float distance, float intensity, int angle, int RPM) {
        this.distance = distance;
        this.intensity = intensity;
        this.angle = angle;
        this.RPM = RPM;
    }

    /**
     * Returns the distance value.
     * @return distance value.
     */
    public float getDistance() {
        return distance;
    }

    /**
     * Returns the intensity value.
     * @return Intensity value.
     */
    public float getIntensity() {
        return intensity;
    }

    /**
     * Returns the angle for the reading.
     * @return Angle value.
     */
    public int getAngle() {
        return angle;
    }

    /**
     * Returns the RPM during the reading.
     * @return RPM during the reading.
     */
    public int getRPM() {return RPM;}

}
