package com.home.ma.photolocationnote;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.home.ma.photolocationnote.azure.ImageManager;
import com.home.ma.photolocationnote.azure.MyHandler;
import com.home.ma.photolocationnote.azure.NotificationSettings;
import com.home.ma.photolocationnote.azure.RegistrationIntentService;
import com.home.ma.photolocationnote.contentProvider.NoteContentProvider;
import com.home.ma.photolocationnote.database.NoteTable;
import com.home.ma.photolocationnote.utility.Utility;
import com.microsoft.windowsazure.notifications.NotificationsManager;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class NoteEditorActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private TextView mTvImageName;
    private EditText mEtNoteAddress;
    private EditText mEtNoteTitle;
    private EditText mEtNoteDesc;
    private TextView mTvDate;
    private String mPhotoFileName = null;
    private PopupWindow mPopupWindow = null;
    private Uri noteUri;
    private double mLongitude = 0;
    private double mLatitude = 0;
    private int mNoteTableUid = 0;
    private final static int MY_REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 102;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private String HubEndpoint = null;
    private String HubSasKeyName = null;
    private String HubSasKeyValue = null;
    public static final String TAG = "photoLocationNote";
    private DrawerLayout drawer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


         drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.app_name, R.string.app_name) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
                // hide soft keyboard while drawer open
                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank
                super.onDrawerOpened(drawerView);
                // hide soft keyboard while drawer open
                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        };

        drawer.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initializeEditor(savedInstanceState);

        // Register for push notification.
        NotificationsManager.handleNotifications(this, NotificationSettings.SenderId, MyHandler.class);
        registerWithNotificationHubs();
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported by Google Play Services.");
                Toast.makeText(this, "This device is not supported by Google Play Services.", Toast.LENGTH_LONG).show();
                return false;
            }
            return false;
        }
        return true;
    }

    public void registerWithNotificationHubs()
    {
        Log.i(TAG, " Registering with Notification Hubs");

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }


    public void initializeEditor(Bundle savedInstanceState) {

        mTvDate = (TextView) findViewById(R.id.tvDate);
        mEtNoteTitle = (EditText) findViewById(R.id.etNoteTitle);
        mEtNoteDesc = (EditText) findViewById(R.id.etNoteDesc);
        mEtNoteAddress = (EditText) findViewById(R.id.etNoteAddress);
        mTvImageName = (TextView) findViewById(R.id.tvImageName);

        mEtNoteAddress.setText(Globals.getTotalAddress());
        mEtNoteAddress.setVisibility(View.VISIBLE);

        Bundle extras = getIntent().getExtras();

        // check from the saved Instance
        noteUri = (savedInstanceState == null) ?
                null : (Uri) savedInstanceState.getParcelable(NoteContentProvider.CONTENT_ITEM_TYPE);

        // Or passed from the other activity
        if (extras != null) {
            noteUri = extras.getParcelable(NoteContentProvider.CONTENT_ITEM_TYPE);
            if (noteUri != null) {
                // it is old note to open with uri. So fill info
                fillData(noteUri);
            }else{
                // a new note with photo
                mPhotoFileName = extras.getString(Globals.PHOTO_FILE_KEY);
                if (mPhotoFileName != null) {
                    mTvImageName.setText("Photo");
                    mTvImageName.setVisibility(View.VISIBLE);
                    if (Utility.fileExist(mPhotoFileName)) {
                        Globals.getInstance(this).setmPhotoFilePath(mPhotoFileName);
                    }
                }
                // I should set current date
                Calendar c = Calendar.getInstance();
                int day = c.get(Calendar.DATE);
                int month = c.get(Calendar.MONTH);
                int year = c.get(Calendar.YEAR);
                String date = Integer.toString(day) + "/" + Integer.toString(month) + "/" + Integer.toString(year);
                mTvDate.setText(date);
            }

        } else {
            // a new note without a photo so we should set current date only
            Calendar c = Calendar.getInstance();
            int day = c.get(Calendar.DATE);
            int month = c.get(Calendar.MONTH);
            int year = c.get(Calendar.YEAR);
            String date = Integer.toString(day) + "/" + Integer.toString(month) + "/" + Integer.toString(year);
            mTvDate.setText(date);
        }
    }


    private void fillData(Uri uri) {
        String[] projection = {NoteTable.COLUMN_ID, NoteTable.COLUMN_TITLE,
                NoteTable.COLUMN_DESCRIPTION, NoteTable.COLUMN_DATE,
                NoteTable.COLUMN_ADDRESS, NoteTable.COLUMN_IMAGE};
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, projection, null, null,
                    null);
        } catch (Exception e) {
            Log.e(Globals.TAG, "ex: " + e.getMessage());
        }

        if (cursor != null) {
            cursor.moveToFirst();
            mNoteTableUid = cursor.getInt(cursor.getColumnIndex(NoteTable.COLUMN_ID));
            mTvDate.setText(cursor.getString(cursor.
                    getColumnIndexOrThrow(NoteTable.COLUMN_DATE)));
            mEtNoteTitle.setText(cursor.getString(cursor.
                    getColumnIndexOrThrow(NoteTable.COLUMN_TITLE)));
            mEtNoteDesc.setText(cursor.getString(cursor.
                    getColumnIndexOrThrow(NoteTable.COLUMN_DESCRIPTION)));
            mEtNoteAddress.setText(cursor.getString(cursor.
                    getColumnIndexOrThrow(NoteTable.COLUMN_ADDRESS)));
            mPhotoFileName = cursor.getString(cursor.getColumnIndexOrThrow(NoteTable.COLUMN_IMAGE));
            if (mPhotoFileName != null) {
                //mImagePath = imagePath;
                mTvImageName.setText("Photo");
                mTvImageName.setVisibility(View.VISIBLE);
            }
            // always close the cursor
            cursor.close();
        }
    }


    public void showPopup(View view) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_layout, null);
        mPopupWindow = new PopupWindow(popupView,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        ImageView imageView = (ImageView) popupView.findViewById(R.id.popupImageView);
        if (mPhotoFileName != null) {
            if (Utility.fileExist(mPhotoFileName)) {
                imageView.setImageBitmap(BitmapFactory.decodeFile(mPhotoFileName));
            } else {
                Toast.makeText(this, "Image not found!", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Image not found!", Toast.LENGTH_LONG).show();
        }
        // PopupWindow should be focusable
        mPopupWindow.setFocusable(true);
        // PopupWindow to dismiss when when touched outside
        mPopupWindow.setBackgroundDrawable(new ColorDrawable());
        int location[] = new int[2];
        view.getLocationOnScreen(location);
        mPopupWindow.showAtLocation(view, Gravity.NO_GRAVITY,
                location[0], location[1] + view.getHeight());
    }

    private void saveNote() {
        String date = mTvDate.getText().toString();
        String title = mEtNoteTitle.getText().toString();
        String description = mEtNoteDesc.getText().toString();
        String address = mEtNoteAddress.getText().toString();


        if (description.length() == 0) {
            description = "No description added";
        }
        if (title.length() == 0) {
            title = "No titile added";
        }
        if (address.length() == 0) {
            address = "No address added";
        }

        ContentValues values = new ContentValues();
        values.put(NoteTable.COLUMN_DATE, date);
        values.put(NoteTable.COLUMN_TITLE, title);
        values.put(NoteTable.COLUMN_DESCRIPTION, description);
        values.put(NoteTable.COLUMN_ADDRESS, address);
        values.put(NoteTable.COLUMN_IMAGE, mPhotoFileName);
        values.put(NoteTable.COLUMN_LATITUDE, Globals.getLocation().getLatitude());
        values.put(NoteTable.COLUMN_LONGITUDE, Globals.getLocation().getLongitude());

        if (noteUri == null) {
            // New note
            noteUri = getContentResolver().insert(NoteContentProvider.CONTENT_URI, values);
        } else {
            // Update note
            getContentResolver().update(noteUri, values, null, null);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note_editor, menu);
        return true;
    }

    private void stat(){
        startActivity(new Intent(this, NoteListActivity.class));
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.editor_upload) {
            if(mPhotoFileName == null){
                hideKeyboard(this);
                Toast.makeText(NoteEditorActivity.this, "No photo available to upload!", Toast.LENGTH_SHORT).show();
            }else{
                uploadPhoto();
            }
            setResult(RESULT_OK);
           // Toast.makeText(NoteEditorActivity.this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show();
            //startActivity(new Intent(this, NoteListActivity.class));
            //finish();
            return true;
        }

        if (id == R.id.editor_save) {
            saveNote();
            setResult(RESULT_OK);
            startActivity(new Intent(this, NoteListActivity.class));
            finish();
            return true;
        }
        else if (id == R.id.editor_delete) {
            if (mNoteTableUid > 0) {
                Uri uri = Uri.parse(NoteContentProvider.CONTENT_URI + "/" + mNoteTableUid);
                getContentResolver().delete(uri, null, null);
                if (mPhotoFileName != null) {
                    Utility.deleteFile(mPhotoFileName);
                }
                setResult(RESULT_OK);
                startActivity(new Intent(this, NoteListActivity.class));
                finish();
            } else {
                // nothing to deal from database. Just return to previous activity.
                finish();
            }
            return true;
        } else if (id == R.id.editor_cancel) {
            // Just go beck to the previous activity
            startActivity(new Intent(this, NoteListActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        /*if(id == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }*/
        if(id == R.id.nav_azure_photo_list)
        {
            Intent intent = new Intent(this, AzurePhotoList.class);
            startActivity(intent);
        }
        else if(id == R.id.nav_location) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_camera) {
            Bundle bundle = new Bundle();
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        } else if (id == R.id.nav_gallery) {
            if(ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                Globals.openGallery(this);
            }else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                        Toast.makeText(this, "External storage permission is required to open Gallery", Toast.LENGTH_LONG).show();
                    }
                    requestPermissions(new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE);
                }else {
                    Globals.openGallery(this);
                }
            }
        } else if (id == R.id.nav_notepad) {
            Intent intent = new Intent(this, NoteEditorActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_noteList) {
            Intent intent = new Intent(this, NoteListActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_share) {
            Globals.shareTextUrl(this);

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE:

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Globals.openGallery(this);
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "External storage read permission is required to open Gallery", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void uploadPhoto()
    {
        try {
            final File photoFile = new File(mPhotoFileName);
            if(photoFile == null){
                Toast.makeText(NoteEditorActivity.this, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }
            final InputStream imageStream = getContentResolver().openInputStream(Uri.fromFile(photoFile));
            final int imageLength = imageStream.available();

            final Handler handler = new Handler();

           new Thread(() -> {
                try {

                    final String imageName = ImageManager.UploadImage(imageStream, imageLength, photoFile.getName());

                    handler.post(new Runnable() {

                        public void run() {
                           Toast.makeText(NoteEditorActivity.this, "Image Uploaded Successfully. Name = " + imageName, Toast.LENGTH_SHORT).show();
                            //Globals.getInstance(NoteEditorActivity.this).sendNotificationButtonOnClick(imageName+" is available in azure cloud!");
                            sendNotificationButtonOnClick(imageName+" is available in azure cloud!");
                        }
                    });
                }
                catch(Exception ex) {
                    final String exceptionMessage = ex.getMessage();
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(NoteEditorActivity.this, exceptionMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
            //th.start();
        }
        catch(Exception ex) {

            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Example code from http://msdn.microsoft.com/library/azure/dn495627.aspx
     * to parse the connection string so a SaS authentication token can be
     * constructed.
     *
     * @param connectionString This must be the DefaultFullSharedAccess connection
     *                         string for this example.
     */
    private void ParseConnectionString(String connectionString)
    {
        String[] parts = connectionString.split(";");
        if (parts.length != 3)
            throw new RuntimeException("Error parsing connection string: "
                    + connectionString);

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("Endpoint")) {
                this.HubEndpoint = "https" + parts[i].substring(11);
            } else if (parts[i].startsWith("SharedAccessKeyName")) {
                this.HubSasKeyName = parts[i].substring(20);
            } else if (parts[i].startsWith("SharedAccessKey")) {
                this.HubSasKeyValue = parts[i].substring(16);
            }
        }
    }


    /**
     * Example code from http://msdn.microsoft.com/library/azure/dn495627.aspx to
     * construct a SaS token from the access key to authenticate a request.
     *
     * @param uri The unencoded resource URI string for this operation. The resource
     *            URI is the full URI of the Service Bus resource to which access is
     *            claimed. For example,
     *            "http://<namespace>.servicebus.windows.net/<hubName>"
     */
    private String generateSasToken(String uri) {

        String targetUri;
        String token = null;
        try {
            targetUri = URLEncoder
                    .encode(uri.toString().toLowerCase(), "UTF-8")
                    .toLowerCase();

            long expiresOnDate = System.currentTimeMillis();
            int expiresInMins = 60; // 1 hour
            expiresOnDate += expiresInMins * 60 * 1000;
            long expires = expiresOnDate / 1000;
            String toSign = targetUri + "\n" + expires;

            // Get an hmac_sha1 key from the raw key bytes
            byte[] keyBytes = HubSasKeyValue.getBytes("UTF-8");
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(toSign.getBytes("UTF-8"));

            // Using android.util.Base64 for Android Studio instead of
            // Apache commons codec
            String signature = URLEncoder.encode(
                    Base64.encodeToString(rawHmac, Base64.NO_WRAP).toString(), "UTF-8");

            // Construct authorization string
            token = "SharedAccessSignature sr=" + targetUri + "&sig="
                    + signature + "&se=" + expires + "&skn=" + HubSasKeyName;
        } catch (Exception e) {
            {
                Toast.makeText(NoteEditorActivity.this, "Exception Generating SaS : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        return token;
    }

    /**
     * Send Notification button click handler. This method parses the
     * DefaultFullSharedAccess connection string and generates a SaS token. The
     * token is added to the Authorization header on the POST request to the
     * notification hub. The text in the editTextNotificationMessage control
     * is added as the JSON body for the request to add a GCM message to the hub.
     */
    public void sendNotificationButtonOnClick(String pushMsg) {
        final String json = "{\"data\":{\"message\":\"" + pushMsg + "\"}}";

        new Thread()
        {
            public void run()
            {
                try
                {
                    // Based on reference documentation...
                    // http://msdn.microsoft.com/library/azure/dn223273.aspx
                    ParseConnectionString(NotificationSettings.HubFullAccessString);
                    URL url = new URL(HubEndpoint + NotificationSettings.HubName +
                            "/messages/?api-version=2015-01");

                    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();

                    try {
                        // POST request
                        urlConnection.setDoOutput(true);

                        // Authenticate the POST request with the SaS token
                        urlConnection.setRequestProperty("Authorization",
                                generateSasToken(url.toString()));

                        // Notification format should be GCM
                        urlConnection.setRequestProperty("ServiceBusNotification-Format", "gcm");

                        // Include any tags
                        // Example below targets 3 specific tags
                        // Refer to : https://azure.microsoft.com/en-us/documentation/articles/notification-hubs-routing-tag-expressions/
                        // urlConnection.setRequestProperty("ServiceBusNotification-Tags",
                        //        "tag1 || tag2 || tag3");

                        // Send notification message
                        urlConnection.setFixedLengthStreamingMode(json.length());
                        OutputStream bodyStream = new BufferedOutputStream(urlConnection.getOutputStream());
                        bodyStream.write(json.getBytes());
                        bodyStream.close();

                        // Get reponse
                        urlConnection.connect();
                        int responseCode = urlConnection.getResponseCode();
                        if ((responseCode != 200) && (responseCode != 201)) {
                            BufferedReader br = new BufferedReader(new InputStreamReader((urlConnection.getErrorStream())));
                            String line;
                            StringBuilder builder = new StringBuilder("Send Notification returned " +
                                    responseCode + " : ")  ;
                            while ((line = br.readLine()) != null) {
                                builder.append(line);
                            }
                            Toast.makeText(NoteEditorActivity.this, builder.toString(), Toast.LENGTH_SHORT).show();
                        }
                    } finally {
                        urlConnection.disconnect();
                    }
                }
                catch(Exception e) {
                    Toast.makeText(NoteEditorActivity.this, "Exception Sending Notification : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }.start();
    }
}
