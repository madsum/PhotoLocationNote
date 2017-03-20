package com.home.ma.photolocationnote;

/**
 * Created by ma on 19/03/2017.
 */

public class Globals {

    private static Globals instance = new Globals();

    public static Globals getInstance() {
        return instance;
    }

    public static void setInstance(Globals instance) {
        Globals.instance = instance;
    }


    public static String getTotalAddress() {
        return totalAddress;
    }

    public static void setTotalAddress(String mTotalAddress) {
        Globals.totalAddress = mTotalAddress;
    }

    public static String getmStreet() {
        return street;
    }

    public static void setStreet(String street) {
        Globals.street = street;
    }

    public static String getmCountry() {
        return country;
    }

    public static void setmCountry(String mCountry) {
        Globals.country = mCountry;
    }

    private static String totalAddress = null;
    private static String street = null;
    private static String country = null;


    private Globals() {

    }

}