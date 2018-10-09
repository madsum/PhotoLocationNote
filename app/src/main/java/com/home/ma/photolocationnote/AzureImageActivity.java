package com.home.ma.photolocationnote;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.home.ma.photolocationnote.azure.ImageManager;
import com.home.ma.photolocationnote.utility.Globals;
import com.home.ma.photolocationnote.utility.NVP;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class AzureImageActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private ImageView imageView;
    private ArrayList<NVP> imageValuePairs;
    private Integer position;
    private final static int MY_REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_azure_image_acitivity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        String selectedImage = this.getIntent().getStringExtra("selectedImage");
        imageValuePairs = (ArrayList<NVP>) this.getIntent().getSerializableExtra("nvp");
        position =  this.getIntent().getIntExtra("postion", 0);

        imageView = findViewById(R.id.ChildImageView);
        imageView.setOnClickListener(v -> {
            if(imageValuePairs != null){
                try {
                    loadImage(imageValuePairs.get(position+1).getName());
                    position = position+1;
                }catch (Exception e){
                    loadImage(imageValuePairs.get(0).getName());
                    position = 0;
                }
            }
        });
        loadImage(selectedImage);

    }

    private void loadImage(String selectedImage){
        final ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        final Handler handler = new Handler();

        Thread th = new Thread(new Runnable() {
            public void run() {

                try {

                    long imageLength = 0;

                    ImageManager.GetImage(selectedImage, imageStream, imageLength);

                    handler.post(new Runnable() {

                        public void run() {
                            byte[] buffer = imageStream.toByteArray();

                            Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);

                            imageView.setImageBitmap(bitmap);
                        }
                    });
                }
                catch(Exception ex) {
                    final String exceptionMessage = ex.getMessage();
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(AzureImageActivity.this, exceptionMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }});
        th.start();
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
        if (id == R.id.nav_location) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_camera) {
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

        }else if (id == R.id.nav_noteList) {
            Intent intent = new Intent(this, NoteListActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {
            Globals.shareTextUrl(this);
        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
