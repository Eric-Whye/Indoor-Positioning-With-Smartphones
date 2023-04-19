package com.fyp.wifigatherer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private ImageView mImageView;

    private WifiManager wifiManager;
    private TextView textView;
    private EditText editText;
    private ProgressBar progressBar;
    private String gridCoordsInput;
    //private String prevScanResult = "";
    private List<GridPoint> gridPointsList = new ArrayList<>();
    private String fileName = "Grid-Data.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handlePermissions(this,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        //Image View for Map
        mImageView = findViewById(R.id.imageView);
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        //Text View for Wifi Data
        textView = findViewById(R.id.textView);

        //Edit Widget for grid point selection
        editText = findViewById(R.id.text_input);

        //Progress bar for visual confirmation of continuous scans
        progressBar = findViewById(R.id.simpleProgressBar);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                gridCoordsInput = s.toString();
            }
        };
        editText.addTextChangedListener(watcher);
    }

    private boolean scan = false;
    //When Scan Button is pressed
    public void startScan(View v) {
        if (scan) return;
        scan = true;

        Runnable runnable = () -> {
            while (scan) {
                registerReceiver(new WifiScanReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                wifiManager.startScan();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void stopScan(View v){
        scan = false;
    }

    class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                    scanSuccess();
                    unregisterReceiver(this);
            }
        }
    }


    @SuppressLint("MissingPermission")
    private void scanSuccess() {
        StringBuilder stringBuilder = new StringBuilder();

        List<ScanResult> currScan = wifiManager.getScanResults();
        System.out.println(gridPointsList.size());
        for (ScanResult result : currScan){
            String ssid = result.SSID;
            int rssi = result.level;
            stringBuilder.append(ssid + ", " + result.BSSID + ", " + rssi + "\n");
        }
        if (stringBuilder.toString().contentEquals(textView.getText()))
            return;

        textView.setText(stringBuilder.toString());



        if (!gridCoordsInput.isEmpty()){
            StringTokenizer tokens = new StringTokenizer(gridCoordsInput, ",");
            GridPoint gridPoint = new GridPoint(
                    Float.parseFloat(tokens.nextToken().trim()),
                    Float.parseFloat(tokens.nextToken().trim()),
                    currScan);

            gridPointsList.add(gridPoint);
        }
    }

    private class GridPoint{
        private final String delim = " ";
        protected float x;
        protected float y;
        private List<ScanResult> data;

        GridPoint(float x, float y, List<ScanResult> data){
            this.x = x;
            this.y = y;
            this.data = data;
        }
    }

    //Button to Write gridPointsList to File
    public void write(View v) {
        if (gridPointsList.isEmpty()) {
            Toast.makeText(this, "No Data Recorded", Toast.LENGTH_SHORT).show();
            return;
        }
        File path = getApplicationContext().getFilesDir();

        try {
            FileOutputStream fileos = openFileOutput(fileName, MODE_PRIVATE);
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.startTag(null, "data");

            for (int i = 0; i < gridPointsList.size(); i++) {
                Log.d("thing", String.valueOf(gridPointsList.size()));
                GridPoint gridPoint = gridPointsList.get(i);

                xmlSerializer.startTag(null, "grid");
                xmlSerializer.attribute(null, "id", String.valueOf(i));

                xmlSerializer.startTag(null, "X");
                xmlSerializer.text(String.valueOf(gridPoint.x));
                xmlSerializer.endTag(null, "X");

                xmlSerializer.startTag(null, "Y");
                xmlSerializer.text(String.valueOf(gridPoint.y));
                xmlSerializer.endTag(null, "Y");

                xmlSerializer.startTag(null, "wifi");
                for (int j = 0; j < gridPoint.data.size(); j++) {
                    xmlSerializer.startTag(null, "rssi");
                    xmlSerializer.attribute(null, "ssid", gridPoint.data.get(j).SSID);
                    xmlSerializer.attribute(null, "mac_ad", gridPoint.data.get(j).BSSID);
                    xmlSerializer.text(String.valueOf(gridPoint.data.get(j).level));
                    xmlSerializer.endTag(null, "rssi");
                }
                xmlSerializer.endTag(null, "wifi");
                xmlSerializer.endTag(null, "grid");
            }
            xmlSerializer.endTag(null, "data");
            xmlSerializer.endDocument();
            xmlSerializer.flush();
            String dataWrite = writer.toString();
            fileos.write(dataWrite.getBytes());
            Toast.makeText(this, "Data saved to " + path + "/" + fileName, Toast.LENGTH_LONG).show();
            fileos.close();
        } catch (IOException e) {e.printStackTrace();}
    }

        // this redirects all touch events in the activity to the gesture detector
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mScaleGestureDetector.onTouchEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        // when a scale gesture is detected, use it to resize the image
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector){
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mImageView.setScaleX(mScaleFactor);
            mImageView.setScaleY(mScaleFactor);
            return true;
        }
    }

    public void handlePermissions(Context context, String... permissions){
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 1);
                    return;
                }
            }
        }
    }
}