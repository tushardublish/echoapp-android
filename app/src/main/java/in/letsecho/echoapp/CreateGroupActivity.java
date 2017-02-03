package in.letsecho.echoapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import in.letsecho.echoapp.library.Group;

public class CreateGroupActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mRootDbRef, mCurrentLocationDbRef;
    private GeoFire mGeoFire;
    private GeoLocation mCurrentLocation;
    private EditText mTitle, mDescription, mPhoneNo;
    private Button mCreateButton;
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
        mGeoFire = new GeoFire(locationDbRef);
        getCurrentLocation();
        //View
        mTitle = (EditText) findViewById(R.id.titleText);
        mDescription = (EditText) findViewById(R.id.descriptionText);
        mTypeRadioGroup = (RadioGroup) findViewById(R.id.typeRadioGroup);
        mPhoneNo = (EditText) findViewById(R.id.phoneNoText);
        mCreateButton = (Button) findViewById(R.id.createButton);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
                Group newGroup = new Group(title, description, ownerId, ownerName, phoneNo, type);
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
    }

    private String getGroupType() {
        int selectedId = mTypeRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedButton = (RadioButton) findViewById(selectedId);
        String type = selectedButton.getText().toString();
        return type;
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
}
