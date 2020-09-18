package com.lighthouse;

import android.app.Activity;
import android.util.Log;


import com.lighthouse.Data.DataPoint;
import com.lighthouse.Data.IncomingDataHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class LIDAR extends Bluetooth {

    /**
     * This is the number of bytes that are contained in each message sent from the LIDAR
     * device to the android phone.  The LIDAR device captures a minimum of 6 angles in 42 bytes.
     */
    private static int bluetoothBytePacketSize = 1260;

    private final int MINIMUM_DISTANCE = 120;
    private final int MAXIMUM_DISTANCE = 3500;

    private final String START_COMMAND = "start";

    private int minimumDistanceFilter = MINIMUM_DISTANCE;
    private int maximumDistanceFilter = MAXIMUM_DISTANCE;

    int intensityThreshold = 0;

    private int pollingInterval = 50;

    private int lidarViewRefreshRate = 16;

    private float lidarViewScaleRate = 8f;

    private boolean outputLIDARDataToLog = false;

    private boolean writeLidarDataToFile = false;

    private URI fileURI;

    /**
     * Instance of the LIDAR View.
     */
    private LidarDisplay lidarDisplay;

    private byte[] myByteArray;


    /**
     * Set to true when we receive a packet of data from the lidar sensor.  This will make sure
     * that we don't invalidate the view if we haven't received new data.
     */
    private static boolean changed = false;


    public LIDAR(Activity activity) {
        super(activity);
    }

    public LIDAR(Activity activity, LidarDisplay lidarDisplay) {
        super(activity);
        this.lidarDisplay = lidarDisplay;
    }

    public boolean isWriteLidarDataToFile() {
        return writeLidarDataToFile;
    }

    public void setWriteLidarDataToFile(URI fileLocation) {
        this.writeLidarDataToFile = true;
        this.fileURI = fileLocation;
    }

    public URI getFileURI() {
        return fileURI;
    }

    public boolean isOutputLIDARDataToLog() {
        return outputLIDARDataToLog;
    }

    public void setOutputLIDARDataToLog(boolean outputLIDARDataToLog) {
        this.outputLIDARDataToLog = outputLIDARDataToLog;
    }

    public float getLidarViewScaleRate() {
        return lidarViewScaleRate;
    }

    public void setLidarViewScaleRate(float lidarViewScaleRate) {
        this.lidarViewScaleRate = lidarViewScaleRate;
    }

    public int getLidarViewRefreshRate() {
        return lidarViewRefreshRate;
    }

    public void setLidarViewRefreshRate(int lidarViewRefreshRate) {
        this.lidarViewRefreshRate = lidarViewRefreshRate;
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public int getIntensityThresholdFilter() {
        return intensityThreshold;
    }

    public void setIntensityThresholdFilter(int intensityThreshold) {
        this.intensityThreshold = intensityThreshold;
    }

    public int getMinimumDistanceFilter() {
        return minimumDistanceFilter;
    }

    public boolean setMinimumDistanceFilter(int minimumDistanceFilter) {
        // Check to make sure that the minimumDistanceFilter value is larger than the smallest
        // range for the LIDAR device.
        if (minimumDistanceFilter < MINIMUM_DISTANCE) {
            this.minimumDistanceFilter = minimumDistanceFilter;
            return true;
        }
        return false;
    }

    public int getMaximumDistanceFilter() {
        return maximumDistanceFilter;
    }

    public boolean setMaximumDistanceFilter(int maximumDistanceFilter) {
        // Check to make sure that the maximumDistanceFilter value is less than the maximum
        // range for the LIDAR device.
        if (maximumDistanceFilter < MAXIMUM_DISTANCE) {
            this.maximumDistanceFilter = maximumDistanceFilter;
            return true;
        }
        return false;
    }

    public int getBluetoothBytePacketSize() {
        return bluetoothBytePacketSize;
    }

    public boolean setBluetoothBytePacketSize(int bluetoothBytePacketSize) {
        // Check to make sure the bluetoothBytePacketSize is in increments of 42
        // since that is the byte stream size of each array of data from the LIDAR device.
        if (bluetoothBytePacketSize % 42 == 0) {
            LIDAR.bluetoothBytePacketSize = bluetoothBytePacketSize;
            return true;
        }
        return false;
    }

    public void stopLIDAR() {
        final String STOP_MESSAGE = "stop";
        byte[] msgBuffer = STOP_MESSAGE.getBytes();
        try {
            getOutStream().write(msgBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startLIDAR() {
        // Set the package size to the configured value in bluetoothBytePacketSize.
        myByteArray = new byte[bluetoothBytePacketSize];

        final String START_MESSAGE = START_COMMAND + bluetoothBytePacketSize;
        byte[] msgBuffer = START_MESSAGE.getBytes();
        try {
            getOutStream().write(msgBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Task()).start();

        Timer timerObj = new Timer();
        TimerTask timerTaskObj = new TimerTask() {
            public void run() {
                if (changed) {
                    lidarDisplay.postInvalidate();
                    Log.v("info", "invalidated display");
                    changed = false;
                }

            }
        };
        timerObj.schedule(timerTaskObj, 0, lidarViewRefreshRate);

    }

    public boolean connectToLIDAR() {

        boolean bluetoothIsOn = enableBluetoothOnPhone();

        if (bluetoothIsOn) {
            mBTSocket = connectToLIDARAndGetSocket();
        }
        if (mBTSocket != null && bluetoothIsOn) {
            return true;
        }
        return false;
    }

    private void outputDataPointArrayToLogs(DataPoint[] dataPointArray) {
        for (DataPoint dataPoint : dataPointArray) {
            Log.v("LIDAR", "Angle: " + dataPoint.getAngle() +
                    " Distance: " + dataPoint.getDistance() +
                    " Intensity: " + dataPoint.getIntensity() +
                    " RPM: " + dataPoint.getRPM());
        }
    }

    private class Task implements Runnable {

        @Override
        public void run() {
            int bytes;
            byte[] buffer;
            long lastReadTime = 0;
            DataPoint[] dataPointArray;

            // Continually loop and check for messages from the pi.
            while (true) {
                if (lastReadTime == 0 || (System.currentTimeMillis() - lastReadTime) > pollingInterval) {
                    try {
                        // Read the bytes that are coming from the pi.
                        bytes = getInStream().available();
                        lastReadTime = System.currentTimeMillis();

                        // If we received a message from the pi, we will continue with processing it.
                        if (bytes > 0) {
                            buffer = new byte[bytes];
                            getInStream().read(buffer);
                            System.arraycopy(buffer, 0, myByteArray, 0, Math.min(bytes, bluetoothBytePacketSize));
                            dataPointArray = IncomingDataHandler.getDataPointArrayFromPiData(myByteArray,
                                    minimumDistanceFilter,
                                    maximumDistanceFilter,
                                    intensityThreshold,
                                    lidarViewScaleRate);
                            changed = true;

                            if (lidarDisplay != null) {
                                lidarDisplay.updateGraphWithDataPoints(dataPointArray);
                            }
                            if (outputLIDARDataToLog) {
                                outputDataPointArrayToLogs(dataPointArray);
                            }
                            if (writeLidarDataToFile) {
                                new Thread(new WriteLidarDataToFile(dataPointArray)).start();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
    }

    private class WriteLidarDataToFile implements Runnable {

        final DataPoint[] dataPointArray;

        public WriteLidarDataToFile(DataPoint[] dataPointArray) {
            this.dataPointArray = dataPointArray;
        }

        @Override
        public void run() {
            File file = new File(fileURI.getPath());
            try {
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                for (DataPoint dataPoint : dataPointArray) {
                    writer.write(
                            dataPoint.getAngle() +
                                    "," +
                                    dataPoint.getDistance() +
                                    "," +
                                    dataPoint.getIntensity() +
                                    "," +
                                    dataPoint.getRPM() +
                                    "\n"
                    );
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
