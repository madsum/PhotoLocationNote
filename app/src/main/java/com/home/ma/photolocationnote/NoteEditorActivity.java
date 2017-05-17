package com.home.ma.photolocationnote;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.home.ma.photolocationnote.utility.Utility;

import java.util.Calendar;

public class NoteEditorActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private TextView mTvImageName;
    private EditText mEtNoteAddress;
    private EditText mEtNoteTitle;
    private EditText mEtNoteDesc;
    private TextView mTvDate;
    private String mPhotoFileName = null;
    PopupWindow mPopupWindow = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initializeEditor(savedInstanceState);
    }

    public void initializeEditor(Bundle savedInstanceState) {

        Bundle extras = getIntent().getExtras();

        if( extras != null){
            mPhotoFileName  =   extras.getString(Globals.PHOTO_FILE_KEY);
        }

        mTvImageName = (TextView) findViewById(R.id.tvImageName);
        if (mPhotoFileName != null) {
            mTvImageName.setText("New photo");
            mTvImageName.setVisibility(View.VISIBLE);
        }

        mEtNoteAddress = (EditText) findViewById(R.id.etNoteAddress);
        if (mPhotoFileName != null) {
            mEtNoteAddress.setText(Globals.getTotalAddress());
            mEtNoteAddress.setVisibility(View.VISIBLE);
        }

        mEtNoteTitle = (EditText) findViewById(R.id.etNoteTitle);
        mEtNoteDesc = (EditText) findViewById(R.id.etNoteDesc);
        mTvDate = (TextView) findViewById(R.id.tvDate);

        // a new note so we should set current date
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        String date = Integer.toString(day) + "/" + Integer.toString(month) + "/" + Integer.toString(year);
        mTvDate.setText(date);
    }

    public void showPopup(View view) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_layout, null);
        mPopupWindow = new PopupWindow(popupView,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        // Example: If you have a TextView inside `popup_layout.xml`
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
        // If the PopupWindow should be focusable
        mPopupWindow.setFocusable(true);
        // If you need the PopupWindow to dismiss when when touched outside
        mPopupWindow.setBackgroundDrawable(new ColorDrawable());
        int location[] = new int[2];
        // Get the View's(the one that was clicked in the Fragment) location
        view.getLocationOnScreen(location);
        // Using location, the PopupWindow will be displayed right under anchorView
        mPopupWindow.showAtLocation(view, Gravity.NO_GRAVITY,
                location[0], location[1] + view.getHeight());
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_location) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.nav_home)
        {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_camera) {
            Bundle bundle = new Bundle();
            // this start camera for still photo
            //bundle.putInt(NoteTable.COLUMN_IMAGE, CameraActivity.MEDIA_TYPE_IMAGE);
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        } else if (id == R.id.nav_gallery) {
            Intent intent = new Intent(this, GalleryActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
