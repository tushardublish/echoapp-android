package in.letsecho.echoapp;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.letsecho.library.ChatMessage;
import in.letsecho.library.UserProfile;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private Toolbar toolbar;

    private FirebaseUser currentUser;
    private String mUsername, secondaryUid;
    private String chatId;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mChatsDbRef, mSecondaryUserDbRef, currentChatDbRef;
    private DatabaseReference currentUserChatQuery;
    private ChildEventListener messageEventListener;
    private ValueEventListener secondaryUserEventListener, currentUserChatsEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mUsername = ANONYMOUS;

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mChatsDbRef = mFirebaseDatabase.getReference().child("chats");
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        // Initialize message ListView and its adapter
        List<ChatMessage> chatMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, chatMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_USER))
        {
            secondaryUid = intent.getStringExtra(Intent.EXTRA_USER);
            mSecondaryUserDbRef = mFirebaseDatabase.getReference().child("users").child(secondaryUid);
        }

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                ChatMessage chatMessage = new ChatMessage(mMessageEditText.getText().toString(),
                        mUsername, currentUser.getUid(), null, new HashMap());
                currentChatDbRef.push().setValue(chatMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });

        InitializeAuthListener();
    }

    private void InitializeAuthListener() {
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    // User is signed in
                    onSignedInInitialize(currentUser.getDisplayName());
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                currentUser = mFirebaseAuth.getCurrentUser();
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
                            ChatMessage chatMessage = new ChatMessage(null, mUsername,
                                    currentUser.getUid(), downloadUrl.toString(), new HashMap());
                            currentChatDbRef.push().setValue(chatMessage);
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuthStateListener == null) {
            InitializeAuthListener();
        }
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {

        if (secondaryUserEventListener == null) {
            secondaryUserEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    UserProfile secondaryUser = dataSnapshot.getValue(UserProfile.class);
                    toolbar.setTitle(secondaryUser.getName());
                };
                @Override
                public void onCancelled(DatabaseError databaseError) {};
            };
            mSecondaryUserDbRef.addValueEventListener(secondaryUserEventListener);
        }

        currentUserChatQuery = mChatsDbRef.child("user_chats").child(currentUser.getUid()).child(secondaryUid);
        if (currentUserChatsEventListener == null) {
            currentUserChatsEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    chatId = dataSnapshot.getValue(String.class);
                    //New Chat
                    if (chatId == null) {
                        //Insert Info
                        DatabaseReference chatInfoDbRef = mChatsDbRef.child("info").push();
                        Map<String, Object> users = new HashMap<String, Object>();
                        users.put(currentUser.getUid(), Boolean.TRUE);
                        users.put(secondaryUid, Boolean.TRUE);
                        chatInfoDbRef.child("users").setValue(users);
                        //Insert User Chats
                        chatId = chatInfoDbRef.getKey();
                        mChatsDbRef.child("user_chats").child(currentUser.getUid()).child(secondaryUid).setValue(chatId);
                        mChatsDbRef.child("user_chats").child(secondaryUid).child(currentUser.getUid()).setValue(chatId);
                    }

                    if (currentChatDbRef == null) {
                        currentChatDbRef = mChatsDbRef.child("messages").child(chatId);
                        currentChatDbRef.addChildEventListener(messageEventListener);
                    }
                };
                @Override
                public void onCancelled(DatabaseError databaseError) {};
            };
            currentUserChatQuery.addValueEventListener(currentUserChatsEventListener);
        }


        if (messageEventListener == null) {
            messageEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                    mMessageAdapter.add(chatMessage);
                    //Mark message as seen
                    String userId = currentUser.getUid();
                    Boolean seen_status = chatMessage.getSeenForUser(userId);
                    if(chatMessage.getSenderUid() != userId && seen_status == Boolean.FALSE){
                        chatMessage.setSeenForUser(userId);
                        currentChatDbRef.child(dataSnapshot.getKey()).setValue(chatMessage);
                    }
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
        }
    }

    private void detachDatabaseReadListener() {
        if (messageEventListener != null) {
            mChatsDbRef.removeEventListener(messageEventListener);
            messageEventListener = null;
        }

        if (secondaryUserEventListener != null) {
            mSecondaryUserDbRef.removeEventListener(secondaryUserEventListener);
            secondaryUserEventListener = null;
        }

        if(currentChatDbRef != null) {
            currentChatDbRef.removeEventListener(currentUserChatsEventListener);
            currentUserChatsEventListener = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}