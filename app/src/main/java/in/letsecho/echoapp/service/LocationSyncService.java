package in.letsecho.echoapp.service;

import android.app.Service;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class LocationSyncService extends JobService {
    private static String TAG = "LocationSyncService";
    @Override
    public boolean onStartJob(JobParameters job) {
        // Do some work here
        Log.i(TAG, "Job is running");
        return false; // Answers the question: "Is there still work going on?"
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false; // Answers the question: "Should this job be retried?"
    }
}
