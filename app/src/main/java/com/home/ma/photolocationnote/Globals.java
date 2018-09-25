package com.home.ma.photolocationnote;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;

public class Globals {

    private static Globals instance = new Globals();
    private static Context context;

    public static Globals getInstance(Context context) {
        Globals.context = context;
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

    public static void openGallery(Context context) {
        String bucketId = "";
        final String[] projection = new String[] {"DISTINCT " + MediaStore.Images.Media.BUCKET_DISPLAY_NAME + ", " + MediaStore.Images.Media.BUCKET_ID};
        final Cursor cur = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);
        while (cur != null && cur.moveToNext()) {
            final String bucketName = cur.getString((cur.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)));
            if (bucketName.equals(Globals.APPLICATION_NAME)) {
                bucketId = cur.getString((cur.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID)));
                break;
            }
        }
        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        if (bucketId.length() > 0) {
            mediaUri = mediaUri.buildUpon()
                    .authority("media")
                    .appendQueryParameter("bucketId", bucketId)
                    .build();
        }
        if(cur != null){
            cur.close();
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, mediaUri);
        context.startActivity(intent);
    }

    private Globals() {

    }

}