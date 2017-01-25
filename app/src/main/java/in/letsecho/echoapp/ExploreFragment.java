package in.letsecho.echoapp;


import android.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import in.letsecho.echoapp.library.Group;
import in.letsecho.echoapp.library.EntityDisplayModel;
import in.letsecho.echoapp.library.UserProfile;

import static in.letsecho.echoapp.library.EntityDisplayModel.GROUP_TYPE;
import static in.letsecho.echoapp.library.EntityDisplayModel.USER_TYPE;

public class ExploreFragment extends Fragment {
    private static String TAG = "ExploreFragment";
    private static String HEADER1 = "Nearby Groups";
    private static String HEADER2 = "Nearby People";
    private static String HEADER3 = "People near you in last 24 hours";
    private static final String NEARBY_DISTANCE_CONFIG_KEY = "nearby_distance";
    private static long HOURS_TO_MILLI_SECS = 60*60*1000;
    private ExpandableListView mPeopleListView;
    private PersonAdapterExpandableList mPeopleAdapter;
    private ProgressBar mProgressBar;
    private List<String> mSectionHeaders;
    private List<EntityDisplayModel> mCurrentPeople, mPastPeople, mGroups;
    private HashMap<String, List<EntityDisplayModel>> mExpandableList;
    private double mNearbyDistance; //(Km)

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
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        // Initialize references to views
        mPeopleListView = (ExpandableListView) view.findViewById(R.id.peopleListView);
        mPeopleListView.setAdapter(mPeopleAdapter);
        mPeopleListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View view, int groupPosition,
                                     int childPosition, long id) {
                EntityDisplayModel profile = null;
                switch(groupPosition) {
                    case 0:
                        profile = mGroups.get(childPosition);
                    case 1:
                        profile = mCurrentPeople.get(childPosition);
                        break;
                    case 2:
                        profile = mPastPeople.get(childPosition);
                        break;
                }
                // Open Profile
                DialogFragment profileDialog = new ProfileFragment();
                Bundle bundle = new Bundle();
                if(profile.getType() == USER_TYPE)
                    bundle.putString("secondaryUserId", profile.getUid());
                else if(profile.getType() == GROUP_TYPE)
                    bundle.putString("groupId", profile.getUid());
                profileDialog.setArguments(bundle);
                profileDialog.show(getActivity().getFragmentManager(), "profile");
                return true;
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
        setupExpandableList();

        //Setting Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        mNearbyDistance = mFirebaseRemoteConfig.getDouble(NEARBY_DISTANCE_CONFIG_KEY);
        fetchRemoteConfigValues();
    }

    private void setupExpandableList() {
        mGroups = new ArrayList<>();
        mCurrentPeople = new ArrayList<>();
        mPastPeople = new ArrayList<>();
        mSectionHeaders = new ArrayList<>();
        mSectionHeaders.add(HEADER1);
        mSectionHeaders.add(HEADER2);
        mSectionHeaders.add(HEADER3);
        mExpandableList = new HashMap<>();
        mExpandableList.put(HEADER1, mGroups);
        mExpandableList.put(HEADER2, mCurrentPeople);
        mExpandableList.put(HEADER3, mPastPeople);
        mPeopleAdapter = new PersonAdapterExpandableList(this.getContext(), mSectionHeaders, mExpandableList);
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
        mCurrentPeople.clear();
        mPastPeople.clear();
        mPeopleAdapter.notifyDataSetChanged();
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
        if(mCurrentLocation == null) {
            mNearbyPeopleGeoQuery = mNearbyPeopleGeoRef.queryAtLocation(currentLocation, mNearbyDistance);
        } else {
            mNearbyPeopleGeoQuery.setCenter(currentLocation);
            mCurrentLocation = currentLocation;
        }
        if(mNearbyPeopleEventListener == null) {
            mNearbyPeopleEventListener = new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    //Tudu: Should add new user at position based on the distance
                    if(!key.equals(mCurrentUser.getUid()))
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
                        if(!secondaryUserId.equals(mCurrentUser.getUid()))
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
                UserProfile secondaryUser = dataSnapshot.getValue(UserProfile.class);
                EntityDisplayModel displayUser = new EntityDisplayModel(secondaryUser);
                mCurrentPeople.add(displayUser);
                mPeopleAdapter.notifyDataSetChanged();
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
                UserProfile secondaryUser = dataSnapshot.getValue(UserProfile.class);
                EntityDisplayModel displayUser = new EntityDisplayModel(secondaryUser);
                //Setting hour ago the person was near
                if (hourDiff == 1)
                    displayUser.setRightAlignedInfo(hourDiff.toString() + " hour");
                else if (hourDiff > 1)
                    displayUser.setRightAlignedInfo(hourDiff.toString() + " hours");
                mPastPeople.add(displayUser);
                mPeopleAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    private void removeUserFromCurrentList(String secondaryUserId) {
        int index = EntityDisplayModel.findProfileOnUid(mCurrentPeople, secondaryUserId);
        EntityDisplayModel removedPerson = mCurrentPeople.get(index);
        mPastPeople.remove(removedPerson);
        mPeopleAdapter.notifyDataSetChanged();
    }

    private void detachDatabaseReadListener() {

        if(mCurrentLocationEventListener != null) {
            mCurrentLocationDbRef.removeEventListener(mCurrentLocationEventListener);
            mCurrentLocationEventListener = null;
        }

        if(mNearbyPeopleEventListener != null) {
            mNearbyPeopleGeoQuery.removeAllListeners();
//            mNearbyPeopleGeoQuery.removeGeoQueryEventListener(mNearbyPeopleEventListener);
            mNearbyPeopleEventListener = null;
        }

        if(mPastPeopleEventListener != null) {
            mPastLocationDbRef.removeEventListener(mPastPeopleEventListener);
            mPastPeopleEventListener = null;
        }
    }

    private void fetchRemoteConfigValues() {
        long cacheExpiration = 3600; // 1 hour in seconds.
        // Need to enable developer mode during OnCreate using following lines to immediately fetch config changes
//        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
//                .setDeveloperModeEnabled(BuildConfig.DEBUG)
//                .build();
//        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        // cacheExpirationSeconds is set to cacheExpiration here, indicating that any previously
        // fetched and cached config would be considered expired because it would have been fetched
        // more than cacheExpiration seconds ago. Thus the next fetch would go to the server unless
        // throttling is in progress. The default expiration duration is 43200 (12 hours).
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this.getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Remote Config Fetch Succeeded");
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                            Log.d(TAG, "Remote Config Fetch Failed");
                        }
                        mNearbyDistance = mFirebaseRemoteConfig.getDouble(NEARBY_DISTANCE_CONFIG_KEY);
                    }
                });
    }
}
