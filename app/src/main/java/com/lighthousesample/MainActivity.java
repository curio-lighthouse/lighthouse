package com.lighthousesample;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.lighthouse.LIDAR;
import com.lighthouse.LidarDisplay;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
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

        myLidar = new LIDAR(this, lidarDisplay);

        // This is the button to start the LIDAR unit.
        final Button conn2PiBtn = findViewById(R.id.conn2PiBtn);
        conn2PiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Don't allow double press.
                if (myLidar.connectToLIDAR()) {
                    myLidar.startLIDAR();
                }
            }
        });

    }

}
