package in.letsecho.echoapp;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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

import in.letsecho.echoapp.library.Admin;
import in.letsecho.echoapp.library.Group;

import static com.facebook.FacebookSdk.getApplicationContext;
import static java.lang.Boolean.FALSE;

public class GroupProfileFragment extends DialogFragment {

    private DatabaseReference mRootDbRef, mGroupProfileDbRef;
    private FirebaseUser mCurrentUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    private View mView;
    private ImageView mPhotoImageView;
    private TextView mTitleTextView, mOwnerTextView, mMemberTextView, mDescriptionTextView;
    private ImageButton mMessageOwnerButton, mCallOwnerButton, mEditButton, mDeleteButton;
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
        mMemberTextView = (TextView) mView.findViewById(R.id.membersTextView);
        mDescriptionTextView = (TextView) mView.findViewById(R.id.descriptionTextView);
        mEditButton = (ImageButton) mView.findViewById(R.id.editImageButton);
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
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity().getApplicationContext());
        mGroupProfileDbRef = mRootDbRef.child("groups").child(mGroupId);
        mGroupProfileDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupObj) {
                final Group group = groupObj.getValue(Group.class);
                // Set Photo
                String photoUrl = "https://scontent-amt2-1.xx.fbcdn.net/v/t1.0-9/16174591_1806158246288972_6479255994666804126_n.jpg?oh=0eb82bd852ee72ce7b8919dc2877b94d&oe=592167E2";
                if(group.getPhotoUrl() != null)
                    photoUrl = group.getPhotoUrl();
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
                //Set Member Count
                setMemberCount(group.getChatId());
                //Set Edit Button
                if(mCurrentUser.getUid().equals(group.getOwnerId()) || Admin.isAdmin(mCurrentUser.getUid())) {
                    mEditButton.setVisibility(View.VISIBLE);
                    mEditButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent editIntent = new Intent(getActivity().getApplicationContext(), CreateGroupActivity.class)
                                    .putExtra("GROUP_ID", group.getId());
                            startActivity(editIntent);
                            dismiss();
                        }
                    });
                }
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
                if(group.getOwnerId() != null && !mCurrentUser.getUid().equals(group.getOwnerId())) {
                    mMessageOwnerButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent chatIntent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                                    .putExtra("CHAT_USER", group.getOwnerId())
                                    .putExtra("TITLE", group.getOwnerName());
                            startActivity(chatIntent);
                            mFirebaseAnalytics.logEvent(getString(R.string.message_group_owner_event), new Bundle());
                        }
                    });
                } else {
                    mMessageOwnerButton.setVisibility(View.INVISIBLE);
                }
                //Set Phone No
                if(group.getPhoneNo() != null && !mCurrentUser.getUid().equals(group.getOwnerId())) {
                    mCallOwnerButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel",
                                                            group.getPhoneNo(), null));
                            startActivity(intent);
                            mFirebaseAnalytics.logEvent(getString(R.string.call_group_owner_event), new Bundle());
                        }
                    });
                } else {
                    mCallOwnerButton.setVisibility(View.INVISIBLE);
                }

                //Set Join Group
                mJoinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMember == FALSE) {
                            addUserToGroup(mCurrentUser.getUid(), group);
                            Toast.makeText(getApplicationContext(), "Following Group Conversation", Toast.LENGTH_LONG).show();
                            logJoinGroup(group);
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
                logViewEvent(group);
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

    private void setMemberCount(String chatId) {
        DatabaseReference membersDbRef = mRootDbRef.child("chats/info").child(chatId).child("users");
        membersDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long memberCount = dataSnapshot.getChildrenCount();
                mMemberTextView.setText(memberCount.toString() + " Members");
            }
            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void logViewEvent(Group group) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, group.getId());
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, group.getTitle());
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "group_profile");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
    }

    private void logJoinGroup(Group group) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.GROUP_ID, group.getId());
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.JOIN_GROUP, bundle);
    }
}
