package in.letsecho.echoapp;


import android.app.DialogFragment;
import android.content.Intent;
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
import static in.letsecho.echoapp.library.Group.SERVICE_TYPE;

public class ExploreFragment extends Fragment {
    private static String TAG = "ExploreFragment";
    private static String HEADER1 = "Nearby Groups";
    private static String HEADER2 = "Nearby Services";
    private static String HEADER3 = "Nearby People";
    private static String HEADER4 = "People near you in last 24 hours";
    private static final String NEARBY_DISTANCE_CONFIG_KEY = "nearby_distance";
    private static long HOURS_TO_MILLI_SECS = 60*60*1000;
    private ExpandableListView mExploreListView;
    private PersonAdapterExpandableList mExploreAdapter;
    private ProgressBar mProgressBar;
    private List<String> mSectionHeaders;
    private List<EntityDisplayModel> mCurrentPeople, mPastPeople, mGroups, mServices;
    private HashMap<String, List<EntityDisplayModel>> mExpandableList;
    private double mNearbyDistance; //(Km)

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersDbRef, mLocationsDbRef, mCurrentLocationDbRef;
    private Query mPastLocationDbRef;
    private ValueEventListener mCurrentLocationEventListener, mPastPeopleEventListener;
    private GeoQueryEventListener mNearbyPeopleEventListener, mNearbyGroupEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentUser;
    private GeoFire mNearbyPeopleGeoRef, mNearbyGroupGeoRef;
    private GeoQuery mNearbyPeopleGeoQuery, mNearbyGroupGeoQuery;
    private GeoLocation mCurrentLocation;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        // Initialize references to views
        mExploreListView = (ExpandableListView) view.findViewById(R.id.peopleListView);
        mExploreListView.setAdapter(mExploreAdapter);
        mExploreListView.setOnChildClickListener(getItemOnClickListener());

        // Initialize progress bar
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

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
        DatabaseReference groupLocationDbRef = mFirebaseDatabase.getReference("locations/groups");
        mNearbyGroupGeoRef = new GeoFire(groupLocationDbRef);

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
        mServices = new ArrayList<>();
        mCurrentPeople = new ArrayList<>();
        mPastPeople = new ArrayList<>();
        mSectionHeaders = new ArrayList<>();
        mSectionHeaders.add(HEADER1);
        mSectionHeaders.add(HEADER2);
        mSectionHeaders.add(HEADER3);
        mSectionHeaders.add(HEADER4);
        mExpandableList = new HashMap<>();
        mExpandableList.put(HEADER1, mGroups);
        mExpandableList.put(HEADER2, mServices);
        mExpandableList.put(HEADER3, mCurrentPeople);
        mExpandableList.put(HEADER4, mPastPeople);
        mExploreAdapter = new PersonAdapterExpandableList(this.getContext(), mSectionHeaders, mExpandableList);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCurrentUser = mFirebaseAuth.getCurrentUser();
        if(mCurrentUser != null) {
            attachDatabaseReadListener();
        }
        // Explanding the lists
        for(int i=0; i<mExploreAdapter.getGroupCount(); i++)
            mExploreListView.expandGroup(i);

        // Manage if opened by intents
        Intent intent = getActivity().getIntent();
        if(intent != null) {
            if(intent.hasExtra("PROFILE_ID")) {
                String profileId = intent.getStringExtra("PROFILE_ID");
                DialogFragment profileDialog = new UserProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putString("secondaryUserId", profileId);
                profileDialog.setArguments(bundle);
                profileDialog.show(getActivity().getFragmentManager(), "userprofile");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mGroups.clear();
        mServices.clear();
        mCurrentPeople.clear();
        mPastPeople.clear();
        mExploreAdapter.notifyDataSetChanged();
        detachDatabaseReadListener();
    }

    private ExpandableListView.OnChildClickListener getItemOnClickListener() {
        ExpandableListView.OnChildClickListener listener =  new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View view, int groupPosition,
                                        int childPosition, long id) {
                EntityDisplayModel profile = null;
                switch(groupPosition) {
                    case 0:
                        profile = mGroups.get(childPosition);
                        break;
                    case 1:
                        profile = mServices.get(childPosition);
                        break;
                    case 2:
                        profile = mCurrentPeople.get(childPosition);
                        break;
                    case 3:
                        profile = mPastPeople.get(childPosition);
                        break;
                }
                // Open Profile
                if(profile.getType() == USER_TYPE) {
                    DialogFragment profileDialog = new UserProfileFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("secondaryUserId", profile.getUid());
                    profileDialog.setArguments(bundle);
                    profileDialog.show(getActivity().getFragmentManager(), "userprofile");
                }
                else if (profile.getType() == GROUP_TYPE) {
                    DialogFragment profileDialog = new GroupProfileFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("groupId", profile.getUid());
                    profileDialog.setArguments(bundle);
                    profileDialog.show(getActivity().getFragmentManager(), "groupprofile");
                }
                return true;
            }
        };
        return listener;
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
                        getNearbyGroups(currentLocation);
                        getNearbyPeople(currentLocation);
                        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };
            mCurrentLocationDbRef.addValueEventListener(mCurrentLocationEventListener);
        }
        addGroupToList("-KcpvhJdGQXSnFcjhyDv"); // Adding support group for everyone
        getPastNearbyPeople();
    }

    private void getNearbyGroups(GeoLocation currentLocation) {
        if(mCurrentLocation == null) {
            mNearbyGroupGeoQuery = mNearbyGroupGeoRef.queryAtLocation(currentLocation, mNearbyDistance);
        } else {
            mNearbyGroupGeoQuery.setCenter(currentLocation);
            mCurrentLocation = currentLocation;
        }
        if(mNearbyGroupEventListener == null) {
            mNearbyGroupEventListener = new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    addGroupToList(key);
                }
                @Override
                public void onKeyExited(String key) {
                    removeGroupFromList(key);
                }
                @Override
                public void onKeyMoved(String key, GeoLocation location) {}
                @Override
                public void onGeoQueryReady() {}
                @Override
                public void onGeoQueryError(DatabaseError error) {}
            };
            mNearbyGroupGeoQuery.addGeoQueryEventListener(mNearbyGroupEventListener);
        }
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

    private void addGroupToList(String groupId) {
        DatabaseReference groupDbRef = mFirebaseDatabase.getReference("groups").child(groupId);
        groupDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Group group = dataSnapshot.getValue(Group.class);
                EntityDisplayModel displayGroup = new EntityDisplayModel(group);
                if(group.getType().equals(SERVICE_TYPE))
                    mServices.add(displayGroup);
                else
                    mGroups.add(displayGroup);
                mExploreAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }


    private void addUserToCurrentList(String secondaryUserId) {
        DatabaseReference userDbRef = mUsersDbRef.child(secondaryUserId);
        userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserProfile secondaryUser = dataSnapshot.getValue(UserProfile.class);
                EntityDisplayModel displayUser = new EntityDisplayModel(secondaryUser);
                mCurrentPeople.add(displayUser);
                mExploreAdapter.notifyDataSetChanged();
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
                mExploreAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    private void removeGroupFromList(String groupId) {
        // First check in groups then check in services
        int index = EntityDisplayModel.findProfileOnUid(mGroups, groupId);
        if(index >= 0 && index < mGroups.size()) {
            EntityDisplayModel removedGroup = mGroups.get(index);
            mGroups.remove(removedGroup);
            mExploreAdapter.notifyDataSetChanged();
            return;
        }
        index = EntityDisplayModel.findProfileOnUid(mServices, groupId);
        if(index >= 0 && index < mServices.size()) {
            EntityDisplayModel removedGroup = mServices.get(index);
            mServices.remove(removedGroup);
            mExploreAdapter.notifyDataSetChanged();
            return;
        }
    }

    private void removeUserFromCurrentList(String secondaryUserId) {
        int index = EntityDisplayModel.findProfileOnUid(mCurrentPeople, secondaryUserId);
        if(index >= 0 && index < mCurrentPeople.size()) {
            EntityDisplayModel removedPerson = mCurrentPeople.get(index);
            mCurrentPeople.remove(removedPerson);
            mExploreAdapter.notifyDataSetChanged();
        }
    }

    private void detachDatabaseReadListener() {

        if(mCurrentLocationEventListener != null) {
            mCurrentLocationDbRef.removeEventListener(mCurrentLocationEventListener);
            mCurrentLocationEventListener = null;
        }

        if(mNearbyGroupEventListener != null) {
            mNearbyGroupGeoQuery.removeAllListeners();
//            mNearbyGroupGeoQuery.removeGeoQueryEventListener(mNearbyGroupEventListener);
            mNearbyGroupEventListener = null;
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
