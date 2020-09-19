package com.lighthouse.Data;


/**
 * Used for handing the incoming data from the LIDAR device bluetooth input stream.
 */
public class IncomingDataHandler {

    /**
     * Static method for processing the raw byte stream from the LIDAR device.
     * @param lidarData Raw byte array from the LIDAR device bluetooth input stream.
     * @param minimumDistanceFilter Minimum distance filter value.
     * @param maximumDistanceFilter Maximum distance filter value.
     * @param intensityThreshold Intensity threshold value.
     * @param lidarViewScaleRate LidarDisplay view scale rate.
     * @return DataPoint array containing the parsed LIDAR data.
     */
    public static DataPoint[] getDataPointArrayFromPiData(byte[] lidarData,
                                                          int minimumDistanceFilter,
                                                          int maximumDistanceFilter,
                                                          int intensityThreshold,
                                                          float lidarViewScaleRate) {
        int dataPointArraySize = lidarData.length / 42 * 6;
        DataPoint[] dataPointArray = new DataPoint[dataPointArraySize];
        int baseAngle, RPM;
        float intensity;
        boolean intensityMeetsThreshold;
        float[] distanceArray;
        byte[] tempArray = new byte[42];
        for (int i = 0; i < lidarData.length / 42; i++) {
            System.arraycopy(lidarData, i * 42, tempArray, 0, 42);
            baseAngle = getBaseAngle(tempArray[1]);
            distanceArray = getSixDistancesFromByteArray(
                    tempArray,
                    minimumDistanceFilter,
                    maximumDistanceFilter,
                    lidarViewScaleRate);

            intensity = (tempArray[5] * 256) + (tempArray[4]);
            intensityMeetsThreshold = isIntensityValueAboveThreshold(intensity, intensityThreshold);

            RPM = (tempArray[3] * 256) + (tempArray[2]);

            for (int x = 0; x < 6; x++) {
                if (intensityMeetsThreshold) {
                    dataPointArray[x + (i * 5)] = new DataPoint((int) distanceArray[x], intensity, baseAngle + x, RPM);
                }
                else {
                    dataPointArray[x + (i * 5)] = new DataPoint(0, intensity, baseAngle + x, RPM);
                }
            }
        }

        return dataPointArray;
    }

    /**
     * Returns the base angle for the reading.
     * @param baseAngleByte Byte representing the base angle for the reading.
     * @return The base angle after bit shifting.
     */
    private static int getBaseAngle(int baseAngleByte) {
        return ((baseAngleByte - 160) * 6) & 0xff;
    }

    /**
     * Returns the six distances captured in the byte array.
     * @param byteArray Byte array containing a single reading.
     * @param minimumDistanceFilter Minimum distance filter value.
     * @param maximumDistanceFilter Maximum distance filter value.
     * @param lidarViewScaleRate The LidarDisplay view scale value.
     * @return Float array containing all six angles.
     */
    private static float[] getSixDistancesFromByteArray(byte[] byteArray,
                                                        int minimumDistanceFilter,
                                                        int maximumDistanceFilter,
                                                        float lidarViewScaleRate) {
        int distance, filteredDistanceValue;
        float[] distanceArray = new float[6];
        for (int x = 0; x < 6; x++) {
            distance = (((byteArray[((6 * (x + 1)) + 1)] * 256)) + (byteArray[(6 * (x + 1))]));
            filteredDistanceValue = applyDistanceFiltersToDistanceValue(
                    distance,
                    minimumDistanceFilter,
                    maximumDistanceFilter);

            distanceArray[x] = applyScaleRateToDistanceValue(filteredDistanceValue, lidarViewScaleRate);
        }
        return distanceArray;
    }

    /**
     * Applies the maximum and minimum distance filters to the distance values.  If a distance value
     * is beyond either filter, it is set to a distance of 0.
     * @param distance The distance being filtered.
     * @param minimumDistanceFilter Minimum distance filter value.
     * @param maximumDistanceFilter Maximum distance filter value.
     * @return Filtered distance value.
     */
    private static int applyDistanceFiltersToDistanceValue(int distance, int minimumDistanceFilter, int maximumDistanceFilter) {
        if (distance > minimumDistanceFilter && distance < maximumDistanceFilter) {
            return distance;
        } else {
            return 0;
        }
    }

    /**
     * Applies the scale rate to the distance value.
     * @param distance Distance value to be scaled.
     * @param lidarViewScaleRate Scale rate.
     * @return Scaled distance value.
     */
    private static float applyScaleRateToDistanceValue(int distance, float lidarViewScaleRate) {
        return distance / lidarViewScaleRate;
    }

    /**
     * Returns true if the intensity value for a reading is beyond the threshold value.
     * @param intensity Intensity value for a reading.
     * @param intensityThreshold Intensity value threshold.
     * @return
     */
    private static boolean isIntensityValueAboveThreshold(float intensity, int intensityThreshold) {
        return !(intensity < intensityThreshold);
    }
}
