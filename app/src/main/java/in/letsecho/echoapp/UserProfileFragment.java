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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import in.letsecho.echoapp.library.FbEducation;
import in.letsecho.echoapp.library.FbWork;
import in.letsecho.echoapp.library.UserConnection;
import in.letsecho.echoapp.library.UserProfile;

import static com.facebook.FacebookSdk.getApplicationContext;
import static in.letsecho.echoapp.library.UserConnection.CONNECTED;
import static in.letsecho.echoapp.library.UserConnection.REQUEST_RECEIVED;
import static in.letsecho.echoapp.library.UserConnection.REQUEST_RECEIVED_BLOCKED;
import static in.letsecho.echoapp.library.UserConnection.REQUEST_RECEVIED_REJECTED;
import static in.letsecho.echoapp.library.UserConnection.REQUEST_SENT;
import static in.letsecho.echoapp.library.UserConnection.REQUEST_SENT_BLOCKED;
import static in.letsecho.echoapp.library.UserConnection.REQUEST_SENT_REJECTED;
import static java.lang.Boolean.TRUE;

public class UserProfileFragment extends DialogFragment {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRootDbRef, mUserConnectionRootDbRef, mPrimaryUserConnectionDbRef, mSecondaryUserConnectionDbRef;
    private ValueEventListener mUserConnectionEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseUser mCurrentUser;
    private View mView;
    private ImageView mPhotoImageView;
    private TextView mNameTextView, mWorkTextView, mEduTextView;
    private Button mConnectButton;
    private ImageButton mFbButton, mMessageButton, mAcceptButton, mRejectButton, mBlockButton;
    private String mSecondaryUserId;
    private UserProfile mSecondaryUser;
    private UserConnection mUserConnection;
    private int max_image_size;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = this.getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fragment_userprofile, null);
        builder.setView(mView);

        // Get mView items
        mPhotoImageView = (ImageView) mView.findViewById(R.id.displayImageView);
        mNameTextView = (TextView) mView.findViewById(R.id.nameTextView);
        mWorkTextView = (TextView) mView.findViewById(R.id.workTextView);
        mEduTextView = (TextView) mView.findViewById(R.id.eduTextView);
        mFbButton = (ImageButton) mView.findViewById(R.id.fbButton);
        mConnectButton = (Button) mView.findViewById(R.id.connectButton);
        mMessageButton = (ImageButton) mView.findViewById(R.id.messageButton);
        mAcceptButton = (ImageButton) mView.findViewById(R.id.acceptButton);
        mRejectButton = (ImageButton) mView.findViewById(R.id.rejectButton);
        mBlockButton = (ImageButton) mView.findViewById(R.id.blockButton);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For current user
                UserConnection primaryConnection = new UserConnection();
                primaryConnection.setStatus(REQUEST_SENT);
                mPrimaryUserConnectionDbRef.setValue(primaryConnection);
                // For the other user
                UserConnection secondaryConnection = new UserConnection();
                secondaryConnection.setStatus(REQUEST_RECEIVED);
                mSecondaryUserConnectionDbRef.setValue(secondaryConnection);
            }
        });
        mAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateConnectionStatus(mCurrentUser.getUid(), mSecondaryUserId, CONNECTED);
                updateConnectionStatus(mSecondaryUserId, mCurrentUser.getUid(), CONNECTED);
                if(mUserConnection.getChatId() == null)
                    createAndInsertNewChat();
                Toast.makeText(getApplicationContext(), "Request Accepted", Toast.LENGTH_SHORT).show();
            }
        });

        mRejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateConnectionStatus(mCurrentUser.getUid(), mSecondaryUserId, REQUEST_RECEVIED_REJECTED);
                updateConnectionStatus(mSecondaryUserId, mCurrentUser.getUid(), REQUEST_SENT_REJECTED);
                Toast.makeText(getApplicationContext(), "Request Rejected", Toast.LENGTH_SHORT).show();
            }
        });

        mBlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateConnectionStatus(mCurrentUser.getUid(), mSecondaryUserId, REQUEST_RECEIVED_BLOCKED);
                updateConnectionStatus(mSecondaryUserId, mCurrentUser.getUid(), REQUEST_SENT_BLOCKED);
                Toast.makeText(getApplicationContext(),
                    "The other user is blocked now. Blocked users cannot open your profile or send you requests again.",
                    Toast.LENGTH_LONG).show();
            }
        });

        mMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatIntent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                        .putExtra("CHAT_USER", mSecondaryUserId)
                        .putExtra("TITLE", mSecondaryUser.getName());
                startActivity(chatIntent);
                mFirebaseAnalytics.logEvent(getString(R.string.connect_with_user_event), new Bundle());
            }
        });
        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mSecondaryUserId = bundle.getString("secondaryUserId");
        if(mSecondaryUserId == null)
            dismiss();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mRootDbRef = mFirebaseDatabase.getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentUser = mFirebaseAuth.getCurrentUser();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity().getApplicationContext());
        mUserConnectionRootDbRef = mRootDbRef.child("chats/user_connections");
        mPrimaryUserConnectionDbRef = mUserConnectionRootDbRef.child(mCurrentUser.getUid()).child(mSecondaryUserId);
        mSecondaryUserConnectionDbRef = mUserConnectionRootDbRef.child(mSecondaryUserId).child(mCurrentUser.getUid());
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mUserConnectionEventListener == null) {
            // Not a single value event listener because, it should get updated if the status is changed
            mUserConnectionEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mUserConnection = dataSnapshot.getValue(UserConnection.class);
                    updateActionButtons();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mPrimaryUserConnectionDbRef.addValueEventListener(mUserConnectionEventListener);
        }

        DatabaseReference mUserProfileDbRef = mRootDbRef.child("users").child(mSecondaryUserId);
        mUserProfileDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userObj) {
                mSecondaryUser = userObj.getValue(UserProfile.class);
                displayUserProfile(userObj);
                logViewEvent();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mUserConnectionEventListener != null) {
            mPrimaryUserConnectionDbRef.removeEventListener(mUserConnectionEventListener);
            mUserConnectionEventListener = null;
        }
    }

    private void updateConnectionStatus(String primaryId, String secondaryId, String status) {
        DatabaseReference connectionStatusDbRef = mUserConnectionRootDbRef.child(primaryId).child(secondaryId)
                .child("status");
        connectionStatusDbRef.setValue(status);
    }

    private void updateActionButtons() {
        if(mUserConnection != null) {
            mConnectButton.setVisibility(View.GONE);
            switch (mUserConnection.getStatus()) {
                case CONNECTED:
                    setActionButtonsVisibility(View.GONE);
                    mMessageButton.setVisibility(View.VISIBLE);
                    // Fb button visible on connected and all received types and none sent types
                    mFbButton.setVisibility(View.VISIBLE);
                    break;
                case REQUEST_SENT:
                    mConnectButton.setVisibility(View.VISIBLE);
                    mConnectButton.setEnabled(false);
                    break;
                case REQUEST_RECEIVED:
                    setActionButtonsVisibility(View.VISIBLE);
                    mFbButton.setVisibility(View.VISIBLE);
                    break;
                case REQUEST_SENT_REJECTED:
                    break;
                case REQUEST_RECEVIED_REJECTED:
                    setActionButtonsVisibility(View.VISIBLE);
                    mRejectButton.setAlpha((float) 1.0);
                    mAcceptButton.setAlpha((float) 0.2);
                    mBlockButton.setAlpha((float) 0.2);
                    mFbButton.setVisibility(View.VISIBLE);
                    break;
                case REQUEST_SENT_BLOCKED:
                    dismiss();
                    break;
                case REQUEST_RECEIVED_BLOCKED:
                    setActionButtonsVisibility(View.VISIBLE);
                    mBlockButton.setAlpha((float) 1.0);
                    mAcceptButton.setAlpha((float) 0.2);
                    mRejectButton.setAlpha((float) 0.2);
                    mFbButton.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void createAndInsertNewChat() {
        //Insert Info
        DatabaseReference chatInfoDbRef = mRootDbRef.child("chats/info").push();
        Map<String, Object> users = new HashMap<String, Object>();
        users.put(mCurrentUser.getUid(), TRUE);
        users.put(mSecondaryUserId, TRUE);
        chatInfoDbRef.child("users").setValue(users);
        //Insert in User Connections
        String chatId = chatInfoDbRef.getKey();
        mPrimaryUserConnectionDbRef.child("chatId").setValue(chatId);
        mSecondaryUserConnectionDbRef.child("chatId").setValue(chatId);
    }

    private void setActionButtonsVisibility(int visibility) {
        mAcceptButton.setVisibility(visibility);
        mRejectButton.setVisibility(visibility);
        mBlockButton.setVisibility(visibility);
    }

    private void displayUserProfile(DataSnapshot userObj) {
        // Set Photo
        if (mSecondaryUser.getPhotoUrl() != null) {
            Glide.with(mPhotoImageView.getContext())
                    .load(mSecondaryUser.getPhotoUrl())
                    .into(mPhotoImageView);
        }
        max_image_size = mView.getWidth();
        mPhotoImageView.setMaxWidth(max_image_size);
        mPhotoImageView.setMaxHeight(max_image_size);
        //Set Name
        mNameTextView.setText(mSecondaryUser.getName());
        // Set Work
        for(DataSnapshot workObj: userObj.child("fbdata/work").getChildren()) {
            FbWork work = workObj.getValue(FbWork.class);
            if(work.getPosition() != null && work.getEmployer() != null)
                mWorkTextView.setText(work.getPosition() + " at " + work.getEmployer());
            else if(work.getPosition() != null)
                mWorkTextView.setText(work.getPosition());
            else if(work.getEmployer() != null)
                mWorkTextView.setText(work.getEmployer());
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
                    mFirebaseAnalytics.logEvent(getString(R.string.view_facebook_profile_event), new Bundle());
                }
            });
        // Connect Button
        if(mCurrentUser.getUid().equals(mSecondaryUser.getUID()))
            mConnectButton.setVisibility(View.INVISIBLE);
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

    private void logViewEvent() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mSecondaryUser.getUID());
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, mSecondaryUser.getName());
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "user_profile");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
    }
}
