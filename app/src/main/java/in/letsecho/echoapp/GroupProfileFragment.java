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
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import in.letsecho.echoapp.library.FbEducation;
import in.letsecho.echoapp.library.FbWork;
import in.letsecho.echoapp.library.Group;
import in.letsecho.echoapp.library.UserProfile;

public class GroupProfileFragment extends DialogFragment {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mGroupProfileDbRef;
    private View mView;
    private ImageView mPhotoImageView;
    private TextView mTitleTextView, mDescriptionTextView;
    private Button mContactOwnerButton, mJoinButton;
    private String mGroupId;
    private int max_image_size;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = this.getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fragment_groupprofile, null);
        builder.setView(mView);

        // Get mView items
        mPhotoImageView = (ImageView) mView.findViewById(R.id.displayImageView);
        mTitleTextView = (TextView) mView.findViewById(R.id.titleTextView);
        mDescriptionTextView = (TextView) mView.findViewById(R.id.descriptionTextView);
        mJoinButton = (Button) mView.findViewById(R.id.joinButton);
//        mConnectButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent chatIntent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
//                        .putExtra("CHAT_USER", mUserId);
//                startActivity(chatIntent);
//            }
//        });
        mContactOwnerButton = (Button) mView.findViewById(R.id.contactOwnerButton);
        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mGroupId = bundle.getString("groupId");
        if(mGroupId == null)
            dismiss();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mGroupProfileDbRef = mFirebaseDatabase.getReference("groups").child(mGroupId);
        mGroupProfileDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupObj) {
                Group group = groupObj.getValue(Group.class);
                // Set Photo
                String photoUrl = "https://scontent-amt2-1.xx.fbcdn.net/v/t1.0-9/16174591_1806158246288972_6479255994666804126_n.jpg?oh=0eb82bd852ee72ce7b8919dc2877b94d&oe=592167E2";
                Glide.with(mPhotoImageView.getContext())
                        .load(photoUrl)
                        .into(mPhotoImageView);
                max_image_size = mView.getWidth();
                mPhotoImageView.setMaxWidth(max_image_size);
                mPhotoImageView.setMaxHeight(max_image_size);
                //Set Name
                mTitleTextView.setText(group.getTitle());
                //Set Description
                mDescriptionTextView.setText(group.getDescription());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }
}
