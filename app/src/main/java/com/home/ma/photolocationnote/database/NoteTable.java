package com.home.ma.photolocationnote.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.home.ma.photolocationnote.utility.Globals;

public class NoteTable {

    // Database table
    public static final String TABLE_NOTE = "note";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_IMAGE = "image";


    // Database creation SQL statement
    private static final String DATABASE_CREATE = "CREATE TABLE " +
            TABLE_NOTE
            + "( "
            + COLUMN_ID + " INTEGER PRIMARY  KEY AUTOINCREMENT, "
            + COLUMN_TITLE  +" VARCHAR(255), "
            + COLUMN_DESCRIPTION + " VARCHAR(1024), "
            + COLUMN_DATE + " VARCHAR(255),"
            + COLUMN_LATITUDE + " REAL,"
            + COLUMN_LONGITUDE + " REAL,"
            + COLUMN_ADDRESS + " VARCHAR(255),"
            + COLUMN_IMAGE + " VARCHAR(255)"
            +");";
    // Database drop table SQL statement
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NOTE;


    public static void onCreate(SQLiteDatabase database) {
        try {
            database.execSQL(DATABASE_CREATE);
            Log.i(Globals.TAG, "table created successfully");
        }catch (Exception e) {
            //System.out.println("ex: "+e.getMessage());
            Log.i(Globals.TAG, "exception: "+e.getMessage());
        }

    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(Globals.TAG, "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL(DROP_TABLE);
        onCreate(database);
    }

}
