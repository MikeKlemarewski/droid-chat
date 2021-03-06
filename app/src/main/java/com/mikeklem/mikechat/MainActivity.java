package com.mikeklem.mikechat;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {

    public static final String TAG = "MainActivity";

    public static final int TAKE_PHOTO_REQUEST = 0;
    public static final int TAKE_VIDEO_REQUEST = 1;
    public static final int CHOOSE_PHOTO_REQUEST = 2;
    public static final int CHOOSE_VIDEO_REQUEST = 3;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public static final int FILE_SIZE_LIMIT = 1024 * 1024 * 10; // 10MB

    protected Uri mMediaUri;
    protected ParseUser mCurrentUser;

    /**
     * The PagerAdapter that will provide
     * fragments for each of the sections. We use a
     * FragmentPagerAdapter derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a FragmentStatePagerAdapter.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    protected DialogInterface.OnClickListener mDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch(i) {
                case TAKE_PHOTO_REQUEST:
                    Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

                    // We failed to create a file
                    if (mMediaUri == null) {
                        Log.e(TAG, "Problem accessing storage");
                        break;
                    }

                    // Tells the device where to store the result
                    photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
                    startActivityForResult(photoIntent, TAKE_PHOTO_REQUEST);
                    break;
                case TAKE_VIDEO_REQUEST:
                    Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);

                    // We failed to create a file
                    if (mMediaUri == null) {
                        Log.e(TAG, "Problem accessing storage");
                        break;
                    }

                    videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
                    videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
                    videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                    startActivityForResult(videoIntent, TAKE_VIDEO_REQUEST);
                    break;
                case CHOOSE_PHOTO_REQUEST:
                    Intent choosePhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    choosePhotoIntent.setType("image/*");
                    startActivityForResult(choosePhotoIntent, CHOOSE_PHOTO_REQUEST);
                    break;
                case CHOOSE_VIDEO_REQUEST:
                    Intent chooseVideoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    chooseVideoIntent.setType("video/*");
                    startActivityForResult(chooseVideoIntent, CHOOSE_VIDEO_REQUEST);
                    break;
            }
        }
    };

    private Uri getOutputMediaFileUri(int mediaType) {
        if (! isExternalStorageAvailable()) {
            return null;
        }
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "Failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (mediaType == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(mediaType == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return Uri.fromFile(mediaFile);

    }

    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CHOOSE_PHOTO_REQUEST || requestCode == CHOOSE_VIDEO_REQUEST) {
                if (data == null) {

                } else {
                    mMediaUri = data.getData();
                }
                if (requestCode == CHOOSE_VIDEO_REQUEST) {
                    int fileSize = 0;
                    InputStream stream = null;
                    try {
                        stream = getContentResolver().openInputStream(mMediaUri);
                        fileSize = stream.available();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    } finally {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            // Do nothing
                        }
                    }
                    Log.d(TAG, "FileSize: " + fileSize);
                    if (fileSize >= FILE_SIZE_LIMIT) {
                        Toast.makeText(this, getString(R.string.error_file_to_big), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            } else {
                // Add to gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(mMediaUri);
                sendBroadcast(mediaScanIntent);
            }

            Intent intent = new Intent(MainActivity.this, RecipientsActivity.class);
            intent.setData(mMediaUri);
            String fileType;
            if (requestCode == TAKE_PHOTO_REQUEST || requestCode == CHOOSE_PHOTO_REQUEST) {
                fileType = ParseConstants.TYPE_IMAGE;
            }
            else {
                fileType = ParseConstants.TYPE_VIDEO;
            }
            intent.putExtra(ParseConstants.KEY_FILE_TYPE, fileType);
            startActivity(intent);
        }
        else if (resultCode != RESULT_CANCELED) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_LONG).show();
            Log.e(TAG, getString(R.string.error));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        mCurrentUser = ParseUser.getCurrentUser();
        if (mCurrentUser == null) {
            navigateToLogin();
        }


        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_logout:
                ParseUser.logOut();
                navigateToLogin();
                return true;
            case R.id.action_edit_friends:
                Intent intent = new Intent(this, EditFriendsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_camera:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setItems(R.array.camera_choices, mDialogListener)
                       .create().show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

}
