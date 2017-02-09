package in.letsecho.echoapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import in.letsecho.echoapp.library.Group;

public class CreateGroupActivity extends AppCompatActivity {

    private static final int RC_PHOTO_PICKER =  1;
    private String mPhotoUrl;
    private Group mGroup;
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mRootDbRef, mCurrentLocationDbRef;
    private StorageReference mProfilePhotoStorageReference;
    private GeoFire mGeoFire;
    private GeoLocation mCurrentLocation;
    private Toolbar mToolbar;
    private EditText mTitle, mDescription, mPhoneNo;
    private Button mCreateButton, mUpdateButton;
    private ImageButton mUploadPhotoButton;
    private ImageView mProfilePhoto;
    private RadioGroup mTypeRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        // Firebase
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mRootDbRef = mFirebaseDatabase.getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentUser = mFirebaseAuth.getCurrentUser();
        DatabaseReference locationDbRef = mRootDbRef.child("locations/groups");
        mProfilePhotoStorageReference = FirebaseStorage.getInstance().getReference().child("profile_photos");
        mGeoFire = new GeoFire(locationDbRef);
        getCurrentLocation();
        //View
        mTitle = (EditText) findViewById(R.id.titleText);
        mDescription = (EditText) findViewById(R.id.descriptionText);
        mTypeRadioGroup = (RadioGroup) findViewById(R.id.typeRadioGroup);
        mPhoneNo = (EditText) findViewById(R.id.phoneNoText);
        mCreateButton = (Button) findViewById(R.id.createButton);
        mUpdateButton = (Button) findViewById(R.id.updateButton);
        mUploadPhotoButton = (ImageButton) findViewById(R.id.uploadImageButton);
        mProfilePhoto = (ImageView) findViewById(R.id.profileImageView);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mUploadPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadPhoto();
            }
        });

        mProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadPhoto();
            }
        });

        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create Group
                String title = mTitle.getText().toString();
                String description = mDescription.getText().toString();
                String phoneNo = mPhoneNo.getText().toString();
                String ownerId = mCurrentUser.getUid();
                String ownerName = mCurrentUser.getDisplayName();
                String type = getGroupType();
                //Check for empty title and description
                if(title.isEmpty() || description.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter title and description.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Group newGroup = new Group(title, description, ownerId, ownerName, phoneNo, type, mPhotoUrl);
                DatabaseReference groupDbRef = mRootDbRef.child("groups").push();
                String groupId = groupDbRef.getKey();
                newGroup.setId(groupId);
                String chatId = setGroupChat(groupId);
                newGroup.setChatId(chatId);
                groupDbRef.setValue(newGroup);
                //Set Location
                mGeoFire.setLocation(groupId, mCurrentLocation);

                Toast.makeText(getApplicationContext(), "Group created successfully!", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Update Group
                mGroup.setTitle(mTitle.getText().toString());
                mGroup.setDescription(mDescription.getText().toString());
                mGroup.setPhoneNo(mPhoneNo.getText().toString());
                mGroup.setType(getGroupType());
                //Check for empty title and description
                if(mGroup.getTitle().isEmpty() || mGroup.getDescription().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter title and description.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Updating DB, but not updating location
                Map<String, Object> groupValues = mGroup.toMap();
                mRootDbRef.child("groups").child(mGroup.getId()).updateChildren(groupValues);

                Toast.makeText(getApplicationContext(), "Group updated successfully!", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("GROUP_ID")) {
            // Set UI
            mToolbar.setTitle("Update Post");
            mCreateButton.setVisibility(View.GONE);
            mUpdateButton.setVisibility(View.VISIBLE);
            // Get Group Info
            String groupId = intent.getStringExtra("GROUP_ID");
            DatabaseReference groupDbRef = mRootDbRef.child("groups").child(groupId);
            groupDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mGroup = dataSnapshot.getValue(Group.class);
                    mTitle.setText(mGroup.getTitle());
                    mDescription.setText(mGroup.getDescription());
                    mPhoneNo.setText(mGroup.getPhoneNo());
                    if(mGroup.getType().equals(getString(R.string.group_type_service)))
                        mTypeRadioGroup.check(R.id.serviceRadioButton);
                    else if(mGroup.getType().equals(getString(R.string.group_type_group)))
                        mTypeRadioGroup.check(R.id.interestRadioButton);
                    if(mGroup.getPhotoUrl() != null) {
                        mProfilePhoto.setVisibility(View.VISIBLE);
                        mUploadPhotoButton.setVisibility(View.GONE);
                        Glide.with(mProfilePhoto.getContext())
                                .load(mGroup.getPhotoUrl())
                                .into(mProfilePhoto);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            // Get a reference to store file at chat_photos/<FILENAME>
            StorageReference photoRef = mProfilePhotoStorageReference.child(selectedImageUri.getLastPathSegment());

            // Upload file to Firebase Storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // When the image has successfully uploaded, we get its download URL
                            mPhotoUrl = taskSnapshot.getDownloadUrl().toString();
                            mProfilePhoto.setVisibility(View.VISIBLE);
                            mUploadPhotoButton.setVisibility(View.GONE);
                            Glide.with(mProfilePhoto.getContext())
                                    .load(mPhotoUrl)
                                    .into(mProfilePhoto);
                        }
                    });
        }
    }

    private String getGroupType() {
        int selectedId = mTypeRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedButton = (RadioButton) findViewById(selectedId);
        if(selectedButton != null) {
            String type = selectedButton.getText().toString();
            return type;
        } else {
            return null;
        }
    }

    private String setGroupChat(String groupId) {
        //Set Chat
        DatabaseReference chatInfoDbRef = mRootDbRef.child("chats/info").push();
        Map<String, Object> users = new HashMap<String, Object>();
        users.put(mCurrentUser.getUid(), Boolean.TRUE);
        chatInfoDbRef.child("users").updateChildren(users);
        //Insert User Chat
        String chatId = chatInfoDbRef.getKey();
        mRootDbRef.child("chats/user_groups").child(mCurrentUser.getUid()).child(groupId).setValue(chatId);
        return chatId;
    }

    private void getCurrentLocation() {
        mCurrentLocationDbRef = mRootDbRef.child("locations/current").child(mCurrentUser.getUid()).child("l");
        mCurrentLocationDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList locationObj = (ArrayList)dataSnapshot.getValue();
                if(locationObj != null) {
                    mCurrentLocation = new GeoLocation((double) locationObj.get(0),
                            (double) locationObj.get(1));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void uploadPhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
    }
}
