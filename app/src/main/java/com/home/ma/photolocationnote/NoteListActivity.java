package com.home.ma.photolocationnote;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import com.home.ma.photolocationnote.azure.MyHandler;
import com.home.ma.photolocationnote.azure.NotificationSettings;
import com.home.ma.photolocationnote.azure.RegistrationIntentService;
import com.home.ma.photolocationnote.contentProvider.NoteContentProvider;
import com.home.ma.photolocationnote.database.NoteDatabaseHelper;
import com.home.ma.photolocationnote.database.NoteTable;
import com.home.ma.photolocationnote.utility.Utility;
import com.microsoft.windowsazure.notifications.NotificationsManager;

import java.io.File;

import static java.security.AccessController.getContext;

public class NoteListActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private static final int DELETE_ID = Menu.FIRST + 1;
    private SimpleCursorAdapter mAdapter;
    private ListView mListView;
    private final static int MY_REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 102;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private Globals globals = Globals.getInstance(this);
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
        menu.add(0, DELETE_ID, 0, "delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

        if(id == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
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
        finish();
    }
}
