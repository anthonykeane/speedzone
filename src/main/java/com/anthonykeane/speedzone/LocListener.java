package com.anthonykeane.speedzone;

/**
 * Created by Keanea on 2/06/13.
 */

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class LocListener implements LocationListener {
    private static double lat = 0.0;
    private static double lon = 0.0;
    private static float bearing = 0;

    private static double alt = 0.0;
    private static float speed = 0;
    private static float accuracy = 0;
    private static boolean bUpdated = false;
    private Location locCurrent = new Location("");
    private Location locLast = new Location("");


    public static double getLat() {
        bUpdated = false;
        return lat;
    }

    public static double getLon() {
        bUpdated = false;
        return lon;
    }


    public static double getAlt() {
        bUpdated = false;
        return alt;
    }


    public static double getSpeed() {
        bUpdated = false;
        return speed;
    }

    public static double getBearing() {
        bUpdated = false;
        return bearing;
    }


    public static Location getLocLast() {
        return getLocLast();
    }

    public static Location getLocCurrent() {
        return getLocCurrent();
    }
    public static boolean getUpdated() {
        return bUpdated;
    }



    @Override
    public void onLocationChanged(Location location) {


        locLast = locCurrent;
        locCurrent = location;
        lat = location.getLatitude();
        lon = location.getLongitude();
        alt = location.getAltitude();
        speed = location.getSpeed();
        bearing = location.getBearing();
        accuracy = location.getAccuracy();
        Log.i("GPS", "onLocationChanged  ");




    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {



    }
}
