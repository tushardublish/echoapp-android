package in.letsecho.echoapp;

import android.app.DialogFragment;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
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
import in.letsecho.echoapp.library.UserProfile;

import static in.letsecho.echoapp.library.EntityDisplayModel.GROUP_TYPE;
import static in.letsecho.echoapp.library.EntityDisplayModel.USER_TYPE;

public class GroupMemberActivity extends AppCompatActivity {

    private String mChatId, mGroupId, mTitle;
    private ListView mPersonListView;
    private PersonAdapter mPersonAdapter;
    private Toolbar mToolbar;
    private ProgressBar mProgressBar;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRootDbRef, mChatMembersDbRef;
    private List<EntityDisplayModel> mPersons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_member);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mRootDbRef = mFirebaseDatabase.getReference();

        // Initialize person ListView and its adapter
        mPersons = new ArrayList<>();
        mPersonAdapter = new PersonAdapter(this, R.layout.item_person, mPersons);

        // Initialize references to views
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPersonListView = (ListView) findViewById(R.id.personListView);
        mPersonListView.setAdapter(mPersonAdapter);
        mPersonListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EntityDisplayModel item = mPersonAdapter.getItem(position);
                DialogFragment profileDialog = new UserProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putString("secondaryUserId", item.getUid());
                profileDialog.setArguments(bundle);
                profileDialog.show(getFragmentManager(), "userprofile");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CHAT_ID")) {
            mChatId = intent.getStringExtra("CHAT_ID");
            mGroupId = intent.getStringExtra("GROUP_ID");
            mTitle = intent.getStringExtra("TITLE");
            populateMemberList(mChatId);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mPersonAdapter.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Up button has to be handled separately
            case android.R.id.home:
                Intent intent = new Intent(this.getApplicationContext(), ChatActivity.class)
                        .putExtra("CHAT_GROUP", mGroupId)
                        .putExtra("TITLE", mTitle);
                finish();
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void populateMemberList(String chatId) {
        mChatMembersDbRef = mRootDbRef.child("chats/info").child(chatId).child("users");
        mChatMembersDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot membersSnapshot) {
                Long memberCount = membersSnapshot.getChildrenCount();
                mToolbar.setTitle(memberCount.toString() + " Members");
                for(DataSnapshot member: membersSnapshot.getChildren()) {
                    String secondaryUserId = member.getKey();
                    addUserToList(secondaryUserId);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
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
}
