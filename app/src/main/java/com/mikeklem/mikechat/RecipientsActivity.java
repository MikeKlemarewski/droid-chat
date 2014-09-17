package com.mikeklem.mikechat;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;


public class RecipientsActivity extends ListActivity {

    protected ParseUser mCurrentUser;
    protected ParseRelation<ParseUser> mFriendsRelation;
    protected List<ParseUser> mFriends;

    protected Uri mMediaUri;
    protected MenuItem mSendItem;
    protected String mFileType;

    private static final String TAG = "RecipientsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_recipients);

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mMediaUri = getIntent().getData();
        mFileType = getIntent().getExtras().getString(ParseConstants.KEY_FILE_TYPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCurrentUser = ParseUser.getCurrentUser();
        mFriendsRelation = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);

        setProgressBarIndeterminateVisibility(true);

        ParseQuery<ParseUser> query = mFriendsRelation.getQuery();
        query.addAscendingOrder(ParseConstants.KEY_USERNAME);
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> friends, ParseException e) {
                setProgressBarIndeterminateVisibility(false);
                if(e != null) {
                    Log.e(TAG, e.getMessage());
                    return;
                }

                mFriends = friends;
                String[] usernames = new String[mFriends.size()];

                int i = 0;
                for (ParseUser friend : mFriends) {
                    usernames[i] = friend.getUsername();
                    i++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        getListView().getContext(),
                        android.R.layout.simple_list_item_checked,
                        usernames
                );
                setListAdapter(adapter);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.recipients, menu);
        mSendItem = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_send) {
            ParseObject message = createMessage();

            if (message == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.error_file_send))
                       .setTitle(getString(R.string.error))
                       .setPositiveButton(android.R.string.ok, null)
                       .create().show();
            }

            send(message);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void send(ParseObject message) {
        message.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
                    builder.setMessage(getString(R.string.error_message_send))
                            .setTitle(getString(R.string.error))
                            .setPositiveButton(android.R.string.ok, null)
                            .create().show();
                }
                Toast.makeText(RecipientsActivity.this, getString(R.string.message_sent),Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        mSendItem.setVisible(true);
        if (getListView().getCheckedItemCount() == 0) {
            mSendItem.setVisible(false);
        }
    }

    protected ParseObject createMessage() {
        ParseObject message = new ParseObject(ParseConstants.CLASS_MESSAGE);
        message.put(ParseConstants.KEY_SENDER_ID, mCurrentUser.getObjectId());
        message.put(ParseConstants.KEY_SENDER_NAME, mCurrentUser.getUsername());
        message.put(ParseConstants.KEY_RECIPIENT_IDS, getRecipientIds());

        message.put(ParseConstants.KEY_FILE_TYPE, mFileType);

        byte[] fileBytes = FileHelper.getByteArrayFromFile(this, mMediaUri);

        if (fileBytes == null) {
            return null;
        }
        if (mFileType.equals(ParseConstants.TYPE_IMAGE)) {
            fileBytes = FileHelper.reduceImageForUpload(fileBytes);
        }

        String fileName = FileHelper.getFileName(this, mMediaUri, mFileType);
        ParseFile file = new ParseFile(fileName, fileBytes);
        message.put(ParseConstants.KEY_FILE, file);

        return message;
    }

    protected ArrayList<String> getRecipientIds() {
        ArrayList<String> recipientIds = new ArrayList<String>();
        for (int i = 0; i < getListView().getCount(); i++) {
            if (getListView().isItemChecked(i)) {
                recipientIds.add(mFriends.get(i).getObjectId());
            }
        }
        return recipientIds;
    }
}
