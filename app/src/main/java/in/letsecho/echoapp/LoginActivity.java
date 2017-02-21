package in.letsecho.echoapp;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import in.letsecho.echoapp.library.UserProfile;


public class LoginActivity extends FragmentActivity {
    public static final int RC_SIGN_IN = 1;
    private static final int NUM_PAGES = 5;

    private FirebaseUser mCurrentUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersDbRef;

    // The pager widget, which handles animation and allows swiping horizontally to access previous and next steps.
    private ViewPager mPager;
    // The pager adapter, which provides the pages to the view pager widget.
    private PagerAdapter mPagerAdapter;
    private TextView[] dots;
    private LinearLayout dotsLayout;
    private Button mLoginButton;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mUsersDbRef = mFirebaseDatabase.getReference().child("users");

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                addBottomDots(position);
            }
        });
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mLoginButton = (Button) findViewById(R.id.button_facebook_login);
        final AuthUI.IdpConfig facebookIdp = new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER)
                .setPermissions(Arrays.asList("user_education_history", "user_work_history"))
                .build();
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressBar.setVisibility(View.VISIBLE);
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(false)
                                .setProviders(Arrays.asList(facebookIdp))
                                .build(),
                        RC_SIGN_IN);
            }
        });
        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
        addBottomDots(0);
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

    private FirebaseAuth.AuthStateListener getAuthStateListener() {
        FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mCurrentUser = firebaseAuth.getCurrentUser();
                if (mCurrentUser != null) {
                    // User is signed in
                    finish();
                }
            }
        };
        return authStateListener;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                // Saving user info in DB. To do: Should not happen on every sign in
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
                mCurrentUser = mFirebaseAuth.getCurrentUser();
                UserProfile userProfile = new UserProfile(mCurrentUser);
                Map<String, Object> userMap = new HashMap<>();
                userMap.put(mCurrentUser.getUid(), userProfile);
                mUsersDbRef.updateChildren(userMap);
                setDefaultLocation();
                mProgressBar.setVisibility(View.INVISIBLE);
                finish();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    // Setting default location to ground zero for new users.
    // Should be done only on signups and not signins
    private void setDefaultLocation() {
        DatabaseReference locationDbRef = mFirebaseDatabase.getReference("locations/current");
        GeoFire geoFire = new GeoFire(locationDbRef);
        GeoLocation currentGeoLocation = new GeoLocation(28.529449, 77.366762);
        geoFire.setLocation(mCurrentUser.getUid(), currentGeoLocation);
    }
    /**
     * A simple pager adapter that represents 5 {@link LoginIntroFragment} objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter{

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return LoginIntroFragment.create(position);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    private void addBottomDots(int currentPage) {
        dots = new TextView[NUM_PAGES];

        int colorsActive = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);
        int colorsInactive = ContextCompat.getColor(getApplicationContext(), R.color.grey_300);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorsInactive);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(colorsActive);
    }
}
