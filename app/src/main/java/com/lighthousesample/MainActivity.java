package com.lighthousesample;

import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;

import com.lighthouse.LIDAR;
import com.lighthouse.LidarDisplay;


public class MainActivity extends Activity {

    LIDAR myLidar;
    LidarDisplay lidarDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This is the graphical plotting of the LIDAR data.
        lidarDisplay = new LidarDisplay(this);
        ConstraintLayout linearLayout = findViewById(R.id.constraint_layout);
        linearLayout.addView(lidarDisplay);
        lidarDisplay.setLidarViewScaleRate(10);
        //lidarDisplay.setAlphaByDistance(true);

        myLidar = new LIDAR(this, lidarDisplay);
        myLidar.setBluetoothBytePacketSize(2520);
        myLidar.setIntensityThresholdFilter(20);
        myLidar.setRpmThreshold(2300);
        myLidar.setLidarViewRefreshRate(32);

        // This is the button to start the LIDAR unit.
        final Button conn2PiBtn = findViewById(R.id.conn2PiBtn);
        conn2PiBtn.setOnClickListener(v -> {
            // TODO: Don't allow double press.
            if (myLidar.connectToLIDAR()) {
                myLidar.startLIDAR();
            }
        });

    }

}
