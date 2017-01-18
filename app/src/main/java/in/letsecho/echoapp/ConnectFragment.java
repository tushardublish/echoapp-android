package in.letsecho.echoapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import in.letsecho.echoapp.library.UserDisplayModel;
import in.letsecho.echoapp.library.UserProfile;

public class ConnectFragment extends Fragment {

    private ListView mPersonListView;
    private PersonAdapter mPersonAdapter;
    private ProgressBar mProgressBar;

    private FirebaseUser mCurrentUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRootDbRef;
    private Query mUserChatsDbRef;
    private ArrayList<Query> mChatDbRefs;
    private ChildEventListener mUserChatsEventListener;
    private ArrayList<ValueEventListener> mChatListeners;
    private List<UserDisplayModel> mPersons;

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
                UserProfile person = mPersonAdapter.getItem(position);
                Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                        .putExtra("CHAT_USER", person.getUID());
                startActivity(intent);
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
        mUserChatsDbRef = mRootDbRef.child("chats/user_chats").child(mCurrentUser.getUid()).orderByValue().limitToLast(1000);
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
            mUserChatsDbRef.addChildEventListener(mUserChatsEventListener);
        }
    }

    private void addUserToList(String secondaryUserId) {
        DatabaseReference userDbRef = mRootDbRef.child("users").child(secondaryUserId);
        userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserDisplayModel secondaryUser = dataSnapshot.getValue(UserDisplayModel.class);
                mPersonAdapter.add(secondaryUser);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    //Adding Listeners to each chat to get unread messages count
    private void setChatListener(String chatId, final String secondaryUserId) {
        Query chatDbRef = mRootDbRef.child("chats/messages").child(chatId)
                .orderByChild("seen/"+ mCurrentUser.getUid()).equalTo(null);
        ValueEventListener chatListener = chatDbRef.addValueEventListener((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long unreadMessageCount = dataSnapshot.getChildrenCount();
                int index = UserDisplayModel.findProfileOnUid(mPersons, secondaryUserId);
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
            mUserChatsDbRef.removeEventListener(mUserChatsEventListener);
            mUserChatsEventListener = null;
        }

        for(int i = 0; i< mChatDbRefs.size(); i++){
            mChatDbRefs.get(i).removeEventListener(mChatListeners.get(i));
        }
    }
}
