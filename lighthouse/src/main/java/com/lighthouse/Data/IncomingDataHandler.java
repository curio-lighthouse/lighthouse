package com.lighthouse.Data;

import android.os.Build;
import android.util.Log;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.RequiresApi;

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
     * @return DataPoint array containing the parsed LIDAR data.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static DataPoint[] getDataPointArrayFromPiData(byte[] lidarData,
                                                          int minimumDistanceFilter,
                                                          int maximumDistanceFilter,
                                                          int intensityThreshold,
                                                          int rpmThreshold) {
        HashMap<Integer, DataPoint> dataPointMap = new HashMap<>();
        int baseAngle, RPM;
        float intensity;
        boolean intensityMeetsThreshold, rpmMeetsThreshold;
        float[] distanceArray;
        byte[] tempArray = new byte[42];
        for (int i = 0; i < lidarData.length / 42; i++) {
            System.arraycopy(lidarData, i * 42, tempArray, 0, 42);
            if (Byte.toUnsignedInt(tempArray[1]) >= 160 && Byte.toUnsignedInt(tempArray[1]) <= 219) {
                baseAngle = getBaseAngle(tempArray[1]);
                distanceArray = getSixDistancesFromByteArray(
                        tempArray,
                        minimumDistanceFilter,
                        maximumDistanceFilter);
                distanceArray = applyStandardDeviationFilterToDistanceArray(distanceArray);

                intensity = (tempArray[5] * 256) + (tempArray[4]);
                intensityMeetsThreshold = isIntensityValueAboveThreshold(intensity, intensityThreshold);

                RPM = (tempArray[3] * 256) + (tempArray[2]);
                rpmMeetsThreshold = RPM > rpmThreshold;

                for (int x = 0; x < 6; x++) {
                    Log.i("lighthouse", "Processing angle: " + (baseAngle + x));
                    if (intensityMeetsThreshold && rpmMeetsThreshold) {
                        dataPointMap.put(baseAngle + x, new DataPoint((int) distanceArray[x], intensity, baseAngle + x, RPM));
                    } else {
                        dataPointMap.put(baseAngle + x, new DataPoint(0, intensity, baseAngle + x, RPM));
                    }
                }
            }
        }
        // TODO: Don't use magic numbers.
        DataPoint[] dataPointArray = new DataPoint[360];
        for (int i=0; i < 360; i++) {
            if (dataPointMap.containsKey(i)) {
                dataPointArray[i] = dataPointMap.get(i);
            } else {
                dataPointArray[i] = new DataPoint(0, 0, 0 , 0);
            }
        }
        Log.i("lighthouse", "Processed " + dataPointMap.size() + " angles");
        return dataPointArray;
    }

    /**
     * Returns the base angle for the reading.
     * @param baseAngleByte Byte representing the base angle for the reading.
     * @return The base angle after bit shifting.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static int getBaseAngle(byte baseAngleByte) {
        return ((Byte.toUnsignedInt(baseAngleByte) - 160) * 6);
    }

    /**
     * Returns the six distances captured in the byte array.
     * @param byteArray Byte array containing a single reading.
     * @param minimumDistanceFilter Minimum distance filter value.
     * @param maximumDistanceFilter Maximum distance filter value.
     * @return Float array containing all six angles.
     */
    private static float[] getSixDistancesFromByteArray(byte[] byteArray,
                                                        int minimumDistanceFilter,
                                                        int maximumDistanceFilter) {
        int distance, filteredDistanceValue;
        float[] distanceArray = new float[6];
        for (int x = 0; x < 6; x++) {
            distance = (((byteArray[((6 * (x + 1)) + 1)] * 256)) + (byteArray[(6 * (x + 1))]));
            filteredDistanceValue = applyDistanceFiltersToDistanceValue(
                    distance,
                    minimumDistanceFilter,
                    maximumDistanceFilter);
            distanceArray[x] = filteredDistanceValue;
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

    private static float[] applyStandardDeviationFilterToDistanceArray(float[] distanceArray) {
        float[] resultArray = new float[6];
        StandardDeviation standardDeviation = new StandardDeviation();
        Mean mean = new Mean();
        for (int i=0; i < distanceArray.length; i++) {
            standardDeviation.increment(distanceArray[i]);
            mean.increment(distanceArray[i]);
        }

        double standardDeviationResult = standardDeviation.getResult();
        double meanResult = mean.getResult();


        for (int i=0; i < distanceArray.length; i++) {
            if (distanceArray[i] > (meanResult + standardDeviationResult) || distanceArray[i] < (meanResult - standardDeviationResult)) {
                resultArray[i] = 0;
            } else {
                resultArray[i] = distanceArray[i];
            }
        }
        return resultArray;

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
