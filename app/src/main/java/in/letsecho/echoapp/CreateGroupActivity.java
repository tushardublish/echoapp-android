package in.letsecho.echoapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import in.letsecho.echoapp.library.Group;

public class CreateGroupActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mRootDbRef, mCurrentLocationDbRef;
    private ValueEventListener mCurrentLocationEventListener;
    private GeoLocation mCurrentLocation;
    private EditText mTitle;
    private EditText mDescription;
    private Button mCreateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        // Firebase
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mRootDbRef = mFirebaseDatabase.getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentUser = mFirebaseAuth.getCurrentUser();
        getCurrentLocation();
        //View
        mTitle = (EditText) findViewById(R.id.titleText);
        mDescription = (EditText) findViewById(R.id.descriptionText);
        mCreateButton = (Button) findViewById(R.id.createButton);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = mTitle.getText().toString();
                String description = mDescription.getText().toString();
                String ownerId = mCurrentUser.getUid();
                String ownerName = mCurrentUser.getDisplayName();
                Group newGroup = new Group(title, description, ownerId, ownerName, mCurrentLocation);
                mRootDbRef.child("groups").push().setValue(newGroup);

                Toast.makeText(getParent(), "Group created successfully!", Toast.LENGTH_LONG).show();
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
            }
        });
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
