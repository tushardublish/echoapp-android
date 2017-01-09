package in.letsecho.appengine;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;

@Api(name = "nearbypeople", version = "v1", scopes = {Constants.EMAIL_SCOPE },
        clientIds = {Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID },
        description = "API to track nearb4y people.")

public class NearbyPeople {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mCurrentLocationDbRef, mPastLocationDbRef;
    private ArrayList<GeoQueryEventListener> mNearbyPeopleEventListeners;
    private ArrayList<GeoQuery> mNearbyPeopleGeoQueries;
    private GeoFire mNearbyPeopleGeoRef;

    public void init(ServletContext context) {
        String credential = "/WEB-INF/echoapp-5e21e-firebase-adminsdk-kpl70-c9e1d2735f.json";
        String databaseUrl = "https://echoapp-5e21e.firebaseio.com";
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setServiceAccount(context.getResourceAsStream(credential))
                .setDatabaseUrl(databaseUrl)
                .build();
        FirebaseApp.initializeApp(options);
        mFirebaseDatabase = FirebaseDatabase.getInstance();
    }

    @ApiMethod(name = "track", path = "track", httpMethod = HttpMethod.GET)
    public void track(ServletContext context) {
        init(context);
        mNearbyPeopleGeoQueries = new ArrayList<>();
        mNearbyPeopleEventListeners = new ArrayList<>();
        mCurrentLocationDbRef = mFirebaseDatabase.getReference("locations/current");
        mPastLocationDbRef = mFirebaseDatabase.getReference("locations/past");
        mNearbyPeopleGeoRef = new GeoFire(mCurrentLocationDbRef);
        mCurrentLocationDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot user: dataSnapshot.getChildren()) {
                    String userId = user.getKey();
                    ArrayList locationObj = (ArrayList)user.child("l").getValue();
                    if(locationObj != null) {
                        GeoLocation currentLocation = new GeoLocation((double) locationObj.get(0),
                                (double) locationObj.get(1));
                        saveNearbyPeople(userId, currentLocation);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }


    /*
    This thing wont work right now because GeoFire isnt compatible with Google Apps Engine.
    There is some restriction on how you can create Threads in GAE.
    An open PR is pending so this might get resolved soon. Check GeoFire PRs and Issues.
     */
    private void saveNearbyPeople(final String userId, GeoLocation currentLocation) {
        double radium_km = 0.1;
        final ArrayList<String> nearbyList = new ArrayList<>();
        GeoQuery mNearbyPeopleGeoQuery = mNearbyPeopleGeoRef.queryAtLocation(currentLocation, radium_km);
        GeoQueryEventListener mNearbyPeopleEventListener = new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                nearbyList.add(key);
            }
            @Override
            public void onKeyExited(String key) {}
            @Override
            public void onKeyMoved(String key, GeoLocation location) {}
            @Override
            public void onGeoQueryReady() {
                HashMap<String, Object> nearbyHash = new HashMap();
                for(String secondaryUserId: nearbyList) {
                    nearbyHash.put(secondaryUserId, ServerValue.TIMESTAMP);
                }
                //This will also update an existing user to the latest meeting time
                mPastLocationDbRef.child(userId).setValue(nearbyHash);
            }
            @Override
            public void onGeoQueryError(DatabaseError error) {}
        };
        mNearbyPeopleGeoQuery.addGeoQueryEventListener(mNearbyPeopleEventListener);
        mNearbyPeopleGeoQueries.add(mNearbyPeopleGeoQuery);
        mNearbyPeopleEventListeners.add(mNearbyPeopleEventListener);
    }
}
