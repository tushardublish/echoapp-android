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
import in.letsecho.echoapp.library.FbEducation;
import in.letsecho.echoapp.library.FbWork;
import in.letsecho.echoapp.library.UserProfile;

public class ProfileFragment extends DialogFragment {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUserProfileDbRef;
    private View mView;
    private ImageView mPhotoImageView;
    private TextView mNameTextView, mWorkTextView, mEduTextView;
    private Button mConnectButton;
    private ImageButton mFbButton;
    private String mUserId;
    private int max_image_size;

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
        mEduTextView = (TextView) mView.findViewById(R.id.eduTextView);
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
                // Set Photo
                if (user.getPhotoUrl() != null) {
                    Glide.with(mPhotoImageView.getContext())
                            .load(user.getPhotoUrl())
                            .into(mPhotoImageView);
                }
                max_image_size = mView.getWidth();
                mPhotoImageView.setMaxWidth(max_image_size);
                mPhotoImageView.setMaxHeight(max_image_size);
                //Set Name
                mNameTextView.setText(user.getName());
                // Set Work
                for(DataSnapshot workObj: userObj.child("fbdata/work").getChildren()) {
                    FbWork work = workObj.getValue(FbWork.class);
                    mWorkTextView.setText(work.getPosition() + " at " + work.getEmployer());
                    break;
                }
                //Set Education
                FbEducation finalEdu = null;
                for(DataSnapshot eduObj: userObj.child("fbdata/education").getChildren()) {
                    FbEducation edu = eduObj.getValue(FbEducation.class);
                    if(finalEdu == null)
                        finalEdu = edu;
                    if(edu.getYear() != null && finalEdu.getYear() != null &&
                            Integer.parseInt(edu.getYear()) > Integer.parseInt(finalEdu.getYear())) {
                        finalEdu = edu;
                    }
                }
                if(finalEdu != null)
                    mEduTextView.setText(finalEdu.getSchool());
                // Set Fb link
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
        String facebookUrl = "https://www.facebook.com/" + profile_id;
        try{
            // open in Facebook app
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=" + facebookUrl));
        } catch (Exception e) {
            // open in browser
            return new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl));
        }
    }
}