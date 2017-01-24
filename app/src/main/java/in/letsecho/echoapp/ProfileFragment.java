package in.letsecho.echoapp;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import in.letsecho.echoapp.adapter.WorkAdapter;
import in.letsecho.echoapp.library.FbWork;
import in.letsecho.echoapp.library.UserProfile;

public class ProfileFragment extends DialogFragment {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUserProfileDbRef;
    private String mUserId;
    private View mView;
    private ImageView mPhotoImageView;
    private TextView mNameTextView, mWorkTextView;
    private Button mConnectButton;
    private ImageButton mFbButton;
//    private ListView mWorkListView;
//    private ArrayList<FbWork> workList;
//    private WorkAdapter mWorkAdapter;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = this.getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fragment_profile, null);
        builder.setView(mView);
        // Get mView items
        mPhotoImageView = (ImageView) mView.findViewById(R.id.displayImageView);
        mNameTextView = (TextView) mView.findViewById(R.id.nameTextView);
        mWorkTextView = (TextView) mView.findViewById(R.id.workTextView);
        mConnectButton = (Button) mView.findViewById(R.id.connectButton);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatIntent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                        .putExtra("CHAT_USER", mUserId);
                startActivity(chatIntent);
            }
        });
        mFbButton = (ImageButton) mView.findViewById(R.id.fbButton);
//        mWorkListView = (ListView) mView.findViewById(R.id.workListView);
//        workList = new ArrayList<>();
//        mWorkAdapter = new WorkAdapter(this.getActivity(), R.layout.item_work, workList);
//        mWorkListView.setAdapter(mWorkAdapter);
        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mUserId = bundle.getString("secondaryUserId");

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUserProfileDbRef = mFirebaseDatabase.getReference("users").child(mUserId);
        mUserProfileDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userObj) {
                UserProfile user = userObj.getValue(UserProfile.class);
                if (user.getPhotoUrl() != null) {
                    Glide.with(mPhotoImageView.getContext())
                            .load(user.getPhotoUrl())
                            .into(mPhotoImageView);
                }
                mNameTextView.setText(user.getName());
                for(DataSnapshot workObj: userObj.child("fbdata/work").getChildren()) {
                    FbWork work = workObj.getValue(FbWork.class);
                    mWorkTextView.setText(work.getPosition() + " at " + work.getEmployer());
                    break;
                }
                final String fbProfileId = userObj.child("fbdata/id").getValue(String.class);
                if(fbProfileId != null)
                mFbButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent fbIntent = getOpenFacebookIntent(getActivity().getApplicationContext(),
                                                                fbProfileId);
                        startActivity(fbIntent);
                    }
                });
                else {
                    mFbButton.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    public static Intent getOpenFacebookIntent(Context context, String profile_id) {
        try{
            // open in Facebook app
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + profile_id));
        } catch (Exception e) {
            // open in browser
            return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + profile_id));
        }
    }
}
