package in.letsecho.echoapp;

import android.Manifest;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.facebook.FacebookSdk;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import in.letsecho.echoapp.service.LocationSyncService;
import in.letsecho.echoapp.library.UserProfile;
import in.letsecho.echoapp.service.MyFirebaseInstanceIDService;

import static java.lang.Boolean.FALSE;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    public static final int RC_SIGN_IN = 1;
    private static final int MY_PERMISSION_ACCESS_COURSE_LOCATION = 2;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersDbRef;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseUser mCurrentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mUsersDbRef = mFirebaseDatabase.getReference().child("users");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Explore"));
        tabLayout.addTab(tabLayout.newTab().setText("Connections"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        checkForPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mAuthStateListener == null) {
            mAuthStateListener = getAuthStateListener();
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
            mAuthStateListener = null;
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
            case R.id.new_post:
            case R.id.new_group:
                Intent intent = new Intent(this.getApplicationContext(), CreateGroupActivity.class);
                startActivity(intent);
                break;
            case R.id.about:
                DialogFragment aboutDialog = new AboutFragment();
                aboutDialog.show(getFragmentManager(), "about");
                break;
            case R.id.my_profile:
                DialogFragment profileDialog = new UserProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putString("secondaryUserId", mCurrentUser.getUid());
                profileDialog.setArguments(bundle);
                profileDialog.show(getFragmentManager(), "userprofile");
                break;
            case R.id.sign_out_menu:
                mCurrentUser = null;
                AuthUI.getInstance().signOut(this);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private FirebaseAuth.AuthStateListener getAuthStateListener() {
        FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mCurrentUser = firebaseAuth.getCurrentUser();
                if (mCurrentUser != null) {
                    // User is signed in
                    initializeUser();
                } else {
                    // User is signed out
                    Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(loginIntent);
                }
            }
        };
        return authStateListener;
    }

    private void initializeUser() {
        FacebookSdk.sdkInitialize(this);
        UserProfile userProfile = new UserProfile(mCurrentUser);
        userProfile.saveFbData(mUsersDbRef);

        initiateLocationSyncJob();
        //Update Instance Id
        MyFirebaseInstanceIDService firebaseInstance = new MyFirebaseInstanceIDService();
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        firebaseInstance.sendRegistrationToServer(mCurrentUser.getUid(), refreshedToken);
    }

    private void initiateLocationSyncJob(){
        int locationPermission = ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION);
        checkEnableGPS();
        if (mCurrentUser != null && locationPermission == PackageManager.PERMISSION_GRANTED) {
            // Create a new dispatcher using the Google Play driver.
            FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
            dispatcher.cancel("location-update-job");
            Bundle userBundle = new Bundle();
            userBundle.putString("userId", mCurrentUser.getUid());

            Job instantJob = dispatcher.newJobBuilder()
                    .setService(LocationSyncService.class)
                    .setTag("location-update-job-instant")
                    .setExtras(userBundle)
                    .build();
            dispatcher.mustSchedule(instantJob);

            Job recurringJob = dispatcher.newJobBuilder()
                    // the JobService that will be called
                    .setService(LocationSyncService.class)
                    // uniquely identifies the job
                    .setTag("location-update-job")
                    // one-off job
                    .setRecurring(true)
                    // persist job forever
                    .setLifetime(Lifetime.FOREVER)
                    // start between 0 and 5*60 seconds from now
                    .setTrigger(Trigger.executionWindow(0, 5*60))
                    // don't overwrite an existing job with the same tag
                    .setReplaceCurrent(false)
                    // retry with exponential backoff
                    .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                    .setExtras(userBundle)
                    .build();
            dispatcher.mustSchedule(recurringJob);
        }
    }

    private void checkEnableGPS() {
        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(GpsStatus == FALSE){
            GPSFragment gpsFragment = new GPSFragment();
            gpsFragment.show(getSupportFragmentManager(), "gps");
        }
    }

    private void checkForPermissions() {
        int locationPermission = ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION);
        if ( locationPermission != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION  },
                    MY_PERMISSION_ACCESS_COURSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_COURSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    initiateLocationSyncJob();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
