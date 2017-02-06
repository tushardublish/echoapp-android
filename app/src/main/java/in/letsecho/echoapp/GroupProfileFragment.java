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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import in.letsecho.echoapp.library.Admin;
import in.letsecho.echoapp.library.FbEducation;
import in.letsecho.echoapp.library.FbWork;
import in.letsecho.echoapp.library.Group;
import in.letsecho.echoapp.library.UserProfile;

import static com.facebook.FacebookSdk.getApplicationContext;
import static java.lang.Boolean.FALSE;

public class GroupProfileFragment extends DialogFragment {

    private DatabaseReference mRootDbRef, mGroupProfileDbRef;
    private FirebaseUser mCurrentUser;
    private View mView;
    private ImageView mPhotoImageView;
    private TextView mTitleTextView, mOwnerTextView, mDescriptionTextView;
    private ImageButton mMessageOwnerButton, mCallOwnerButton, mDeleteButton;
    private Button mJoinButton;
    private String mGroupId;
    private Boolean mMember;
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
        mOwnerTextView = (TextView) mView.findViewById(R.id.ownerTextView);
        mDescriptionTextView = (TextView) mView.findViewById(R.id.descriptionTextView);
        mDeleteButton = (ImageButton) mView.findViewById(R.id.deleteImageButton);
        mJoinButton = (Button) mView.findViewById(R.id.joinButton);
        mMessageOwnerButton = (ImageButton) mView.findViewById(R.id.messageOwnerButton);
        mCallOwnerButton = (ImageButton) mView.findViewById(R.id.callOwnerButton);
        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMember = FALSE;
        Bundle bundle = getArguments();
        mGroupId = bundle.getString("groupId");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mGroupId == null || mCurrentUser == null)
            dismiss();
        mRootDbRef = FirebaseDatabase.getInstance().getReference();
        mGroupProfileDbRef = mRootDbRef.child("groups").child(mGroupId);
        mGroupProfileDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupObj) {
                final Group group = groupObj.getValue(Group.class);
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
                //Set Owner Name
                if(group.getOwnerName() != null)
                    mOwnerTextView.setText("Owner: " + group.getOwnerName());
                else
                    mOwnerTextView.setVisibility(View.GONE);
                //Set Description
                mDescriptionTextView.setText(group.getDescription());
                //Set Delete Button
                if(mCurrentUser.getUid().equals(group.getOwnerId()) || Admin.isAdmin(mCurrentUser.getUid())) {
                    mDeleteButton.setVisibility(View.VISIBLE);
                    mDeleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideGroup(group);
                            Toast.makeText(getApplicationContext(), "Group deleted successfully!", Toast.LENGTH_LONG).show();
                            dismiss();
                        }
                    });
                }
                //Set Message
                if(group.getOwnerId() != null) {
                    mMessageOwnerButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent chatIntent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                                    .putExtra("CHAT_USER", group.getOwnerId())
                                    .putExtra("TITLE", group.getOwnerName());
                            startActivity(chatIntent);
                        }
                    });
                } else {
                    mMessageOwnerButton.setVisibility(View.INVISIBLE);
                }
                //Set Phone No
                if(group.getPhoneNo() == null)
                    mCallOwnerButton.setVisibility(View.INVISIBLE);
                else {
                    mCallOwnerButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel",
                                                            group.getPhoneNo(), null));
                            startActivity(intent);
                        }
                    });
                }

                //Set Join Group
                mJoinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMember == FALSE) {
                            addUserToGroup(mCurrentUser.getUid(), group);
                            Toast.makeText(getApplicationContext(), "Following Group Conversation", Toast.LENGTH_LONG).show();
                        }
                        Intent chatIntent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                                .putExtra("CHAT_GROUP", group.getId())
                                .putExtra("TITLE", group.getTitle());
                        startActivity(chatIntent);
                    }
                });

                //Check Membership
                DatabaseReference membershipDbRef = mRootDbRef.child("chats/user_groups")
                        .child(mCurrentUser.getUid()).child(group.getId());
                membershipDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String chatId = dataSnapshot.getValue(String.class);
                        // Checking if the chat exists in groups of the user
                        if(chatId != null) {
                            mMember = Boolean.TRUE;
                            mJoinButton.setText("Enter");
                        }
                        else
                            mMember = FALSE;
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    private void addUserToGroup(String userId, Group group) {
        //Set Chat
        DatabaseReference chatInfoDbRef = mRootDbRef.child("chats/info").child(group.getChatId());
        Map<String, Object> users = new HashMap<String, Object>();
        users.put(userId, Boolean.TRUE);
        chatInfoDbRef.child("users").updateChildren(users);
        //Insert User Chat
        mRootDbRef.child("chats/user_groups").child(userId).child(group.getId()).setValue(group.getChatId());
    }

    private void hideGroup(Group group) {
        //Setting visibility to False
        Map<String, Object> visibility = new HashMap<>();
        visibility.put("visible", FALSE);
        mRootDbRef.child("groups").child(group.getId()).updateChildren(visibility);
        //Removing location
        DatabaseReference locationDbRef = mRootDbRef.child("locations/groups").child(group.getId());
        locationDbRef.setValue(null);
        //Not removing info from user_group and chat/info. Currently, the group wont be visible in explore,
        //but the conversation can still continue
    }
}
