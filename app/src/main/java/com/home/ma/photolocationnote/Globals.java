package com.home.ma.photolocationnote;

import android.location.Location;
import android.os.Environment;

import java.io.File;

public class Globals {

    private static Globals instance = new Globals();

    public static Globals getInstance() {
        return instance;
    }

    public static final String TAG = "photoLocationNote";


    public static String getTotalAddress() {
        return totalAddress;
    }

    public static void setTotalAddress(String totalAddress) {
        Globals.totalAddress = totalAddress;
    }

    private static String totalAddress = "No address";
    public static String PHOTO_FILE_KEY = "photoFileKey";

    public static Location getLocation() {
        return location;
    }

    public static void setLocation(Location location) {
        Globals.location = location;
    }

    private static Location location;
    public static final String APPLICATION_NAME = "Photo Location Note";
    private static File mediaStorageDir = new File(Environment.getExternalStorageDirectory().toString(),
                "/media/" + APPLICATION_NAME + "/");

    public static File getMediaStorageDir() {
        return mediaStorageDir;
    }

    private Globals() {

    }

}