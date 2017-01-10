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
import android.widget.TextView;

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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import in.letsecho.library.UserDisplayModel;
import in.letsecho.library.UserProfile;

public class ExploreFragment extends Fragment {
    private static String TAG = "ExploreFragment";
    private static long HOURS_TO_MILLI_SECS = 60*60*1000;
    private ListView mCurrentPeopleListView, mPastPeopleListView;
    private PersonAdapter mCurrentPeopleAdapter, mPastPeopleAdapter;
    private ProgressBar mProgressBar;
    List<UserDisplayModel> mCurrentPeople, mPastPeople;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersDbRef, mLocationsDbRef, mCurrentLocationDbRef;
    private Query mPastLocationDbRef;
    private ChildEventListener mUserProfileEventListener;
    private ValueEventListener mCurrentLocationEventListener, mPastPeopleEventListener;
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
        mCurrentPeopleListView = (ListView) view.findViewById(R.id.currentPeopleListView);
        View currentPeopleHeader = inflater.inflate(R.layout.header_current_people, null);
        mCurrentPeopleListView.addHeaderView(currentPeopleHeader);
        mCurrentPeopleListView.setAdapter(mCurrentPeopleAdapter);
        mCurrentPeopleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserProfile person = mCurrentPeopleAdapter.getItem(position);
                Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                        .putExtra(Intent.EXTRA_USER, person.getUID());
                startActivity(intent);
            }
        });

        mPastPeopleListView = (ListView) view.findViewById(R.id.pastPeopleListView);
        View pastPeopleHeader = inflater.inflate(R.layout.header_past_people, null);
        mPastPeopleListView.addHeaderView(pastPeopleHeader);
        mPastPeopleListView.setAdapter(mPastPeopleAdapter);
        mPastPeopleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserProfile person = mPastPeopleAdapter.getItem(position);
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
        mCurrentPeople = new ArrayList<>();
        mCurrentPeopleAdapter = new PersonAdapter(this.getContext(), R.layout.item_person, mCurrentPeople);
        mPastPeople = new ArrayList<>();
        mPastPeopleAdapter = new PersonAdapter(this.getContext(), R.layout.item_person, mPastPeople);
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
        mCurrentPeopleAdapter.clear();
        mPastPeopleAdapter.clear();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        mCurrentLocationDbRef = mLocationsDbRef.child(mCurrentUser.getUid()).child("l");
        if (mCurrentLocationEventListener == null) {
            mCurrentLocationEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList locationObj = (ArrayList)dataSnapshot.getValue();
                    if(locationObj != null) {
                        GeoLocation currentLocation = new GeoLocation((double) locationObj.get(0),
                                (double) locationObj.get(1));
                        getNearbyPeople(currentLocation);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };
            mCurrentLocationDbRef.addValueEventListener(mCurrentLocationEventListener);
        }

        getPastNearbyPeople();
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
                    addUserToCurrentList(key);
                }
                @Override
                public void onKeyExited(String key) {
                    removeUserFromCurrentList(key);
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

    private void getPastNearbyPeople() {
        long one_day_milli_secs = 24*HOURS_TO_MILLI_SECS;
        long start_time = System.currentTimeMillis() - one_day_milli_secs;
        mPastLocationDbRef = mFirebaseDatabase.getReference("locations/past").child(mCurrentUser.getUid())
                .orderByValue().startAt(start_time).limitToLast(100);
        if(mPastPeopleEventListener == null) {
            mPastPeopleEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot secondaryUser: dataSnapshot.getChildren()) {
                        String secondaryUserId = secondaryUser.getKey();
                        int hourDiff = (int)((System.currentTimeMillis() - secondaryUser.getValue(Long.class))
                                /HOURS_TO_MILLI_SECS);
                        addUserToPastList(secondaryUserId, hourDiff);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };
            mPastLocationDbRef.addListenerForSingleValueEvent(mPastPeopleEventListener);
        }
    }

    private void addUserToCurrentList(String secondaryUserId) {
        DatabaseReference userDbRef = mUsersDbRef.child(secondaryUserId);
        userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserDisplayModel secondaryUser = dataSnapshot.getValue(UserDisplayModel.class);
                mCurrentPeopleAdapter.add(secondaryUser);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    private void addUserToPastList(String secondaryUserId, final Integer hourDiff) {
        DatabaseReference userDbRef = mUsersDbRef.child(secondaryUserId);
        userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserDisplayModel secondaryUser = dataSnapshot.getValue(UserDisplayModel.class);
                //Setting hour ago the person was near
                if (hourDiff == 1)
                    secondaryUser.setRightAlignedInfo(hourDiff.toString() + " hour");
                else if (hourDiff > 1)
                    secondaryUser.setRightAlignedInfo(hourDiff.toString() + " hours");
                mPastPeopleAdapter.add(secondaryUser);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    private void removeUserFromCurrentList(String secondaryUserId) {
        int index = UserDisplayModel.findProfileOnUid(mCurrentPeople, secondaryUserId);
        UserDisplayModel removedPerson = mCurrentPeople.get(index);
        mCurrentPeopleAdapter.remove(removedPerson);
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
