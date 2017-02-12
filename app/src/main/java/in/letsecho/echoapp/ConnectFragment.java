package in.letsecho.echoapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
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
import java.util.List;

import in.letsecho.echoapp.library.EntityDisplayModel;
import in.letsecho.echoapp.library.Group;
import in.letsecho.echoapp.library.UserProfile;

import static android.R.color.white;
import static in.letsecho.echoapp.library.EntityDisplayModel.GROUP_TYPE;
import static in.letsecho.echoapp.library.EntityDisplayModel.USER_TYPE;

public class ConnectFragment extends Fragment {

    private ListView mPersonListView;
    private PersonAdapter mPersonAdapter;
    private ProgressBar mProgressBar;

    private FirebaseUser mCurrentUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRootDbRef;
    private Query mUserChatsQuery, mGroupChatsQuery;
    private ArrayList<Query> mChatDbRefs;
    private ChildEventListener mUserChatsEventListener, mGroupChatsEventListener;
    private ArrayList<ValueEventListener> mChatListeners;
    private List<EntityDisplayModel> mPersons;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        // Initialize references to views
        mPersonListView = (ListView) view.findViewById(R.id.personListView);
        mPersonListView.setAdapter(mPersonAdapter);
        mPersonListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EntityDisplayModel item = mPersonAdapter.getItem(position);
                if(item.getType() == USER_TYPE) {
                    Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                            .putExtra("CHAT_USER", item.getUid())
                            .putExtra("TITLE", item.getTitle());
                    startActivity(intent);
                }
                else if(item.getType() == GROUP_TYPE) {
                    Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                            .putExtra("CHAT_GROUP", item.getUid())
                            .putExtra("TITLE", item.getTitle());
                    startActivity(intent);
                }
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

        // Initialize person ListView and its adapter
        mPersons = new ArrayList<>();
        mPersonAdapter = new PersonAdapter(this.getContext(), R.layout.item_person, mPersons);
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
    }

    @Override
    public void onPause() {
        super.onPause();
        mCurrentUser = null;
        mPersonAdapter.clear();
        detachDatabaseReadListener();
        mChatDbRefs.clear();
        mChatListeners.clear();
    }

    private void attachDatabaseReadListener() {
        mUserChatsQuery = mRootDbRef.child("chats/user_chats").child(mCurrentUser.getUid()).orderByValue().limitToLast(1000);
        if (mUserChatsEventListener == null) {
            mUserChatsEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String secondaryUserId = dataSnapshot.getKey();
                    if(!secondaryUserId.equals(mCurrentUser.getUid())) {
                        String chatId = dataSnapshot.getValue(String.class);
                        addUserToList(secondaryUserId);
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

    private void addUserToList(String secondaryUserId) {
        DatabaseReference userDbRef = mRootDbRef.child("users").child(secondaryUserId);
        userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserProfile secondaryUser = dataSnapshot.getValue(UserProfile.class);
                EntityDisplayModel displayUser = new EntityDisplayModel(secondaryUser);
                mPersonAdapter.add(displayUser);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    private void addGroupToList(String groupId) {
        DatabaseReference userDbRef = mRootDbRef.child("groups").child(groupId);
        userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Group group = dataSnapshot.getValue(Group.class);
                EntityDisplayModel displayEntity = new EntityDisplayModel(group);
                mPersonAdapter.add(displayEntity);
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
                int index = EntityDisplayModel.findProfileOnUid(mPersons, entityId);
                if(index >= 0 && unreadMessageCount > 0) {
                    mPersons.get(index).setRightAlignedInfo(unreadMessageCount.toString());
                    mPersonAdapter.notifyDataSetChanged();
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
