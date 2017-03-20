package com.home.ma.photolocationnote;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

/*
import com.masum.database.NoteTable;
import com.masum.locationlibrary.LocationApplication;
import com.masum.utils.Utility;
*/

import com.home.ma.photolocationnote.utility.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {

    // Activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final String ACTIVITY_LAUNCH = "launch";
    private File capturedImage = null;
    private boolean activity_launch = true;
    // directory name to store captured images and videos
    private static final String IMAGE_DIRECTORY_NAME = "LocationNote";
    private Uri fileUri; // file url to store image
    private ImageView imageView;
    // private ImageView imgPreview;
    // private VideoView videoPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Checking camera availability
        if (!isDeviceSupportCamera()) {
            Utility.displayWarning(this, "No camera", "Sorry! Your device doesn't support camera");
            // will close the activity if the device does't have camera
            finish();
        }

        if (savedInstanceState != null) {
            activity_launch = savedInstanceState.getBoolean(ACTIVITY_LAUNCH);
        }
        if (activity_launch) {
            Bundle extras = getIntent().getExtras();
           // int mediaType = extras.getInt(NoteTable.COLUMN_IMAGE);
            //initializeView(mediaType);
        }
        //imageView = (ImageView) findViewById(R.id.imageView);
        initializeView(MEDIA_TYPE_IMAGE);
    }

    public void initializeView(int mediaType) {

        if (mediaType == MEDIA_TYPE_IMAGE) {
            // capture picture
            captureImage();
        } else if (mediaType == MEDIA_TYPE_VIDEO) {
            // record video
            //recordVideo();
        } else {
            Utility.displayWarning(this, "Unknown media type", "Please try again");
        }
    }

    /**
     * Checking device has camera hardware or not
     */
    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /*
     * Capturing Camera Image will launch camera app requrest image capture
     */
    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    /*
     * Here we store the file url as it will be null after returning from camera
     * app
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save file url in bundle as it will be null on screen orientation changes
        outState.putParcelable("file_uri", fileUri);
        // it will not try to open camera viewfinder. Only open viewfinder for launch
        outState.putBoolean(ACTIVITY_LAUNCH, false);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // get the file url
        fileUri = savedInstanceState.getParcelable("file_uri");
    }

    /**
     * Receiving activity result method will be called after closing the camera
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                saveImage();
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture. Just return to previous activity.
                finish();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == CAMERA_CAPTURE_VIDEO_REQUEST_CODE) {
            if (resultCode == 0) {
                // video successfully recorded
                // preview the recorded video
                //previewVideo();
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled recording
                Toast.makeText(getApplicationContext(),
                        "User cancelled video recording", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to record video
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to record video", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void saveImage() {
        // bitmap factory
        BitmapFactory.Options options = new BitmapFactory.Options();
        // downsizing image as it throws OutOfMemory Exception for larger images
        options.inSampleSize = 8;
        Bitmap srcBitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
        // address and time text to stamped
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String currentTime = sdf.format(Calendar.getInstance().getTime());

        imageTextStamped(srcBitmap, Globals.getInstance().getTotalAddress(), currentTime);
       // imageView.setImageBitmap(bitmap);

        /*if (bitmap != null) {
            startNoteEditActivity();
        } else {
            Toast.makeText(this, "Failed to save image!", Toast.LENGTH_LONG).show();
        }*/
       // Utility.deleteFile(fileUri.getPath());
    }
/*

    void startNoteEditActivity() {
        LocationApplication locationApplication = (LocationApplication) getApplication();
        Bundle bundle = new Bundle();
        bundle.putDouble(NoteTable.COLUMN_LATITUDE, locationApplication.mLocationInfo.lastLat);
        bundle.putDouble(NoteTable.COLUMN_LONGITUDE, locationApplication.mLocationInfo.lastLong);
        if (locationApplication.mTotalAddress == null) {
            locationApplication.mTotalAddress = "debug address";
        }
        bundle.putString(NoteTable.COLUMN_ADDRESS, locationApplication.mTotalAddress);
        bundle.putString(NoteTable.COLUMN_IMAGE, capturedImage.getAbsolutePath());
        Intent intent = new Intent(this, NoteEditor.class);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }
*/

    void imageTextStamped(Bitmap srcBitmap, String address, String currentTime){
        Bitmap destBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cs = new Canvas(destBitmap);
        Paint tPaint = new Paint();
        tPaint.setTextSize(20);
        tPaint.setColor(Color.WHITE);
        tPaint.setStyle(Paint.Style.FILL);
        cs.drawBitmap(srcBitmap, 0f, 0f, null);

        float height = tPaint.measureText("yX");
        cs.drawText(address, 0, destBitmap.getHeight() - height, tPaint);
        cs.drawText(currentTime, 0, destBitmap.getHeight(), tPaint);
        try {
            capturedImage = getOutputMediaFile(MEDIA_TYPE_IMAGE, " ");
            destBitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(capturedImage));
        } catch (FileNotFoundException e) {
            Log.e(Globals.TAG, "ex: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * ------------ Helper Methods ----------------------
     */

	/*
     * Creating file uri to store image/video
	 */
    public Uri getOutputMediaFileUri(int type) {

        return Uri.fromFile(getOutputMediaFile(type, ""));
    }

    /*
     * returning image / video
     */
    private File getOutputMediaFile(int type, String fileNamePad) {

        String path = Environment.getExternalStorageDirectory().toString();
        File mediaStorageDir = new File(path, "/media/"+getString(R.string.app_name)+"/");
        if (!mediaStorageDir.isDirectory()) {
            mediaStorageDir.mkdirs();
        }

        // Create a media file name
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String currentTime = sdf.format(Calendar.getInstance().getTime());
        /*String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm",
                Locale.getDefault()).format(new Date()) + fileNamePad;*/
        File mediaFile;

        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "LocationImage_" + currentTime + ".jpg");

        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "LocationNoteVideo_" + currentTime + ".mp4");
        } else {
            return null;
        }

       // File file = new File(dir, filename + ".jpg");
        String imagePath =  mediaFile.getAbsolutePath();
        //scan the image so show up in album
        MediaScannerConnection.scanFile(this,
                new String[] { imagePath }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d(Globals.TAG, "scanned : " + path);
                        /*if(Config.LOG_DEBUG_ENABLED) {
                            Log.d(Config.LOGTAG, "scanned : " + path);
                        }*/
                    }
                });







        // External sdcard location
       /* File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Utility.displayWarning(this, "Directory error", "Directory creation filed!");
                Log.d(Globals.TAG, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }*/

        return mediaFile;
    }


    /*
     * Recording video
     */
  /*
    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);

        // set video quality
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file
        // name

        // start the video capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_VIDEO_REQUEST_CODE);
    }
*/

    /*
     * Previewing recorded video
     */
  /*
    private void previewVideo() {
        try {
            // hide image preview
            imgPreview.setVisibility(View.GONE);

            videoPreview.setVisibility(View.VISIBLE);
            videoPreview.setVideoPath(fileUri.getPath());
            // start playing
            videoPreview.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/

}
