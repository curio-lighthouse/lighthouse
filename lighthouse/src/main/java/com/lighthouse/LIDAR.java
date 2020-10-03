package com.lighthouse;

import android.app.Activity;
import android.os.Build;
import android.util.Log;


import com.lighthouse.Data.DataPoint;
import com.lighthouse.Data.IncomingDataHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.RequiresApi;

public class LIDAR extends Bluetooth {

    /**
     * This is the number of bytes that are contained in each message sent from the LIDAR
     * device to the android phone.  The LIDAR device captures a minimum of 6 angles in 42 bytes.
     */
    private static int bluetoothBytePacketSize = 2520;

    /**
     * This is the minimum distance that the LIDAR device can produce a reading for.
     */
    private final int MINIMUM_DISTANCE = 120;

    /**
     * This is the maximum distance that the LIDAR device can produce a reading for.
     */
    private final int MAXIMUM_DISTANCE = 3500;

    /**
     * The command used to start the LIDAR device over bluetooth.
     */
    private final String START_COMMAND = "start";

    /**
     * The command used to stop the LIDAR device over bluetooth.
     */
    private final String STOP_COMMAND = "stop";

    /**
     * Configurable value used to filter out distances lower than it.
     */
    private int minimumDistanceFilter = MINIMUM_DISTANCE;

    /**
     * Configurable value used to filter out distances higher than it.
     */
    private int maximumDistanceFilter = MAXIMUM_DISTANCE;

    /**
     * Configurable value used to filter out intensities lower than it.
     */
    int intensityThreshold = 0;

    int rpmThreshold = 0;

    /**
     * Configurable value used to set the interval in which the bluetooth
     * inputstream is checked for data.
     */
    private int pollingInterval = 0;

    /**
     * Configurable value used to set the interval at which the LidarDisplay view is
     * invalidated.
     */
    private int lidarViewRefreshRate = 16;

    /**
     * Boolean value to set output of LIDAR data to the log.
     */
    private boolean outputLIDARDataToLog = false;

    /**
     * Boolean value to set output of LIDAR data to a file.
     */
    private boolean writeLidarDataToFile = false;

    /**
     * Location of the output file in which the LIDAR data will be written.
     */
    private URI fileURI;

    /**
     * Instance of the LIDAR View.
     */
    private LidarDisplay lidarDisplay = null;

    /**
     * Array used to store LIDAR data from the inputstream.
     */
    private byte[] myByteArray;


    /**
     * Set to true when we receive a packet of data from the lidar sensor.  This will make sure
     * that we don't invalidate the view if we haven't received new data.
     */
    private static boolean changed = false;

    private long lastReadTime = 0;


    /**
     * Constructor without LidarDisplay view
     * @param activity The activity from which the LIDAR is created.
     */
    public LIDAR(Activity activity) {
        super(activity);
    }

    /**
     * Constructor with LidarDisplay view.  This constructor will handle refreshing and updating
     * the LidarDisplay.
     * @param activity The activity from which the LIDAR is created.
     * @param lidarDisplay The LidarDisplay reference.
     */
    public LIDAR(Activity activity, LidarDisplay lidarDisplay) {
        super(activity);
        this.lidarDisplay = lidarDisplay;
    }

    public int getRpmThreshold() {
        return rpmThreshold;
    }

    public void setRpmThreshold(int rpmThreshold) {
        this.rpmThreshold = rpmThreshold;
    }

    /**
     * Returns true if the LIDAR data is set to be output to a file.
     * @return
     */
    public boolean isWriteLidarDataToFile() {
        return writeLidarDataToFile;
    }

    /**
     * Sets the location of the file which will be written to for LIDAR data output.
     * @param fileLocation The location of the output file.
     */
    public void setWriteLidarDataToFile(URI fileLocation) {
        this.writeLidarDataToFile = true;
        this.fileURI = fileLocation;
    }

    /**
     * Returns the location of the specified output file for LIDAR data.
     * @return Output file location.
     */
    public URI getFileURI() {
        return fileURI;
    }

    /**
     * Returns true if the LIDAR object is configured to output to the log.  Returns false if
     * not set to output to the log.
     * @return
     */
    public boolean isOutputLIDARDataToLog() {
        return outputLIDARDataToLog;
    }

    /**
     * Sets the LIDAR data to output to the log.  Pass true for outputting to the log.  False for
     * not outputting LIDAR data to the log.
     * @param outputLIDARDataToLog Boolean value for setting LIDAR output to the log.
     */
    public void setOutputLIDARDataToLog(boolean outputLIDARDataToLog) {
        this.outputLIDARDataToLog = outputLIDARDataToLog;
    }

    /**
     * Returns the configured refresh rate for the LidarDisplay view invalidate call.
     * @return The rate at which invalidate is called on the LidarDisplay view.
     */
    public int getLidarViewRefreshRate() {
        return lidarViewRefreshRate;
    }

    /**
     * Set the rate at which invalidate is called on the LidarDisplay view.
     * @param lidarViewRefreshRate The rate at which invalidate is called on the LidarDisplay view.
     */
    public void setLidarViewRefreshRate(int lidarViewRefreshRate) {
        this.lidarViewRefreshRate = lidarViewRefreshRate;
    }

    /**
     * Returns the rate at which the bluetooth inputstream is checked for available data.
     * @return The rate at which the bluetooth inputstream is checked for available data.
     */
    public int getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Set the rate at which the bluetooth inputstream is checked for available data.
     * @param pollingInterval The rate at which the bluetooth inputstream is checked for available data.
     */
    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    /**
     * Returns the intensity threshold which is used to filter out intensity values which are lower
     * than the specified threshold value.
     * @return Threshold value.
     */
    public int getIntensityThresholdFilter() {
        return intensityThreshold;
    }

    /**
     * Set the intensity threshold which is used to filter out intensity values which are lower
     * than the specified threshold value.
     * @param intensityThreshold Threshold value.
     */
    public void setIntensityThresholdFilter(int intensityThreshold) {
        this.intensityThreshold = intensityThreshold;
    }

    /**
     * Returns the minimum distance filter value which is used to ignore distance measured less than
     * the specified value.
     * @return Minimum distance.
     */
    public int getMinimumDistanceFilter() {
        return minimumDistanceFilter;
    }

    /**
     * Sets the minimum distance filter value which is used to ignore distances measured less than
     * the specified value. Returns true if the minimum distance was set correctly.
     * @param minimumDistanceFilter Minimum Distance.
     * @return Success.
     */
    public boolean setMinimumDistanceFilter(int minimumDistanceFilter) {
        // Check to make sure that the minimumDistanceFilter value is larger than the smallest
        // range for the LIDAR device.
        if (minimumDistanceFilter > MINIMUM_DISTANCE &&
                minimumDistanceFilter < maximumDistanceFilter) {
            this.minimumDistanceFilter = minimumDistanceFilter;
            return true;
        }
        return false;
    }

    /**
     * Returns the maximum distance filter value which is used to ignore distances measured more than
     * the specified value. Returns true if the maximum distance was set correctly.
     * @return The maximum distance filter value.
     */
    public int getMaximumDistanceFilter() {
        return maximumDistanceFilter;
    }

    /**
     * Sets the maximum distance filter value which is used to ignore distances measured more than
     * the specified value. Returns true if the maximum distance was set correctly.
     * @param maximumDistanceFilter Maximum distance filter value.
     * @return Success.
     */
    public boolean setMaximumDistanceFilter(int maximumDistanceFilter) {
        // Check to make sure that the maximumDistanceFilter value is less than the maximum
        // range for the LIDAR device.
        if (maximumDistanceFilter < MAXIMUM_DISTANCE && maximumDistanceFilter > minimumDistanceFilter) {
            this.maximumDistanceFilter = maximumDistanceFilter;
            return true;
        }
        return false;
    }

    /**
     * Returns the size, in bytes, from which each chunk of data will be sent from the LIDAR to the
     * phone.
     * @return The size, in bytes, of each chunk of data sent from the LIDAR.
     */
    public int getBluetoothBytePacketSize() {
        return bluetoothBytePacketSize;
    }

    /**
     * Sets the size, in bytes, from which each chunk of data will be sent from the LIDAR to the
     * phone. Returns true if successful.  False if the value passed is not a factor of 42.
     * @param bluetoothBytePacketSize The size, in bytes, of each chunk of data sent from the LIDAR.
     * @return Success.
     */
    public boolean setBluetoothBytePacketSize(int bluetoothBytePacketSize) {
        // Check to make sure the bluetoothBytePacketSize is in increments of 42
        // since that is the byte stream size of each array of data from the LIDAR device.
        if (bluetoothBytePacketSize % 42 == 0) {
            LIDAR.bluetoothBytePacketSize = bluetoothBytePacketSize;
            return true;
        }
        return false;
    }

    /**
     * Send a stop command to the LIDAR device.  This will stop the LIDAR device from spinning and
     * sending data.
     */
    public void stopLIDAR() {
        final String STOP_MESSAGE = STOP_COMMAND;
        byte[] msgBuffer = STOP_MESSAGE.getBytes();
        try {
            getOutStream().write(msgBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start capturing data with the LIDAR device, reading from the bluetooth inputstream, and
     * updating any output specified (logs, file, LidarDisplay).
     */
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

        if (lidarDisplay != null) {
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

    }

    /**
     * Attempts to connect to the LIDAR device and open a Bluetooth Socket.  Returns true if
     * successful, false if failed.
     * @return Success.
     */
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

    /**
     * Task used to continously read from the bluetooth inputstream.
     */
    private class Task implements Runnable {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            int bytes;
            byte[] buffer;
            DataPoint[] dataPointArray;

            // Continually loop and check for messages from the pi.
            while (true) {
                if (lastReadTime == 0 || (System.currentTimeMillis() - lastReadTime) > pollingInterval) {
                    try {
                        // Read the bytes that are coming from the pi.
                        bytes = getInStream().available();
                        lastReadTime = System.currentTimeMillis();

                        // If we received a message from the pi, we will continue with processing it.
                        if (bytes >= bluetoothBytePacketSize) {
                            Log.i("lighthouse", "read: " + bytes + " of data.");
                            buffer = new byte[bytes];
                            getInStream().read(buffer);
                            System.arraycopy(buffer, 0, myByteArray, 0, bluetoothBytePacketSize);
                            dataPointArray = IncomingDataHandler.getDataPointArrayFromPiData(myByteArray,
                                    minimumDistanceFilter,
                                    maximumDistanceFilter,
                                    intensityThreshold,
                                    rpmThreshold);
                            changed = true;

                            if (lidarDisplay != null) {
                                lidarDisplay.updateGraphWithDataPoints(dataPointArray);
                            }
                            if (outputLIDARDataToLog) {
                                new Thread(new WriteLidarDataToLog(dataPointArray)).start();
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

    /**
     * Writes LIDAR data to a specified output file.
     */
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
                    if (dataPoint != null) {
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
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class WriteLidarDataToLog implements Runnable {

        final DataPoint[] dataPointArray;

        public WriteLidarDataToLog(DataPoint[] dataPointArray) {
            this.dataPointArray = dataPointArray;
        }

        @Override
        public void run() {
            for (DataPoint dataPoint : dataPointArray) {
                if (dataPoint != null) {
                    Log.i("info", "Angle: " + dataPoint.getAngle() +
                            " Distance: " + dataPoint.getDistance() +
                            " Intensity: " + dataPoint.getIntensity() +
                            " RPM: " + dataPoint.getRPM());
                }
            }
        }
    }
}
