package com.lighthouse.Data;

public class DataPoint {

    protected final float distance;
    protected final float intensity;
    protected final int angle;
    protected final int RPM;

    public DataPoint(float distance, float intensity, int angle, int RPM) {
        this.distance = distance;
        this.intensity = intensity;
        this.angle = angle;
        this.RPM = RPM;
    }

    public float getDistance() {
        return distance;
    }

    public float getIntensity() {
        return intensity;
    }

    public int getAngle() {
        return angle;
    }

    public int getRPM() {return RPM;}

}
