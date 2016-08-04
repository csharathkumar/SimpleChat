package com.codepath.simplechat;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = ChatActivity.class.getSimpleName();
    static final String USER_ID_KEY = "userId";
    static final String BODY_KEY = "body";
    static final int MAX_CHAT_MESSAGES_TO_SHOW = 50;
    static final int POLL_INTERVAL = 1000; //milliseconds


    EditText etMessage;
    Button btSend;
    ListView lvMessages;
    boolean mFirstLoad;

    ChatListAdapter chatListAdapter;
    List<Message> messages;

    android.os.Handler mHandler = new android.os.Handler();
    Runnable mRefreshMessages = new Runnable() {
        @Override
        public void run() {
            refreshMessages();
            mHandler.postDelayed(this,POLL_INTERVAL);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // User login
        if (ParseUser.getCurrentUser() != null) { // start with existing user
            startWithCurrentUser();
        } else { // If not logged in, login as a new anonymous user
            login();
        }
        mHandler.postDelayed(mRefreshMessages,POLL_INTERVAL);
        etMessage = (EditText) findViewById(R.id.etMessage);
        btSend = (Button) findViewById(R.id.btSend);
        lvMessages = (ListView) findViewById(R.id.lvChat);
        lvMessages.setTranscriptMode(1);
        mFirstLoad = true;
        messages = new ArrayList<>();
        final String userId = ParseUser.getCurrentUser().getObjectId();
        chatListAdapter = new ChatListAdapter(this,userId,messages);
        lvMessages.setAdapter(chatListAdapter);

        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Utils.isNetworkAvailable(ChatActivity.this) && Utils.isOnline()){
                    String data = etMessage.getText().toString();
                    ParseObject message = ParseObject.create("Message");
                    message.put(USER_ID_KEY, ParseUser.getCurrentUser().getObjectId());
                    message.put(BODY_KEY, data);
                    message.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            Toast.makeText(ChatActivity.this, "Successfully created message on Parse",
                                    Toast.LENGTH_SHORT).show();
                            refreshMessages();
                        }
                    });
                    etMessage.setText(null);
                }else{
                    Toast.makeText(ChatActivity.this,"No Internet Connection", Toast.LENGTH_SHORT).show();
                }


            }
        });
        refreshMessages();

    }
    void refreshMessages() {
        if(Utils.isNetworkAvailable(ChatActivity.this) && Utils.isOnline()){
            // Construct query to execute
            ParseQuery<Message> query = ParseQuery.getQuery(Message.class);
            // Configure limit and sort order
            query.setLimit(MAX_CHAT_MESSAGES_TO_SHOW);
            query.orderByDescending("createdAt");
            // Execute query to fetch all messages from Parse asynchronously
            // This is equivalent to a SELECT query with SQL
            query.findInBackground(new FindCallback<Message>() {
                public void done(List<Message> messagesReceived, ParseException e) {
                    if (e == null) {
                        Collections.reverse(messagesReceived);
                        messages.clear();
                        messages.addAll(messagesReceived);
                        chatListAdapter.notifyDataSetChanged(); // update adapter
                        // Scroll to the bottom of the list on initial load
                        if (mFirstLoad) {
                            lvMessages.setSelection(chatListAdapter.getCount() - 1);
                            mFirstLoad = false;
                        }
                    } else {
                        Log.e("message", "Error Loading Messages" + e);
                    }
                }
            });
        }else{
                Toast.makeText(ChatActivity.this,"No Internet Connection", Toast.LENGTH_SHORT).show();

        }


    }

    // Get the userId from the cached currentUser object
    void startWithCurrentUser() {
        // TODO:
    }

    // Create an anonymous user using ParseAnonymousUtils and set sUserId
    void login() {
        ParseAnonymousUtils.logIn(new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Anonymous login failed: ", e);
                } else {
                    startWithCurrentUser();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
