package in.letsecho.appengine;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by Tushar on 29-12-2016.
 */

public class MessageNotifier extends Thread {

    private DatabaseReference rootDbRef, currentChatDbRef;
    private ChildEventListener messageEventListener;
    private ChildEventListener currentUserChatsEventListener;

    public MessageNotifier(DatabaseReference rootRef) {
        this.rootDbRef = rootRef;
    }

    public void run() {
        while(true) {
            currentChatDbRef = rootDbRef.child("chats").child("messages").child("-KZfWIgv3hb__PcaWNtQ");
            if (messageEventListener == null) {
                messageEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        String chat = "found";
                    }

                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                    public void onChildRemoved(DataSnapshot dataSnapshot) {}
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                    public void onCancelled(DatabaseError databaseError) {}
                };
                currentChatDbRef.addChildEventListener(messageEventListener);
                rootDbRef.child("test").push().setValue("abcde");
            }
        }
    }
}
