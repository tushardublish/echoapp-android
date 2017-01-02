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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import in.letsecho.library.UserProfile;

/**
 * Created by Tushar on 02-01-2017.
 */

public class ConnectFragment extends Fragment {

    FirebaseUser currentUser;

    private ListView personListView;
    private PersonAdapter personAdapter;
    private ProgressBar progressBar;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference rootDbRef, userChatsDbRef;
    private ChildEventListener userChatsEventListener;
    private FirebaseAuth firebaseAuth;

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
                        .putExtra(Intent.EXTRA_USER, person.getUID());
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
        List<UserProfile> persons = new ArrayList<>();
        personAdapter = new PersonAdapter(this.getContext(), R.layout.item_person, persons);
    }

    @Override
    public void onResume() {
        super.onResume();
        currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null) {
            attachDatabaseReadListener();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        currentUser = null;
        personAdapter.clear();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        userChatsDbRef = rootDbRef.child("chats/user_chats").child(currentUser.getUid());
        if (userChatsEventListener == null) {
            userChatsEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String secondaryUserId = dataSnapshot.getKey();
                    String chatId = dataSnapshot.getValue(String.class);

                    DatabaseReference userDbRef = rootDbRef.child("users").child(secondaryUserId);
                    userDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            UserProfile secondaryUser = dataSnapshot.getValue(UserProfile.class);
                            personAdapter.add(secondaryUser);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {}
                    }));
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            userChatsDbRef.addChildEventListener(userChatsEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (userChatsEventListener != null) {
            userChatsDbRef.removeEventListener(userChatsEventListener);
            userChatsEventListener = null;
        }
    }
}
