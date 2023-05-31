# Indoor Positioning With Smartphones

Final Year Project where the aim is to apply techniques to achieve viable indoor positioning (In the UCD O'Brien Science Centre) using a smartphone

Full details of the project and implementation are in the included Final Report

The main outcomes of the project were:

Tools were developed to capture inertial and Wi-Fi Recevied Signal Strength(RSS) Data. This is despite the fact that the project brief outlined the lack of necessity for a smartphone app, in the end, I made 2.

Appropriate data processing was applied to both the ouputs of the two tools. This data is then fed into a particle filter.

The particle filter was implemented and successfully predicted the real path with artificial test data. However it was not able to predict any positiong with real data due to the limitations of the Wi-Fi RSS data detailed the Final Report


# Contents

InertialGatherer is the android app for gathering data during the online phase

WifiGatherer is the android app fro gathering data during the offline phase

Core is where all the input xml files are with the jupyter notebook file for data processing and the implementation of the particle filter
