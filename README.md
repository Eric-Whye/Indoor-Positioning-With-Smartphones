# Indoor-Positioning-with-Smartphones
Final Year Project where the aim is to apply techniques to achieve viable indoor positioning (In the UCD O'Brien Science Centre) using a smartphone

### What was achieved:

Tools were made in the form of android apps to collect the necessary Wi-Fi and inertial data despite the projet brief stating that the development of an app was not necessary. In the end, I made two.

Data processing was applied to the collected data to be used in the particle filter which ultimately predicts the real path of the user.

The most difficult part of the implementation was the particle filter and it was successful when working with test data and was able to predict a simulated user given quality data (Data in large quantity was found to not be necessary). Despite this fact, the particle filter was not able to predict a real user with real data. The inertial and heading data was of a sufficient standard, but the Wi-Fi RSS data was too inaccurate and lacked both quality and quantity.

Regardless the project was a success as it achieved the objective of applying researched techniques and achieved indoor positioning in a theorectical environment.

The full details of the implementation and the results can be found in the included Final Report

### Contents:

WifiGatherer is the android app for collecting wifi RSS data to make an RSS fingerprinting map.  (Offline phase)
  
InertialGatherer is the android app where the user's acceleration data, rotational vector and wifi scans are collected. (Online Phase)

Core is where all the output files of the above apps are processed and used in the implementation of the particle filter.

The final report chapter on implementation explains all these terms.
