package com.home.ma.photolocationnote;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.home.ma.photolocationnote.azure.ImageManager;
import com.home.ma.photolocationnote.azure.MyHandler;
import com.home.ma.photolocationnote.azure.NotificationSettings;
import com.home.ma.photolocationnote.azure.RegistrationIntentService;
import com.home.ma.photolocationnote.database.NoteContentProvider;
import com.home.ma.photolocationnote.database.NoteDatabaseHelper;
import com.home.ma.photolocationnote.database.NoteTable;
import com.home.ma.photolocationnote.utility.Globals;
import com.home.ma.photolocationnote.utility.Utility;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.notifications.NotificationsManager;
import com.squareup.okhttp.OkHttpClient;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class NoteListActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private static final int UPLOAD_ID = Menu.FIRST + 1;
    private static final int DELETE_ID = Menu.FIRST + 2;
    private SimpleCursorAdapter mAdapter;
    private ListView mListView;
    private MobileServiceClient mClient;
    private MobileServiceTable<PhotoLocationNote> mToDoTable;
    private final static int MY_REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 102;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private String HubEndpoint = null;
    private String HubSasKeyName = null;
    private String HubSasKeyValue = null;
    private ProgressDialog pd;
    public static final String TAG = "photoLocationNote";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mListView = (ListView) findViewById(R.id.note_list);
        mListView.setDividerHeight(1);
        registerForContextMenu(mListView);
        mListView.setOnItemClickListener(this);

        fillData();

        // Register for push notification.
        NotificationsManager.handleNotifications(this, NotificationSettings.SenderId, MyHandler.class);
        registerWithNotificationHubs();

        initializeAzureMobileClient();
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

    public void initializeAzureMobileClient(){
        try {
            mClient = new MobileServiceClient(
                    "https://photolocationmobileapp.azurewebsites.net",
                    this);//.withFilter(new NoteEditorActivity.ProgressFilter());

            // Extend timeout from default of 10s to 20s
            mClient.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(20, TimeUnit.SECONDS);
                    client.setWriteTimeout(20, TimeUnit.SECONDS);
                    return client;
                }
            });
            mToDoTable = mClient.getTable(PhotoLocationNote.class);
        }catch (MalformedURLException e) {
            Toast.makeText(NoteListActivity.this, "There was an error creating the Mobile Service. Verify the URL", Toast.LENGTH_LONG ).show();
        } catch (Exception e){
            Toast.makeText(NoteListActivity.this, "Error", Toast.LENGTH_LONG ).show();
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
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, UPLOAD_ID, 0, "upload");
        menu.add(0, DELETE_ID, 0, "delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case UPLOAD_ID:
                AdapterView.AdapterContextMenuInfo info2 = (AdapterView.AdapterContextMenuInfo) item
                        .getMenuInfo();
                startProgressDialog();
                getSelectedItem(info2.id);
                return true;

            case DELETE_ID:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                        .getMenuInfo();
                Uri uri = Uri.parse(NoteContentProvider.CONTENT_URI + "/" + info.id);
                getContentResolver().delete(uri, null, null);
                fillData();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void getSelectedItem(long id){
        String[] projection    = new String[] {NoteTable.COLUMN_TITLE, NoteTable.COLUMN_DESCRIPTION,
                                               NoteTable.COLUMN_ADDRESS, NoteTable.COLUMN_DATE,
                                               NoteTable.COLUMN_IMAGE, NoteTable.COLUMN_LATITUDE,
                                               NoteTable.COLUMN_LONGITUDE};
        Uri uri = Uri.parse(NoteContentProvider.CONTENT_URI + "/" +String.valueOf(id) );
        Cursor cursor = getContentResolver().query(uri, projection, null,
                                    null, null);

        if(cursor.moveToFirst()){
            String title = cursor.getString(cursor.getColumnIndex(NoteTable.COLUMN_TITLE));
            String description = cursor.getString(cursor.getColumnIndex(NoteTable.COLUMN_DESCRIPTION));
            String date = cursor.getString(cursor.getColumnIndex(NoteTable.COLUMN_DATE));
            String address = cursor.getString(cursor.getColumnIndex(NoteTable.COLUMN_ADDRESS));
            String image = cursor.getString(cursor.getColumnIndex(NoteTable.COLUMN_IMAGE));
            double latitude = cursor.getDouble(cursor.getColumnIndex(NoteTable.COLUMN_LATITUDE));
            double longitude = cursor.getDouble(cursor.getColumnIndex(NoteTable.COLUMN_LATITUDE));
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(NoteTable.COLUMN_DATE, date);
            values.put(NoteTable.COLUMN_TITLE, title);
            values.put(NoteTable.COLUMN_DESCRIPTION, description);
            values.put(NoteTable.COLUMN_ADDRESS, address);
            values.put(NoteTable.COLUMN_IMAGE, image);
            values.put(NoteTable.COLUMN_LATITUDE, latitude);
            values.put(NoteTable.COLUMN_LONGITUDE, longitude);

            PhotoLocationNote photoLocationNote = new PhotoLocationNote();
            photoLocationNote.setData(values);
            saveInAzureDb(photoLocationNote);
        }
    }

    private void saveInAzureDb(PhotoLocationNote photoLocationNote){
        final boolean isImageExist = photoLocationNote.getImage() != null ?  true : false;
        File photoFile = null;
        if(isImageExist){
            photoFile = new File(photoLocationNote.getImage());
        }
        if(photoFile == null){
            Toast.makeText(NoteListActivity.this, "Photo not found", Toast.LENGTH_SHORT).show();
            //return;
        }
        InputStream imageStream = null;
        int imageLength = 0;
        try {
            if(photoFile != null){
                imageStream = new FileInputStream(photoFile);
                imageLength = imageStream.available();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //final InputStream imageStream = getContentResolver().openInputStream(photoFile);
        //final int imageLength = imageStream.available();
        InputStream finalImageStream = imageStream;
        int finalImageLength = imageLength;
        File finalPhotoFile = photoFile;
        String imageName = null;
        String finalImageName = imageName;
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final PhotoLocationNote entity = addItemInTable(photoLocationNote);
                    if(finalImageStream != null){
                        ImageManager.UploadImage(finalImageStream, finalImageLength, finalPhotoFile.getName());
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if( finalPhotoFile != null){
                                Toast.makeText(NoteListActivity.this, finalPhotoFile.getName()+" is available in azure cloud!", Toast.LENGTH_LONG).show();
                                sendNotificationButtonOnClick(finalPhotoFile.getName()+" is available in azure cloud!");
                            }else{
                                Toast.makeText(NoteListActivity.this, "New note data is available in azure cloud!", Toast.LENGTH_LONG).show();
                                sendNotificationButtonOnClick("New note data is available in azure cloud!");
                            }

                            stopProgressDialog();

                        }
                    });
                } catch (final Exception e) {
                    System.out.println("error");
                }
                return null;
            }
        };
        task.execute();
    }

    public PhotoLocationNote addItemInTable(PhotoLocationNote item) throws ExecutionException, InterruptedException {
        return mToDoTable.insert(item).get();
    }

    private void startProgressDialog(){
        pd = new ProgressDialog(NoteListActivity.this);
        pd.setTitle("Uploding...");
        pd.setMessage("Please wait.");
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
    }

    private void stopProgressDialog(){
        if (pd!=null) {
            pd.dismiss();
        }
    }


    private void fillData() {
        String[] from = new String[]{NoteTable.COLUMN_TITLE};
        // Fields on the UI to which we map
        int[] to = new int[]{R.id.listItemTvTitle};
        Log.i(Globals.TAG, "NoteListActivity: getLoaderManager().initLoader called!");
        getSupportLoaderManager().initLoader(0, null, this);
        mAdapter = new SimpleCursorAdapter(this, R.layout.note_list_item, null, from, to, 0);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {NoteTable.COLUMN_ID, NoteTable.COLUMN_TITLE};
        // last parameter is how to sort query
        // column_name DESC OR column_name ASC
        android.support.v4.content.CursorLoader  cursorLoader = new android.support.v4.content.CursorLoader(this,
                NoteContentProvider.CONTENT_URI, projection, null, null, NoteTable.COLUMN_ID+" DESC");
        Log.i(Globals.TAG, "NoteListActivity: onCreateLoader called!");
        return cursorLoader;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.list_sync) {
            return true;
        }else if( id == R.id.delete_all_list){

            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Confirmation");
            adb.setMessage("Do you really want to delete all notes?");
            adb.setIcon(R.mipmap.ic_warning_black_24dp);
            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    NoteDatabaseHelper databaseHelper = new NoteDatabaseHelper(NoteListActivity.this);
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();
                    try {
                        Cursor  cursor = db.rawQuery("select * from "+NoteTable.TABLE_NOTE, null);
                        try {
                            // looping through all rows and delete all file from filesystem
                            if (cursor.moveToFirst()) {
                                do {
                                    String imagePath =cursor.getString(cursor.getColumnIndex(NoteTable.COLUMN_IMAGE));
                                    Utility.deleteFile(imagePath);
                                } while (cursor.moveToNext());
                            }

                        } finally {
                            try {
                                cursor.close();
                                db.execSQL("delete from "+ NoteTable.TABLE_NOTE);
                            }
                            catch (Exception ignore)
                            {
                                Log.i(Globals.TAG, "exception");
                            }
                        }
                    } finally {
                        try {
                            db.close();
                        } catch (Exception ignore)
                        {
                            Log.i(Globals.TAG, "exception");
                        }
                    }
                    //reload the activity
                    finish();
                    startActivity(getIntent());
                }
            });
            adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                } });
            adb.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

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

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
        Log.i(Globals.TAG, "NoteListActivity: onLoadFinished called!");
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        Uri noteUri = Uri.parse(NoteContentProvider.CONTENT_URI + "/" + id);
        intent.putExtra(NoteContentProvider.CONTENT_ITEM_TYPE, noteUri);
        startActivity(intent);
        //finish();
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
                Toast.makeText(NoteListActivity.this, "Exception Generating SaS : " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(NoteListActivity.this, builder.toString(), Toast.LENGTH_SHORT).show();
                        }
                    } finally {
                        urlConnection.disconnect();
                    }
                }
                catch(Exception e) {
                    Toast.makeText(NoteListActivity.this, "Exception Sending Notification : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }.start();
    }


    private class PhotoLocationNote{
        @com.google.gson.annotations.SerializedName("id")
        public String id;

        @com.google.gson.annotations.SerializedName("title")
        public String title;

        @com.google.gson.annotations.SerializedName("description")
        public String description;

        @com.google.gson.annotations.SerializedName("date")
        public String date;

        @com.google.gson.annotations.SerializedName("latitude")
        public double latitude;

        @com.google.gson.annotations.SerializedName("longitude")
        public double longitude;

        @com.google.gson.annotations.SerializedName("address")
        public String address;

        @com.google.gson.annotations.SerializedName("image")
        public String image;

        public void setData(ContentValues values){
            title = values.getAsString(NoteTable.COLUMN_TITLE);
            description = values.getAsString(NoteTable.COLUMN_DESCRIPTION);
            date = values.getAsString(NoteTable.COLUMN_DATE);
            latitude = values.getAsDouble(NoteTable.COLUMN_LATITUDE);
            longitude = values.getAsDouble(NoteTable.COLUMN_LONGITUDE);
            address = values.getAsString(NoteTable.COLUMN_ADDRESS);
            image = values.getAsString(NoteTable.COLUMN_IMAGE);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    };
}
