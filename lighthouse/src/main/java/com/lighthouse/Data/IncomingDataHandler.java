package com.lighthouse.Data;


public class IncomingDataHandler {

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

    private static int getBaseAngle(int baseAngleByte) {
        return ((baseAngleByte - 160) * 6) & 0xff;
    }

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

    private static int applyDistanceFiltersToDistanceValue(int distance, int minimumDistanceFilter, int maximumDistanceFilter) {
        if (distance > minimumDistanceFilter && distance < maximumDistanceFilter) {
            return distance;
        } else {
            return 0;
        }
    }

    private static float applyScaleRateToDistanceValue(int distance, float lidarViewScaleRate) {
        return distance / lidarViewScaleRate;
    }

    private static boolean isIntensityValueAboveThreshold(float intensity, int intensityThreshold) {
        return !(intensity < intensityThreshold);
    }
}
