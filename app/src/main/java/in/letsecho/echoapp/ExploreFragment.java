package in.letsecho.echoapp;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import in.letsecho.library.UserProfile;

public class ExploreFragment extends Fragment {
    private static String TAG = "ExploreFragment";
    private ListView mPersonListView;
    private PersonAdapter mPersonAdapter;
    private ProgressBar mProgressBar;
    List<UserProfile> mPersons;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersDbRef, mLocationsDbRef, mCurrentLocationDbRef;
    private ChildEventListener mUserProfileEventListener;
    private ValueEventListener mCurrentLocationEventListener;
    private GeoQueryEventListener mNearbyPeopleEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentUser;
    private GeoFire mNearbyPeopleGeoRef;
    private GeoQuery mNearbyPeopleGeoQuery;
    private GeoLocation mCurrentLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        // Initialize references to views
        mPersonListView = (ListView) view.findViewById(R.id.personListView);
        mPersonListView.setAdapter(mPersonAdapter);
        mPersonListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserProfile person = mPersonAdapter.getItem(position);
                Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                        .putExtra(Intent.EXTRA_USER, person.getUID());
                startActivity(intent);
            }
        });
        // Initialize progress bar
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mUsersDbRef = mFirebaseDatabase.getReference("users");
        mLocationsDbRef = mFirebaseDatabase.getReference("locations/current");
        mNearbyPeopleGeoRef = new GeoFire(mLocationsDbRef);

        // Initialize person ListView and its adapter
        mPersons = new ArrayList<>();
        mPersonAdapter = new PersonAdapter(this.getContext(), R.layout.item_person, mPersons);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCurrentUser = mFirebaseAuth.getCurrentUser();
        if(mCurrentUser != null) {
            attachDatabaseReadListener();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mPersonAdapter.clear();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        mCurrentLocationDbRef = mLocationsDbRef.child(mCurrentUser.getUid()).child("l");
        if (mCurrentLocationEventListener == null) {
            mCurrentLocationEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList locationObj = (ArrayList)dataSnapshot.getValue();
                    GeoLocation currentLocation = new GeoLocation((double)locationObj.get(0),
                                                                    (double)locationObj.get(1));
                    getNearbyPeople(currentLocation);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };
            mCurrentLocationDbRef.addValueEventListener(mCurrentLocationEventListener);
        }
    }

    private void getNearbyPeople(GeoLocation currentLocation) {
        double radium_km = 0.1;
        if(mCurrentLocation == null) {
            mNearbyPeopleGeoQuery = mNearbyPeopleGeoRef.queryAtLocation(currentLocation, radium_km);
        } else {
            mNearbyPeopleGeoQuery.setCenter(currentLocation);
            mCurrentLocation = currentLocation;
        }
        if(mNearbyPeopleEventListener == null) {
            mNearbyPeopleEventListener = new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    //Tudu: Should add new user at position based on the distance
                    addUserToList(key);
                }
                @Override
                public void onKeyExited(String key) {
                    removeUserFromList(key);
                }
                @Override
                public void onKeyMoved(String key, GeoLocation location) {}
                @Override
                public void onGeoQueryReady() {}
                @Override
                public void onGeoQueryError(DatabaseError error) {}
            };
            mNearbyPeopleGeoQuery.addGeoQueryEventListener(mNearbyPeopleEventListener);
        }
    }

    private void addUserToList(String secondaryUserId) {
        DatabaseReference userDbRef = mUsersDbRef.child(secondaryUserId);
        userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserProfile secondaryUser = dataSnapshot.getValue(UserProfile.class);
                mPersonAdapter.add(secondaryUser);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    private void removeUserFromList(String secondaryUserId) {
        int index = UserProfile.findProfileOnUid(mPersons, secondaryUserId);
        UserProfile removedPerson = mPersons.get(index);
        mPersonAdapter.remove(removedPerson);
    }

    private void detachDatabaseReadListener() {
        if (mUserProfileEventListener != null) {
            mUsersDbRef.removeEventListener(mUserProfileEventListener);
            mUserProfileEventListener = null;
        }

        if(mCurrentLocationEventListener != null) {
            mCurrentLocationDbRef.removeEventListener(mCurrentLocationEventListener);
            mCurrentLocationEventListener = null;
        }

        if(mNearbyPeopleEventListener != null) {
            mNearbyPeopleGeoQuery.removeGeoQueryEventListener(mNearbyPeopleEventListener);
            mNearbyPeopleEventListener = null;
        }
    }
}
