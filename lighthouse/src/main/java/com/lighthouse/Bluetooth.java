package com.lighthouse;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Bluetooth {

    /**
     * The UUID used to identify the LIDAR device over bluetooth.  This is a standard value.
     */
    private static final UUID LIDAR_UUID = UUID.fromString("1e0ca4ea-299d-4335-93eb-27fcfe7fa848"); // "random" unique identifier

    /**
     * Constant for the name of the LIDAR device advertising the bluetooth service.
     */
    private static final String PI_DEVICE_NAME = "raspberrypi";

    /**
     * Message displayed when LIDAR device is not found.
     */
    private static final String LIDAR_NOT_FOUND = "Unable to detect the LIDAR nearby";

    /**
     * Messaged displayed when Bluetooth is turned off.
     */
    private static final String BLUETOOTH_MUST_BE_ON = "Bluetooth must be turned on to use LIDAR";

    /**
     * Message displayed when Bluetooth is not supported by the phone.
     */
    private static final String BLUETOOTH_NOT_SUPPORTED = "Bluetooth is not supported on this device";

    /**
     * Message displayed when Bluetooth discovery fails to start.
     */
    private static final String BLUETOOTH_DISCOVERY_FAILED = "Bluetooth discovery failed to start";

    /**
     * Bluetooth socket used for communication with LIDAR.
     */
    protected BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    /**
     * Bluetooth adapter for the phone.
     */
    private BluetoothAdapter mBTAdapter = null;

    // TODO: see how this is used and if I can delete it.
    private Set<BluetoothDevice> mPairedDevices;

    /**
     * Boolean flag values for signaling results from discovery and searching for the LIDAR bluetooth
     * signal.
     */
    private volatile boolean lidarDeviceFoundNearby, discoveryFinished = false;

    /**
     * The activity from which the LIDAR device is being used.
     */
    private final Activity activity;

    /**
     * The output stream which is used to send messages to the LIDAR device.
     */
    private OutputStream outStream = null;

    /**
     * The input stream which the LIDAR device uses to send data to the application.
     */
    private InputStream inStream = null;

    /**
     * Constructor.
     * @param activity The activity from which the LIDAR object is created.
     */
    public Bluetooth(Activity activity) {
        this.activity = activity;
    }

    /**
     * Returns the output stream.
     * @return Output stream used for sending data to the LIDAR device.
     */
    public OutputStream getOutStream() {
        return outStream;
    }

    /**
     * Returns the input stream.
     * @return The input stream which the LIDAR device uses to send data to the application.
     */
    public InputStream getInStream() {
        return inStream;
    }

    /**
     * Requests the user to enable bluetooth on the device.  If the device does not have bluetooth,
     * or the bluetooth adapter is not enabled, then this method will return false.
     */
    public boolean enableBluetoothOnPhone() {

        boolean bluetoothIsOn = true;

        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        // Check if the phone supports bluetooth.
        if (mBTAdapter == null) {
            // Device does not support Bluetooth
            bluetoothIsOn = false;
            Toast.makeText(activity.getApplicationContext(), BLUETOOTH_NOT_SUPPORTED, Toast.LENGTH_SHORT).show();
        } else {

            // Here we need to check to make sure the bluetooth adapter on the phone is enabled.
            if (!mBTAdapter.isEnabled()) {
                // Or else we need to request that the user enable it.
                Toast.makeText(activity.getBaseContext(), BLUETOOTH_MUST_BE_ON, Toast.LENGTH_SHORT).show();
                bluetoothIsOn = false;
            }
        }
        return bluetoothIsOn;
    }

    /**
     * Connects to the LIDAR device and returns a Bluetooth Socket.
     * @return The bluetooth socket.
     */
    public BluetoothSocket connectToLIDARAndGetSocket() {
        // Check if we have previously connected to the LIDAR.
        if (!connectToPiIfPreviouslyPaired()) {
            // If not, discover new LIDAR and connect to that one.
            if (!discoverNewLIDARAndConnect()) {
                return mBTSocket;
            }
        }

        long delayTime = System.currentTimeMillis() + 30000;
        // Wait for the bluetooth connection to succeed.
        while (!mBTSocket.isConnected() && System.currentTimeMillis() < delayTime );
        return mBTSocket;
    }

    /**
     * This broadcast receiver handles callbacks from the startDiscovery method when the phone finds a
     * bluetooth device.
     */
    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mPairedDevices.add(device);

                if (PI_DEVICE_NAME.equals(device.getName())) {
                    String piAddress = device.getAddress();
                    connectToPi(piAddress);
                    lidarDeviceFoundNearby = true;
                }
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                discoveryFinished = true;
                Toast.makeText(activity.getBaseContext(), BLUETOOTH_MUST_BE_ON, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Discovers and connects to any Lidar device nearby.
     */
    protected boolean discoverNewLIDARAndConnect() {

        // Check if the device is already discovering
        if (mBTAdapter.isDiscovering()) {
            // If the device is already discovering, then we will cancel the current discovery
            // and continue with our new discovery.
            mBTAdapter.cancelDiscovery();
        }

        if (mBTAdapter.isEnabled()) {

            if (ContextCompat.checkSelfPermission(activity.getApplicationContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }

            // Ask for location permission if not already allowed
            if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.BLUETOOTH_ADMIN}, 1);
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH}, 1);


            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            activity.getApplicationContext().registerReceiver(blReceiver, intentFilter);
            BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            boolean started = mBtAdapter.startDiscovery();
            if (!started) {
                Toast.makeText(activity.getBaseContext(), BLUETOOTH_DISCOVERY_FAILED, Toast.LENGTH_SHORT).show();
            }

            // Inside of the broadcast receiver for startDiscovery, we will set the piDeviceFoundNearby
            // flag to true if we find a pi device nearby which we then try to connect to.  Otherwise,
            // we continue to loop in this while loop.
            while (!lidarDeviceFoundNearby && !discoveryFinished && started);

        } else {
            Toast.makeText(activity.getBaseContext(), LIDAR_NOT_FOUND, Toast.LENGTH_SHORT).show();
        }
        return lidarDeviceFoundNearby;
    }

    /**
     * This method checks to see if the pi has already been paired to the phone in the past.
     * This returns true if the pi has been paired in the past, and returns false if no pi
     * has ever been paired to the phone before.
     * @return
     */
    protected boolean connectToPiIfPreviouslyPaired() {
        boolean result = false;

        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // We have to first check if the bluetooth adapter is still enabled, or else getBondedDevices
        // will return an empty list.
        if (mBtAdapter.isEnabled()) {
            mPairedDevices = mBtAdapter.getBondedDevices();
            // getBondedDevices will give us a list of all the devices which have previously been paired
            // with the phone.
            for (BluetoothDevice device : mPairedDevices) {

                if (PI_DEVICE_NAME.equals(device.getName())) {
                    // Since we confirmed that the device has the same name as our pi,
                    // we will go ahead and connect to it again.
                    String address = device.getAddress();
                    connectToPi(address);
                    result = true;
                }
            }
        } else {
            // The bluetooth adapter on the phone is not turned on.
            Toast.makeText(activity.getBaseContext(), BLUETOOTH_MUST_BE_ON, Toast.LENGTH_SHORT).show();
        }

        return result;
    }

    /**
     * Connects to the pi over bluetooth.  Requires the address as a parameter, the BT Adapter should
     * already be enabled.  This method will block the UI thread.  Once connected, this method will
     * open the in and out streams.  Upon success of everything this method will return true.  If
     * anything fails to connect or open this method will return false.
     * @param piAddress
     */
    private void connectToPi(final String piAddress) {

        // Check to make sure that the bluetooth adapter is enabled.
        if (mBTAdapter.isEnabled()) {

            BluetoothDevice device = mBTAdapter.getRemoteDevice(piAddress);


            // Create the socket.
            try {
                mBTSocket = device.createRfcommSocketToServiceRecord(LIDAR_UUID);
            } catch (IOException e) {
                // TODO: insert code to deal with this
                return;
            }

            // Establish the Bluetooth socket connection.
            try {
                // Cancel discovery since this could interfere with our connect() call.
                mBTAdapter.cancelDiscovery();
                mBTSocket.connect();
            } catch (IOException e) {
                try {
                    mBTSocket.close();
                    return;
                } catch (IOException e2) {
                    // TODO: insert code to deal with this
                    return;
                }
            }


            // Now we will setup the data streams which will communicate to the LIDAR.
            try {
                inStream = mBTSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                outStream = mBTSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(activity.getBaseContext(), BLUETOOTH_MUST_BE_ON, Toast.LENGTH_SHORT).show();
        }
    }
}
