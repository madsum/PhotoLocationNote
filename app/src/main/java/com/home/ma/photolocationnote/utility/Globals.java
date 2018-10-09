package com.home.ma.photolocationnote.utility;

import android.app.Activity;
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

    public static Globals getInstance(Activity activity) {
        Globals.activity = activity;
        return instance;
    }

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
    public static final int REQUEST_PERMISSIONS_REQUEST_CODE = 111;
    public final static int MY_REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 102;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    // Activity request codes
    public static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int REQUEST_EXTERNAL_PERMISSION_RESULT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final String ACTIVITY_LAUNCH = "launch";

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

    private static String mPhotoFilePath = null;

    private static String mPhotoFileName = null;

    private String HubEndpoint = null;
    private String HubSasKeyName = null;
    private String HubSasKeyValue = null;
    private static Activity activity = null;

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

    public static String getmPhotoFilePath() {
        return mPhotoFilePath;
    }

    public static void setmPhotoFilePath(String mPhotoFilePath) {
        Globals.mPhotoFilePath = mPhotoFilePath;
    }

    public static String getmPhotoFileName() {
        return mPhotoFileName;
    }

    public static void setmPhotoFileName(String mPhotoFileName) {
        Globals.mPhotoFileName = mPhotoFileName;
    }

    // Method to share either text or URL.
    public static void shareTextUrl(Activity activity) {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // Add data to the intent, the receiving app will decide
        // what to do with it.
        share.putExtra(Intent.EXTRA_SUBJECT, "Photo Location Note");
        share.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=com.home.ma.photolocationnote");

        activity.startActivity(Intent.createChooser(share, "Share link!"));
    }
}