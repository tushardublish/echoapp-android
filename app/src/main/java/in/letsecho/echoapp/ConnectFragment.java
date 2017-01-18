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

/**
 * Created by Tushar on 02-01-2017.
 */

public class ConnectFragment extends Fragment {

    FirebaseUser currentUser;

    private ListView personListView;
    private PersonAdapter personAdapter;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference rootDbRef;
    private Query userChatsDbRef;
    private ArrayList<Query> chatDbRefs;
    private ChildEventListener userChatsEventListener;
    private ArrayList<ValueEventListener> chatListeners;

    List<UserDisplayModel> persons;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        // Initialize references to views
        personListView = (ListView) view.findViewById(R.id.personListView);
        personListView.setAdapter(personAdapter);
        personListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserProfile person = personAdapter.getItem(position);
                Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                        .putExtra("CHAT_USER", person.getUID());
                startActivity(intent);
            }
        });

        // Initialize progress bar
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase components
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        rootDbRef = firebaseDatabase.getReference();

        // Initialize person ListView and its adapter
        persons = new ArrayList<>();
        personAdapter = new PersonAdapter(this.getContext(), R.layout.item_person, persons);
    }

    @Override
    public void onResume() {
        super.onResume();
        currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null) {
            chatDbRefs = new ArrayList<Query>();
            chatListeners = new ArrayList<ValueEventListener>();
            attachDatabaseReadListener();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        currentUser = null;
        personAdapter.clear();
        detachDatabaseReadListener();
        chatDbRefs.clear();
        chatListeners.clear();
    }

    private void attachDatabaseReadListener() {
        userChatsDbRef = rootDbRef.child("chats/user_chats").child(currentUser.getUid()).orderByValue().limitToLast(1000);
        if (userChatsEventListener == null) {
            userChatsEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String secondaryUserId = dataSnapshot.getKey();
                    String chatId = dataSnapshot.getValue(String.class);
                    addUserToList(secondaryUserId);
                    setChatListener(chatId, secondaryUserId);
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            userChatsDbRef.addChildEventListener(userChatsEventListener);
        }
    }

    private void addUserToList(String secondaryUserId) {
        DatabaseReference userDbRef = rootDbRef.child("users").child(secondaryUserId);
        userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserDisplayModel secondaryUser = dataSnapshot.getValue(UserDisplayModel.class);
                personAdapter.add(secondaryUser);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
    }

    //Adding Listeners to each chat to get unread messages count
    private void setChatListener(String chatId, final String secondaryUserId) {
        Query chatDbRef = rootDbRef.child("chats/messages").child(chatId)
                .orderByChild("seen/"+currentUser.getUid()).equalTo(null);
        ValueEventListener chatListener = chatDbRef.addValueEventListener((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long unreadMessageCount = dataSnapshot.getChildrenCount();
                int index = UserDisplayModel.findProfileOnUid(persons, secondaryUserId);
                if(index >= 0 && unreadMessageCount > 0) {
                    persons.get(index).setRightAlignedInfo(unreadMessageCount.toString());
                    personAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
        chatDbRefs.add(chatDbRef);
        chatListeners.add(chatListener);
    }

    private void detachDatabaseReadListener() {
        if (userChatsEventListener != null) {
            userChatsDbRef.removeEventListener(userChatsEventListener);
            userChatsEventListener = null;
        }

        for(int i=0; i< chatDbRefs.size(); i++){
            chatDbRefs.get(i).removeEventListener(chatListeners.get(i));
        }
    }
}
