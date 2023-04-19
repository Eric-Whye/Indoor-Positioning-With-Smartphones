package com.fyp.intertialgatherer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{
    //Text view of Wifi data
    private TextView wifiTextView;

    //Text view of angle data
    private TextView angleTextView;

    //Compass image variable
    private ImageView compassImg;

    //Graph variables
    private int pointsPlotted=1;
    private final LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
            new DataPoint(0, 0)
    });
    private Viewport viewport;

    //Sensors
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor rotateSensor;

    //Wifi
    private WifiManager wifiManager;
    private boolean isScanReady = false;
    private List<ScanResult> currScan;

    //File I/O Related
    private String fileName = "data1.xml";
    private boolean isRecordStart = false;
    private List<com.fyp.intertialgatherer.DataPoint> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handlePermissions(this,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE);

        //Sensor Management
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rotateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorEventListener, rotateSensor, SensorManager.SENSOR_DELAY_GAME);



        //Acceleration Graph
        GraphView graph = (GraphView) findViewById(R.id.graph);
        viewport = graph.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMaxY(10+20);
        viewport.setMinY(10-20);
        graph.addSeries(series);


        //Text view of Angle data
        angleTextView = findViewById(R.id.text_angle);

        //Compass Image
        compassImg = (ImageView) findViewById(R.id.compass);


        //Wifi Management
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        this.registerReceiver(new WifiScanReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        float x;
        float y;
        float z;
        double acceleration;

        float[] values = new float[3];
        private float lastRotateDegree;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                //Extract axis from acceleration
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                acceleration = Math.sqrt(x*x + y*y + z*z);

                //update graph
                pointsPlotted++;
                if (pointsPlotted > 1000) {
                    pointsPlotted = 1;
                    series.resetData(new DataPoint[] {new DataPoint(pointsPlotted, acceleration)});
                } else
                    series.appendData(new DataPoint(pointsPlotted, acceleration), true, pointsPlotted);
                viewport.setMaxX(pointsPlotted);
                viewport.setMinX(pointsPlotted-200);


            }
            else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {

                float[] R = new float[9];
                values = new float[3];
                SensorManager.getRotationMatrixFromVector(R, event.values);
                SensorManager.getOrientation(R, values);
                //Log.d("MainActivity", "value[0] is " + Math.toDegrees(values[0]));
                float rotateDegree = -(float) Math.toDegrees(values[0]);
                if (Math.abs(rotateDegree - lastRotateDegree) > 1) {
                    RotateAnimation animation = new RotateAnimation(
                            lastRotateDegree, rotateDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    animation.setFillAfter(true);
                    compassImg.startAnimation(animation);
                    lastRotateDegree = rotateDegree;
                }

                angleTextView.setText(String.format("%.3f", rotateDegree));

                //Record Sensor Data when activated
                if (isRecordStart) {

                    com.fyp.intertialgatherer.DataPoint dataPoint;
                    if (isScanReady) {
                        dataPoint = new com.fyp.intertialgatherer.DataPoint(acceleration, Math.toDegrees(values[0]), event.timestamp,
                                currScan);
                        isScanReady = false;
                    } else
                        dataPoint = new com.fyp.intertialgatherer.DataPoint(acceleration, Math.toDegrees(values[0]), event.timestamp,
                            null);

                    list.add(dataPoint);
                }

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }


    };

    public void calibrate(View v){
        sensorManager.unregisterListener(sensorEventListener, rotateSensor);
        sensorManager.registerListener(sensorEventListener, rotateSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void startRec(View v){
        if (isRecordStart){
            Toast.makeText(this, "Recording Already Started", Toast.LENGTH_SHORT).show();
            return;
        }
        isRecordStart = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRecordStart) {
                    registerReceiver(new WifiScanReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    wifiManager.startScan();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        //ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(2);
        //exec.scheduleAtFixedRate(() -> isWifiReady = true, 1, 2, TimeUnit.SECONDS); // execute every 60 seconds
        Toast.makeText(this, "Data Recording Started", Toast.LENGTH_SHORT).show();
    }

    public void endRec(View v){
        if (!isRecordStart){
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

            for (int i = 0; i < list.size(); i++) {
                com.fyp.intertialgatherer.DataPoint dataPoint = list.get(i);

                xmlSerializer.startTag(null, "data-point");
                xmlSerializer.attribute(null, "id", String.valueOf(i));

                xmlSerializer.startTag(null, "acceleration");
                xmlSerializer.text(String.valueOf(dataPoint.acceleration));
                xmlSerializer.endTag(null, "acceleration");

                xmlSerializer.startTag(null, "heading");
                xmlSerializer.text(String.valueOf(dataPoint.heading));
                xmlSerializer.endTag(null, "heading");

                xmlSerializer.startTag(null, "timestamp");
                xmlSerializer.text(String.valueOf((long)dataPoint.timestamp));
                xmlSerializer.endTag(null, "timestamp");

                xmlSerializer.startTag(null, "wifi");
                if (dataPoint.data!= null)
                    for (int j = 0; j < dataPoint.data.size(); j++) {
                        xmlSerializer.startTag(null, "rssi");
                        xmlSerializer.attribute(null, "ssid", dataPoint.data.get(j).SSID);
                        xmlSerializer.attribute(null, "mac_ad", dataPoint.data.get(j).BSSID);
                        xmlSerializer.text(String.valueOf(dataPoint.data.get(j).level));
                        xmlSerializer.endTag(null, "rssi");
                    }
                xmlSerializer.endTag(null, "wifi");
                xmlSerializer.endTag(null, "data-point");
            }
            xmlSerializer.endTag(null, "data");
            xmlSerializer.endDocument();
            xmlSerializer.flush();
            String dataWrite = writer.toString();
            fileos.write(dataWrite.getBytes());
            Toast.makeText(this, "Data saved to " + path + "/" + fileName, Toast.LENGTH_LONG).show();
            fileos.close();
            list.clear();
        } catch (IOException e) {e.printStackTrace();}
    }


    class WifiScanReceiver extends BroadcastReceiver {
        private String delim = " ";
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
        for (ScanResult result : currScan){
            String ssid = result.SSID;
            int rssi = result.level;
            stringBuilder.append(ssid + ", " + result.BSSID + ", " + rssi + "\n");
        }
        if (this.currScan != null){
            StringBuilder tempStringBuilder = new StringBuilder();
            for (ScanResult result : this.currScan){
                String ssid = result.SSID;
                int rssi = result.level;
                tempStringBuilder.append(ssid + ", " + result.BSSID + ", " + rssi + "\n");
            }
            if (stringBuilder.toString().equals(tempStringBuilder.toString()))
                return;
        }

        this.currScan = currScan;
        isScanReady = true;
    }


    
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
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