package in.letsecho.echoapp;

import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.twitter.sdk.android.core.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.letsecho.echoapp.library.Group;
import in.letsecho.echoapp.library.UserConnection;
import in.letsecho.library.ChatMessage;
import in.letsecho.echoapp.library.UserProfile;

import static in.letsecho.echoapp.R.id.photoImageView;
import static in.letsecho.echoapp.library.EntityDisplayModel.GROUP_TYPE;
import static in.letsecho.echoapp.library.EntityDisplayModel.USER_TYPE;
import static in.letsecho.echoapp.library.UserConnection.REQUEST_RECEIVED;
import static in.letsecho.echoapp.library.UserConnection.REQUEST_SENT;
import static java.lang.Boolean.TRUE;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;
    private static final int CHAT_USER = 1;
    private static final int CHAT_GROUP = 2;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private Toolbar mToolbar;
    private MenuItem mMembers, mDelete;

    private FirebaseUser mCurrentUser;
    private String mSecondaryUid, mChatId, mGroupId;
    private Group mGroup;
    private UserProfile mSecondaryUser;
    private int mChatType;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mChatsDbRef, mOtherEntityDbRef, mUserConnectionRootDbRef;
    private DatabaseReference mCurrentUserChatDbRef, mCurrentChatDbRef;
    private ChildEventListener mMessageEventListener;
    private ValueEventListener mCurrentUserChatsEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mChatsDbRef = mFirebaseDatabase.getReference().child("chats");
        mUserConnectionRootDbRef = mChatsDbRef.child("user_connections");
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize message ListView and its adapter
        List<ChatMessage> chatMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, chatMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        mToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open Profile
                if (mChatType == CHAT_USER) {
                    DialogFragment profileDialog = new UserProfileFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("secondaryUserId", mSecondaryUser.getUID());
                    profileDialog.setArguments(bundle);
                    profileDialog.show(getFragmentManager(), "userprofile");
                }
                else if (mChatType == CHAT_GROUP) {
                    DialogFragment profileDialog = new GroupProfileFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("groupId", mGroup.getId());
                    profileDialog.setArguments(bundle);
                    profileDialog.show(getFragmentManager(), "groupprofile");
                }
            }
        });

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
                mFirebaseAnalytics.logEvent(getString(R.string.send_photo_event), new Bundle());
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                ChatMessage chatMessage = new ChatMessage(mMessageEditText.getText().toString(),
                        mCurrentUser.getDisplayName(), mCurrentUser.getUid(), null, new HashMap());
                if(mChatType == CHAT_GROUP && mGroupId != null) {
                    chatMessage.setGroupId(mGroupId);
                    chatMessage.setGroupName(mGroup.getTitle());
                }
                mCurrentChatDbRef.push().setValue(chatMessage);
                // Clear input box
                mMessageEditText.setText("");
                mFirebaseAnalytics.logEvent(getString(R.string.send_message_event), new Bundle());
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mAuthStateListener == null) {
            mAuthStateListener = getAuthStateListener();
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }

        // Initialize Intent
        Intent intent = getIntent();
        if (intent != null) {
            if(intent.hasExtra("CHAT_USER")) {
                mChatType = CHAT_USER;
                mSecondaryUid = intent.getStringExtra("CHAT_USER");
                mOtherEntityDbRef = mFirebaseDatabase.getReference().child("users").child(mSecondaryUid);
            } else if(intent.hasExtra("CHAT_GROUP")) {
                mChatType = CHAT_GROUP;
                mGroupId = intent.getStringExtra("CHAT_GROUP");
                mOtherEntityDbRef = mFirebaseDatabase.getReference().child("groups").child(mGroupId);
            }
            if(intent.hasExtra("TITLE"))
                mToolbar.setTitle(intent.getStringExtra("TITLE"));
        } else {
            Intent mainIntent = new Intent(this.getApplicationContext(), MainActivity.class);
            startActivity(mainIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
            mAuthStateListener = null;
        }
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        if(mChatType == CHAT_GROUP) {
            mMembers = menu.findItem(R.id.member_list);
            mMembers.setVisible(TRUE);
            mDelete = menu.findItem(R.id.delete_chat);
            mDelete.setTitle("Unfollow");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.member_list:
                Intent intent = new Intent(this, GroupMemberActivity.class)
                        .putExtra("CHAT_ID", mChatId)
                        .putExtra("GROUP_ID", mGroupId)
                        .putExtra("TITLE", mToolbar.getTitle());
                startActivity(intent);
                return true;
            case R.id.delete_chat:
                if(mChatType == CHAT_GROUP)
                    deleteGroupChat();
                else if(mChatType == CHAT_USER)
                    deleteUserChat();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private FirebaseAuth.AuthStateListener getAuthStateListener() {
        FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mCurrentUser = firebaseAuth.getCurrentUser();
                if (mCurrentUser != null) {
                    // User is signed in
                    onSignedInInitialize();
                } else {
                    // User is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(
                                            AuthUI.GOOGLE_PROVIDER,
                                            AuthUI.FACEBOOK_PROVIDER)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
        return authStateListener;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                mCurrentUser = mFirebaseAuth.getCurrentUser();
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();

            // Get a reference to store file at chat_photos/<FILENAME>
            StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

            // Upload file to Firebase Storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // When the image has successfully uploaded, we get its download URL
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();

                            // Set the download URL to the message box, so that the user can send it to the database
                            ChatMessage chatMessage = new ChatMessage(null, mCurrentUser.getDisplayName(),
                                    mCurrentUser.getUid(), downloadUrl.toString(), new HashMap());
                            mCurrentChatDbRef.push().setValue(chatMessage);
                        }
                    });
        }
    }

    private void onSignedInInitialize() {
        mMessageAdapter.setUserId(mCurrentUser.getUid());
        attachDatabaseReadListener();
        logViewEvent();
    }

    private void onSignedOutCleanup() {
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        //To get secondary user name or group name
        mOtherEntityDbRef.addListenerForSingleValueEvent( new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(mChatType == CHAT_USER) {
                        mSecondaryUser = dataSnapshot.getValue(UserProfile.class);
                    } else if(mChatType == CHAT_GROUP) {
                        mGroup = dataSnapshot.getValue(Group.class);
                    }
                };
                @Override
                public void onCancelled(DatabaseError databaseError) {};
            });


        // Get mChatId or create a new chat
        if (mChatType == CHAT_USER)
            mCurrentUserChatDbRef = mChatsDbRef.child("user_connections").child(mCurrentUser.getUid()).child(mSecondaryUid);
        else if(mChatType == CHAT_GROUP)
            mCurrentUserChatDbRef = mChatsDbRef.child("user_groups").child(mCurrentUser.getUid()).child(mGroupId);
        if (mCurrentUserChatsEventListener == null) {
            mCurrentUserChatsEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(mChatType == CHAT_USER) {
                        UserConnection userConnection = dataSnapshot.getValue(UserConnection.class);
                        if(userConnection != null)  // if connection does not exist, giving flexibility to application
                            mChatId = userConnection.getChatId();
                    } else if (mChatType == CHAT_GROUP) {
                        mChatId = dataSnapshot.getValue(String.class);
                    }
                    // Even though the chatId is usually create during connection,
                    // but this is required to handle other type of spontaneous connections like message owner
                    if (mChatId == null) {
                        //Insert Info
                        DatabaseReference chatInfoDbRef = mChatsDbRef.child("info").push();
                        Map<String, Object> users = new HashMap<String, Object>();
                        users.put(mCurrentUser.getUid(), TRUE);
                        users.put(mSecondaryUid, TRUE);
                        chatInfoDbRef.child("users").setValue(users);
                        // Insert User Chats
                        // Deprecated from 1.0.16. Only used right now to handle older versions.
                        mChatId = chatInfoDbRef.getKey();
                        mChatsDbRef.child("user_chats").child(mCurrentUser.getUid()).child(mSecondaryUid).setValue(mChatId);
                        mChatsDbRef.child("user_chats").child(mSecondaryUid).child(mCurrentUser.getUid()).setValue(mChatId);
                        // Insert User Connections
                        // For current user
                        UserConnection primaryConnection = new UserConnection();
                        primaryConnection.setStatus(REQUEST_SENT);
                        primaryConnection.setChatId(mChatId);
                        DatabaseReference mPrimaryUserConnectionDbRef = mUserConnectionRootDbRef
                                .child(mCurrentUser.getUid()).child(mSecondaryUid);
                        mPrimaryUserConnectionDbRef.setValue(primaryConnection);
                        // For the other user
                        UserConnection secondaryConnection = new UserConnection();
                        secondaryConnection.setStatus(REQUEST_RECEIVED);
                        secondaryConnection.setChatId(mChatId);
                        DatabaseReference mSecondaryUserConnectionDbRef = mUserConnectionRootDbRef
                                .child(mSecondaryUid).child(mCurrentUser.getUid());
                        mSecondaryUserConnectionDbRef.setValue(secondaryConnection);
                    }

                    if(mMessageEventListener == null) {
                        mMessageEventListener = getMessageEventListener();
                        mCurrentChatDbRef = mChatsDbRef.child("messages").child(mChatId);
                        mCurrentChatDbRef.addChildEventListener(mMessageEventListener);
                    }
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                };
                @Override
                public void onCancelled(DatabaseError databaseError) {};
            };
            mCurrentUserChatDbRef.addValueEventListener(mCurrentUserChatsEventListener);
        }
    }

    private ChildEventListener getMessageEventListener() {
        ChildEventListener messageEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot messageSnapshot, String s) {
                ChatMessage chatMessage = messageSnapshot.getValue(ChatMessage.class);
                mMessageAdapter.add(chatMessage);
                //Mark message as seen
                String userId = mCurrentUser.getUid();
                Boolean seen_status = chatMessage.getSeenForUser(userId);
                if(chatMessage.getSenderUid() != userId && seen_status == Boolean.FALSE){
                    Map<String, Object> user_seen = new HashMap<>();
                    user_seen.put(userId, TRUE);
                    mCurrentChatDbRef.child(messageSnapshot.getKey()).child("seen").updateChildren(user_seen);
                }
            }
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) {}
        };
        return messageEventListener;
    }

    private void detachDatabaseReadListener() {
        if (mMessageEventListener != null) {
            mCurrentChatDbRef.removeEventListener(mMessageEventListener);
            mMessageEventListener = null;
        }

        if(mCurrentUserChatsEventListener != null) {
            mCurrentUserChatDbRef.removeEventListener(mCurrentUserChatsEventListener);
            mCurrentUserChatsEventListener = null;
        }
    }

    private void deleteGroupChat() {
        DatabaseReference groupDbRef = mFirebaseDatabase.getReference("groups").child(mGroupId);
        groupDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Group group = dataSnapshot.getValue(Group.class);
                // If the user is admin and the group is still visible to others then dont allow
                if(mCurrentUser.getUid().equals(group.getOwnerId()) && group.isVisible()) {
                    Toast.makeText(getApplicationContext(), "You are admin of this group. Please delete the group first" +
                            " then you can delete this conversation.", Toast.LENGTH_LONG).show();
                } else {
                    // Remove event listener
                    mCurrentUserChatDbRef.removeEventListener(mCurrentUserChatsEventListener);
                    mCurrentUserChatsEventListener = null;
                    // Delete chat info
                    mChatsDbRef.child("info").child(mChatId).child("users").child(mCurrentUser.getUid()).removeValue();
                    // Delete user group
                    mChatsDbRef.child("user_groups").child(mCurrentUser.getUid()).child(mGroupId).removeValue();
                    Toast.makeText(getApplicationContext(), "Chat deleted successfully. " +
                            "You can follow the group again if you are interested.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void deleteUserChat() {
        // Remove event listener
        mCurrentUserChatDbRef.removeEventListener(mCurrentUserChatsEventListener);
        mCurrentUserChatsEventListener = null;
        // Delete chat info
        mChatsDbRef.child("info").child(mChatId).removeValue();
        // Delete user connection for both users
        mChatsDbRef.child("user_connections").child(mCurrentUser.getUid()).child(mSecondaryUid).removeValue();
        mChatsDbRef.child("user_connections").child(mSecondaryUid).child(mCurrentUser.getUid()).removeValue();
        Toast.makeText(getApplicationContext(), "Chat deleted successfully. " +
                "You can connect again with the person if you are interested.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void logViewEvent() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mCurrentUser.getUid());
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, mCurrentUser.getDisplayName());
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "chat_window");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
    }
}