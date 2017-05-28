package com.home.ma.photolocationnote;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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

import com.home.ma.photolocationnote.contentProvider.NoteContentProvider;
import com.home.ma.photolocationnote.database.NoteTable;
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
    private PopupWindow mPopupWindow = null;
    private Uri noteUri;
    private double mLongitude = 0;
    private double mLatitude = 0;
    private int mNoteTableUid = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

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
                }
                // we should set current date
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
            } //else {
            // Toast.makeText(this, "No image found for this note", Toast.LENGTH_LONG).show();
            //}
            // always close the cursor
            cursor.close();
        }
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

    private void saveNote() {
        String date = mTvDate.getText().toString();
        String title = mEtNoteTitle.getText().toString();
        String description = mEtNoteDesc.getText().toString();
        String address = mEtNoteAddress.getText().toString();


        // only save if either summary or description is available
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

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
                // nothing to delte from database. Just return to previous activity.
                finish();
            }
            return true;
        } else if (id == R.id.editor_cancel) {
            // Just go beck to the previous activity
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
            Intent intent = new Intent(this, GalleryActivity.class);
            startActivity(intent);

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
}
