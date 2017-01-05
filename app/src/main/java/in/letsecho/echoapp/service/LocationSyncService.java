package in.letsecho.echoapp.service;

import android.Manifest;
import android.app.Service;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import in.letsecho.echoapp.R;

public class LocationSyncService extends JobService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private static String TAG = "LocationSyncService";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5*1000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static Boolean mRequestingLocationUpdates = false;

    protected String mLastUpdateTime;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;

    @Override
    public boolean onStartJob(JobParameters job) {
        // Do some work here
        Bundle userBundle = job.getExtras();
//        String userId = userBundle.get("userId").toString();

        buildGoogleApiClient();
        mGoogleApiClient.connect();

        Log.i(TAG, "Job is running");
        return false; // Answers the question: "Is there still work going on?"
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        mGoogleApiClient.disconnect();
        return false; // Answers the question: "Should this job be retried?"
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient===");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        int locationPermission = ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION);
        if ( locationPermission == PackageManager.PERMISSION_GRANTED ) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mCurrentLocation != null) {
                Log.d(TAG, "Latitude:==" + mCurrentLocation.getLatitude() + "\n Longitude:==" + mCurrentLocation.getLongitude());
            }
        }
        Log.i(TAG, "Connection connected==");
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended==");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }
}

