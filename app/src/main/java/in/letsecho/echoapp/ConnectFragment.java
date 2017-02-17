package in.letsecho.echoapp;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import in.letsecho.echoapp.library.EntityDisplayModel;
import in.letsecho.echoapp.library.Group;
import in.letsecho.echoapp.library.UserConnection;
import in.letsecho.echoapp.library.UserProfile;

import static in.letsecho.echoapp.library.EntityDisplayModel.GROUP_TYPE;
import static in.letsecho.echoapp.library.EntityDisplayModel.USER_TYPE;
import static in.letsecho.echoapp.library.UserConnection.CONNECTED;
import static in.letsecho.echoapp.library.UserConnection.REQUEST_RECEIVED;

public class ConnectFragment extends Fragment {

    private static String HEADER1 = "New Requests";
    private static String HEADER2 = "Existing Connections";
    private ExpandableListView mConnectionsListView;
    private PersonAdapterExpandableList mConnectAdapter;
    private ProgressBar mProgressBar;

    private FirebaseUser mCurrentUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRootDbRef, mUserConnectionsDbRef;
    private Query mUserChatsQuery, mGroupChatsQuery;
    private ArrayList<Query> mChatDbRefs;
    private ChildEventListener mUserChatsEventListener, mGroupChatsEventListener;
    private ArrayList<ValueEventListener> mChatListeners;
    private List<EntityDisplayModel> mExistingConnections, mNewRequests;
    private List<String> mSectionHeaders;
    private HashMap<String, List<EntityDisplayModel>> mExpandableList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        // Initialize references to views
        mConnectionsListView = (ExpandableListView) view.findViewById(R.id.personListView);
        mConnectionsListView.setAdapter(mConnectAdapter);
        mConnectionsListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                EntityDisplayModel profile = null;
                switch(groupPosition) {
                    case 0:
                        profile = mNewRequests.get(childPosition);
                        // Open Profile
                        if(profile.getType() == USER_TYPE) {
                            DialogFragment profileDialog = new UserProfileFragment();
                            Bundle bundle = new Bundle();
                            bundle.putString("secondaryUserId", profile.getUid());
                            profileDialog.setArguments(bundle);
                            profileDialog.show(getActivity().getFragmentManager(), "userprofile");
                        }
                        else if (profile.getType() == GROUP_TYPE) {
                            DialogFragment profileDialog = new GroupProfileFragment();
                            Bundle bundle = new Bundle();
                            bundle.putString("groupId", profile.getUid());
                            profileDialog.setArguments(bundle);
                            profileDialog.show(getActivity().getFragmentManager(), "groupprofile");
                        }
                        break;
                    case 1:
                        // Open Chat
                        profile = mExistingConnections.get(childPosition);
                        if(profile.getType() == USER_TYPE) {
                            Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                                    .putExtra("CHAT_USER", profile.getUid())
                                    .putExtra("TITLE", profile.getTitle());
                            startActivity(intent);
                        }
                        else if(profile.getType() == GROUP_TYPE) {
                            Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                                    .putExtra("CHAT_GROUP", profile.getUid())
                                    .putExtra("TITLE", profile.getTitle());
                            startActivity(intent);
                        }
                        break;
                }
                return true;
            }
        });

        // Initialize progress bar
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase components
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mRootDbRef = mFirebaseDatabase.getReference();
        mUserConnectionsDbRef = mRootDbRef.child("chats/user_connections");

        // Initialize person ListView and its adapter
        setupExpandableList();
    }

    private void setupExpandableList() {
        mNewRequests = new ArrayList<>();
        mExistingConnections = new ArrayList<>();
        mSectionHeaders = new ArrayList<>();
        mSectionHeaders.add(HEADER1);
        mSectionHeaders.add(HEADER2);
        mExpandableList = new HashMap<>();
        mExpandableList.put(HEADER1, mNewRequests);
        mExpandableList.put(HEADER2, mExistingConnections);
        mConnectAdapter = new PersonAdapterExpandableList(this.getContext(), mSectionHeaders, mExpandableList);
    }

    @Override
    public void onResume() {
        super.onResume();
        mChatDbRefs = new ArrayList<Query>();
        mChatListeners = new ArrayList<ValueEventListener>();
        mCurrentUser = mFirebaseAuth.getCurrentUser();
        if(mCurrentUser != null) {
            attachDatabaseReadListener();
        }
        mConnectionsListView.expandGroup(1);
    }

    @Override
    public void onPause() {
        super.onPause();
        mNewRequests.clear();
        mExistingConnections.clear();
        mConnectAdapter.notifyDataSetChanged();
        mCurrentUser = null;
        detachDatabaseReadListener();
        mChatDbRefs.clear();
        mChatListeners.clear();
    }

    private void attachDatabaseReadListener() {
        mUserChatsQuery = mUserConnectionsDbRef.child(mCurrentUser.getUid()).orderByValue().limitToLast(1000);
        if (mUserChatsEventListener == null) {
            mUserChatsEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String secondaryUserId = dataSnapshot.getKey();
                    UserConnection userConnection = dataSnapshot.getValue(UserConnection.class);
                    if(!secondaryUserId.equals(mCurrentUser.getUid())) {
                        addUserToList(secondaryUserId, userConnection);
                        String chatId = userConnection.getChatId();
                        if(chatId != null)      // chatId will be null for new connections, until accepted
                            setChatListener(chatId, secondaryUserId);
                    }
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            mUserChatsQuery.addChildEventListener(mUserChatsEventListener);
        }

        mGroupChatsQuery = mRootDbRef.child("chats/user_groups").child(mCurrentUser.getUid()).orderByValue().limitToLast(1000);
        if (mGroupChatsEventListener == null) {
            mGroupChatsEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String groupId = dataSnapshot.getKey();
                    String chatId = dataSnapshot.getValue(String.class);
                    addGroupToList(groupId);
                    setChatListener(chatId, groupId);
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            mGroupChatsQuery.addChildEventListener(mGroupChatsEventListener);
        }
    }

    private void addUserToList(String secondaryUserId, UserConnection userConnection) {
        final String status = userConnection.getStatus();
        if(status.equals(CONNECTED) || status.equals(REQUEST_RECEIVED)) {
            DatabaseReference userDbRef = mRootDbRef.child("users").child(secondaryUserId);
            userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    UserProfile secondaryUser = dataSnapshot.getValue(UserProfile.class);
                    EntityDisplayModel displayUser = new EntityDisplayModel(secondaryUser);
                    if(status.equals(CONNECTED)) {
                        mExistingConnections.add(displayUser);
                    }
                    else if(status.equals(REQUEST_RECEIVED)) {
                        mNewRequests.add(displayUser);
                        // Update Header
                        String oldHeader = mSectionHeaders.get(0);
                        String newHeader = HEADER1 + " (" + mNewRequests.size() + ")";
                        mSectionHeaders.set(0, newHeader);
                        mExpandableList.put(newHeader, mExpandableList.remove(oldHeader));
                    }
                    mConnectAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            }));
        }
    }

    private void addGroupToList(String groupId) {
        DatabaseReference userDbRef = mRootDbRef.child("groups").child(groupId);
        userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Group group = dataSnapshot.getValue(Group.class);
                EntityDisplayModel displayEntity = new EntityDisplayModel(group);
                mExistingConnections.add(displayEntity);
                mConnectAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    //Adding Listeners to each chat to get unread messages count
    private void setChatListener(String chatId, final String entityId) {
        Query chatDbRef = mRootDbRef.child("chats/messages").child(chatId)
                .orderByChild("seen/"+ mCurrentUser.getUid()).equalTo(null);
        ValueEventListener chatListener = chatDbRef.addValueEventListener((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long unreadMessageCount = dataSnapshot.getChildrenCount();
                int index = EntityDisplayModel.findProfileOnUid(mExistingConnections, entityId);
                if(index >= 0 && unreadMessageCount > 0) {
                    mExistingConnections.get(index).setRightAlignedInfo(unreadMessageCount.toString());
                    mConnectAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
        mChatDbRefs.add(chatDbRef);
        mChatListeners.add(chatListener);
    }

    private void detachDatabaseReadListener() {
        if (mUserChatsEventListener != null) {
            mUserChatsQuery.removeEventListener(mUserChatsEventListener);
            mUserChatsEventListener = null;
        }

        if (mGroupChatsEventListener != null) {
            mGroupChatsQuery.removeEventListener(mGroupChatsEventListener);
            mGroupChatsEventListener = null;
        }

        for(int i = 0; i< mChatDbRefs.size(); i++){
            mChatDbRefs.get(i).removeEventListener(mChatListeners.get(i));
        }
    }
}
