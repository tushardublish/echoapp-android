/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package in.letsecho.appengine;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.*;

import in.letsecho.library.ChatMessage;

public class MyServlet extends HttpServlet {

    private DatabaseReference rootDbRef;
    private DatabaseReference currentChatDbRef;
    private ChildEventListener messageEventListener;

    @Override
    public void init(ServletConfig config) {
        String credential = config.getInitParameter("credential");
        String databaseUrl = config.getInitParameter("databaseUrl");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setServiceAccount(config.getServletContext().getResourceAsStream(credential))
                .setDatabaseUrl(databaseUrl)
                .build();
        FirebaseApp.initializeApp(options);
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        rootDbRef = firebaseDb.getReference();
//        SendMessageNotifications();
//        messageNotifier = new MessageNotifier(rootDbRef);
//        messageNotifier.start();
    }

    private void SendMessageNotifications() {
        currentChatDbRef = rootDbRef.child("chats").child("messages").child("-KZfWIgv3hb__PcaWNtQ");
        if (messageEventListener == null) {
            messageEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                    String messageKey = dataSnapshot.getKey();
                    if(chatMessage.getNotified() != Boolean.TRUE) {
                        //Send Notification


                        //Update Status
                        chatMessage.setNotified(Boolean.TRUE);
                        currentChatDbRef.child(messageKey).setValue(chatMessage);
                    }
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            currentChatDbRef.addChildEventListener(messageEventListener);
        }
    }

    //One time script to extracr info from user_chats section and populate info section in chats
    private void UpdateChatInfo() {
        rootDbRef.child("chats/user_chats").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot user1: dataSnapshot.getChildren()) {
                    String uid1 = user1.getKey();
                    for(DataSnapshot user2: user1.getChildren()) {
                        String uid2 = user2.getKey();
                        String chatId = user2.getValue(String.class);
                        DatabaseReference chatInfoDbRef = rootDbRef.child("chats/info").child(chatId).child("users");
                        Map<String, Object> users = new HashMap<String, Object>();
                        users.put(uid1, Boolean.TRUE);
                        users.put(uid2, Boolean.TRUE);
                        chatInfoDbRef.setValue(users);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("Please use the form to POST to this url");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String name = req.getParameter("name");
        resp.setContentType("text/plain");
        if (name == null) {
            resp.getWriter().println("Please enter a name");
        }
        resp.getWriter().println("Hello " + name);
    }
}
