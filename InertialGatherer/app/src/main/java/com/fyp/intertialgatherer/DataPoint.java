package com.fyp.intertialgatherer;

import android.net.wifi.ScanResult;

import java.util.List;

public class DataPoint {
    double acceleration;
    double heading;
    double timestamp;
    List<ScanResult> data;

    DataPoint(double acceleration, double heading, double timestamp, List<ScanResult> data){
        this.acceleration = acceleration;
        this.heading = heading;
        this.timestamp = timestamp;
        this.data = data;
    }
}
