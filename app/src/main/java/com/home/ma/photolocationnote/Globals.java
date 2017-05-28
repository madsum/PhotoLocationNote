package com.home.ma.photolocationnote;

import android.content.Context;
import android.location.Location;
import android.os.Environment;

import java.io.File;

/**
 * Created by ma on 19/03/2017.
 */

public class Globals {

    private static Globals instance = new Globals();

    public static Globals getInstance() {
        return instance;
    }

//    public static void setInstance(Globals instance) {
//        Globals.instance = instance;
//    }

    public static final String TAG = "photoLocationNote";


    public static String getTotalAddress() {
        return totalAddress;
    }

    public static void setTotalAddress(String totalAddress) {
        Globals.totalAddress = totalAddress;
    }

//    public static String getStreet() {
//        return street;
//    }

//    public static void setStreet(String street) {
//        Globals.street = street;
//    }

//    public static String getCountry() {
//        return country;
//    }

//    public static void setCountry(String mCountry) {
//        Globals.country = mCountry;
//    }

    private static String totalAddress = "No address";
    public static String PHOTO_FILE_KEY = "photoFileKey";

    public static Location getLocation() {
        return location;
    }

    public static void setLocation(Location location) {
        Globals.location = location;
    }

    private static Location location;
   // private static String street = null;
   // private static String country = null;
    public static final String APPLICATION_NAME = "Photo Location Note";
    private static File mediaStorageDir = new File(Environment.getExternalStorageDirectory().toString(),
                "/media/" + APPLICATION_NAME + "/");

    public static File getMediaStorageDir() {
        return mediaStorageDir;
    }

    private Globals() {

    }

}