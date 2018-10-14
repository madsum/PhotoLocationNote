package com.home.ma.photolocationnote;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;;
import com.home.ma.photolocationnote.azure.MyHandler;
import com.home.ma.photolocationnote.azure.NotificationSettings;
import com.home.ma.photolocationnote.azure.RegistrationIntentService;
import com.home.ma.photolocationnote.utility.Globals;
import com.microsoft.windowsazure.notifications.NotificationsManager;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private ZoomControls zoom;
    private Button satView;
    private Button clear;
    private Double mLatitude = null;
    private Double mLongitude = null;
    private LocationRequest mLocationRequest;
    private EditText etLocationEntry;

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLastLocation;
    LocationCallback mLocationCallback;

    public static final String TAG = "photoLocationNote";


    /**
     * Constant used in the location settings dialog.
     */
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // All toolbar and drawer code.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // Map fragment code
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // check is it connected to wifi or mobile data
        if( isConnected(this)){
            // initialise map
            mapUIInitialise();

            // Register for push notification.
            NotificationsManager.handleNotifications(this, NotificationSettings.SenderId, MyHandler.class);
            registerWithNotificationHubs();
        }else{
            showDataSettingDialog();
            //finish();
        }
        setTitle("Current Location");
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, Globals.PLAY_SERVICES_RESOLUTION_REQUEST)
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

    private void startLocationUpdates() {
        mFusedLocationClient = new FusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location lastLocation = null;

                for (Location location : locationResult.getLocations()) {

                    if (lastLocation != null) {
                        if (location.getTime() < lastLocation.getTime()) {
                            lastLocation = location;
                        }
                    } else {
                        lastLocation = location;
                    }
                }

                stopLocationUpdates();
            }
        };
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        mFusedLocationClient = null;
        mLocationRequest = null;
        mLocationCallback = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!checkPermissions()) {
            Log.i(TAG, "Inside onStart function; requesting permission when permission is not available");
            requestPermissions();
        } else {
            Log.i(TAG, "Inside onStart function; getting location when permission is already available");
            startLocationUpdates();
            getLastLocation();
        }
    }


    //Return whether permissions is needed as boolean value.
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    //Request permission from user
    private void requestPermissions() {
        Log.i(TAG, "Inside requestPermissions function");
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        //Log an additional rationale to the user. This would happen if the user denied the
        //request previously, but didn't check the "Don't ask again" checkbox.
        // In case you want, you can also show snackbar. Here, we used Log just to clear the concept.
        if (shouldProvideRationale) {
            Log.i(TAG, "****Inside requestPermissions function when shouldProvideRationale = true");
            startLocationPermissionRequest();
        } else {
            Log.i(TAG, "****Inside requestPermissions function when shouldProvideRationale = false");
            startLocationPermissionRequest();
        }
    }

    //Start the permission request dialog
    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MapsActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                Globals.REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLastLocation = task.getResult();
                            mLatitude =  mLastLocation.getLatitude();
                            mLongitude = mLastLocation.getLongitude();

                            LatLng myPlace = new LatLng(mLatitude, mLongitude);
                            mMap.addMarker(new MarkerOptions().position(myPlace).title("Marker in current"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(myPlace));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace, 18.0f));
                            mMap.setMyLocationEnabled(true);
                            Globals.getInstance().setLocation(mLastLocation);
                            // as soon as we get location, I tired to find local address
                            getFullAddress();
                        } else {
                            Log.i(TAG, "Inside getLocation function. Error while getting location");
                            System.out.println(TAG+task.getException());
                        }
                    }
                });
    }

    public  boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiInfo != null && wifiInfo.isConnected()) || (mobileInfo != null && mobileInfo.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private  void showDataSettingDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please connect to internet. Then restart the app.")
                .setCancelable(false)
                .setPositiveButton("WIFI enable", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        finish();
                    }
                })
                .setNegativeButton("Mobile data enable", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }


    private void mapUIInitialise(){
        zoom = (ZoomControls) findViewById(R.id.zcZoom);
        zoom.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());

            }
        });
        zoom.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());

            }
        });

        etLocationEntry = (EditText) findViewById(R.id.etLocationEntry);
        // serach icon in EditText
        etLocationEntry.setHint("\uD83D\uDD0D Search");

        etLocationEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String search = etLocationEntry.getText().toString();
                    etLocationEntry.getText().clear();
                    // hide the soft keyborad after search button click.
                    InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.hideSoftInputFromWindow(etLocationEntry.getWindowToken(), 0);

                    if (search != null && !search.equals("")) {
                        List<Address> addressList = null;
                        Geocoder geocoder = new Geocoder(MapsActivity.this);
                        //Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.getDefault());
                        try {
                            addressList = geocoder.getFromLocationName(search, 5);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (addressList.size() == 0) {
                            Toast.makeText(getApplicationContext(), "Location not found", Toast.LENGTH_LONG).show();
                            return true;
                        }

                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                        mMap.addMarker(new MarkerOptions().position(latLng).title("from geo coder"));
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(latLng.latitude, latLng.longitude)));
                    }
                    return true;
                }
                return false;
            }
        });

        satView = (Button) findViewById(R.id.btSatellite);
        satView.setOnClickListener(view -> {
            if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                satView.setText("NORM");
            } else {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                satView.setText("SAT");
            }

        });

        clear = (Button) findViewById(R.id.btClear);
        clear.setOnClickListener(view -> mMap.clear());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(Globals.TAG, "User agreed to make required location settings changes.");
                        getLastLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(Globals.TAG, "User chose not to make required location settings changes.");
                        Toast.makeText(this, "Current address won't be located", Toast.LENGTH_LONG).show();
                        break;
                }
                break;
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMapClickListener(latLng -> {
            mMap.addMarker(new MarkerOptions().position(latLng).title("On map click"));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        });

        // Add a marker in Espoo and move the camera
        LatLng myPlace = new LatLng(60.205490, 24.655899);
        mMap.addMarker(new MarkerOptions().position(myPlace).title("Marker in Espoo"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myPlace));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace, 15.0f));
        mMap.setOnMarkerClickListener(this);
    }

    private void enableMyLocationIfPermitted() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    Globals.LOCATION_PERMISSION_REQUEST_CODE);
        } else if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }


    /**
     * Callback to the following function is received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Globals.LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocationIfPermitted();
                } else {
                    showDefaultLocation();
                }
                break;
            case Globals.MY_REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE:

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

    private void showDefaultLocation() {
        Toast.makeText(this, "Location permission not granted, " +
                        "showing default location",
                Toast.LENGTH_SHORT).show();
        LatLng espoo = new LatLng(60.205490, 24.655899);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(espoo));
    }


    private void getFullAddress() {
        List<Address> addresses = null;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            // Here 1 represent max location result to returned
            addresses = geocoder.getFromLocation(mLatitude, mLongitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        String address = addresses.get(0).getAddressLine(0);
        if(StringUtils.isNotEmpty(address)){
            Globals.setTotalAddress(address);
        }else{
            Globals.setTotalAddress("No address");
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            stopLocationUpdates();

    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
        getLastLocation();

    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        /*if(id == R.id.nav_home)
        {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }*/
        if(id == R.id.nav_azure_photo_list)
        {
            Intent intent = new Intent(this, AzurePhotoList.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_camera) {
            Bundle bundle = new Bundle();
            // this start camera for still photo
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
                    requestPermissions(new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, Globals.MY_REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE);
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
}
