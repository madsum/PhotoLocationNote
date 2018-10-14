package com.home.ma.photolocationnote;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.home.ma.photolocationnote.utility.Globals;
import com.home.ma.photolocationnote.utility.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CameraActivity extends Activity {

    // Activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int REQUEST_EXTERNAL_PERMISSION_RESULT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final String ACTIVITY_LAUNCH = "launch";
    private boolean activity_launch = true;
    // file url to store image
    private Uri fileUri;
    // final bitmap after address and time stamped.
    private Bitmap processedBitmap;
    private ImageView imageView;
    private File mediaFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // To bypass the new Android security model
        StrictMode.VmPolicy.Builder newbuilder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(newbuilder.build());
        setContentView(R.layout.activity_camera);
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
            initializeView(MEDIA_TYPE_IMAGE);
        }
        imageView = (ImageView) findViewById(R.id.imageView);
    }

    public void initializeView(int mediaType) {

        if (mediaType == MEDIA_TYPE_IMAGE) {
            // capture picture
            callCameraApp();
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
     * Capturing Camera Image will launch camera app request image capture
     */
    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    private void callCameraApp(){
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            captureImage();
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(this, "External storage permission is required to save photo", Toast.LENGTH_LONG).show();
                }
                requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_PERMISSION_RESULT);
            }else {
                captureImage();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == REQUEST_EXTERNAL_PERMISSION_RESULT){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                captureImage();
            }else {
                Toast.makeText(this, "External storage permission not granted, so photo can't be saved", Toast.LENGTH_LONG ).show();
            }
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //Here we store the file url as it will be null after returning from camera app
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


    //Receiving activity result method will be called after closing the camera
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
        }
    }

    private void saveImage() {
        // bitmap factory
        BitmapFactory.Options options = new BitmapFactory.Options();
        // downsizing image as it throws OutOfMeextrasmory Exception for larger images
        options.inSampleSize = 8;
        Bitmap srcBitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
        // address and time to be stamped
        imageTextStamped(srcBitmap);
    }

    void imageTextStamped(Bitmap srcBitmap) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        processedBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cs = new Canvas(processedBitmap);
        Paint tPaint = new Paint();
        tPaint.setTextSize(20);
        tPaint.setColor(Color.WHITE);
        tPaint.setStyle(Paint.Style.FILL);
        cs.drawBitmap(srcBitmap, 0f, 0f, null);
        float height = tPaint.measureText("yX");
        // current address to stamped
        cs.drawText(Globals.getInstance(this).getTotalAddress(), 0, processedBitmap.getHeight() - height, tPaint);
        // current time text to stamped
        cs.drawText(sdf.format(Calendar.getInstance().getTime()), 0, processedBitmap.getHeight(), tPaint);
        try {
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new File(fileUri.getPath())));
        } catch (FileNotFoundException e) {
            Log.e(Globals.TAG, "ex: " + e.getMessage());
            e.printStackTrace();
        }
        // picture will be visible in phone's gallery
        populatePhotoInGallery(fileUri.getPath());
        startNoteActivity();
    }

    private void startNoteActivity(){
        Bundle bundle = new Bundle();
        // this start Note editor
        bundle.putString(Globals.PHOTO_FILE_KEY, fileUri.getPath());
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void populatePhotoInGallery(String path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(new File(path));
            scanIntent.setData(contentUri);
            sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            sendBroadcast(intent);
        }
    }

    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private File getOutputMediaFile(int type) {
        if (!Globals.getMediaStorageDir().isDirectory()) {
            Globals.getMediaStorageDir().mkdirs();
        }


        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(Globals.getMediaStorageDir().getPath() + File.separator
                    + Globals.getTotalAddress() + ".jpg");
            String fileName;
            int counter = 1;
            while (mediaFile.exists()) {
                fileName = Globals.getTotalAddress() + "(" + counter + ")" + ".jpg";
                counter++;
                mediaFile = new File(Globals.getMediaStorageDir().getPath() + File.separator + fileName);
            }
        } else {
            return null;
        }
        Globals.setmPhotoFileName(mediaFile.getAbsoluteFile().getName());
        return mediaFile;
    }

}
