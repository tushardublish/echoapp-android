/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package in.letsecho.appengine;

import com.google.appengine.repackaged.com.google.gson.JsonObject;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
        sendMessageNotifications();
//        messageNotifier = new MessageNotifier(rootDbRef);
//        messageNotifier.start();
    }

    private void sendMessageNotifications() {
        final String chatId = "-KZfU1gdjh5qmSn2WbWV";
        currentChatDbRef = rootDbRef.child("chats").child("messages").child(chatId);
        final List<String> userIds = getUsersFromChatId(chatId);
        if (messageEventListener == null) {
            messageEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                    String messageKey = dataSnapshot.getKey();
                    if(chatMessage.getNotified() != Boolean.TRUE) {
                        //Send Notification
                        String senderUid = chatMessage.getSenderUid();
                        for(String userId: userIds) {
                            if(userId != senderUid) {
                                try {
                                    sendNotificationToUser(userId, chatMessage);
                                } catch (Exception e) {

                                }
                            }
                        }

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

    private void sendNotificationToUser(String userId, ChatMessage message) throws Exception{
        String apiKey = "AAAAJudZXIQ:APA91bG2sneJVDhM-Mh17bVYkBTtolDxb0UtqZjLujcqQskDmSOSRDUR1a3b0ceVw08bMBXozZn9_PqFuJokvRFGkGbj_af1aC-ZcEqOkkL7FKcvBnYE3PF7cgd_Llvw-__TxLM32r46x9BaoNq8eADx-G-b8rvc3A";
//        List<String> instanceId = getInstanceIdForUser(userId);
        String instanceId = "e4p1gkzTJGg:APA91bFoaaewjkc4S9nXRzIn6Hbi5flINmNQuakIzMrW1WVhWyAiH5sA0S1ynW5uaXZ06k6K6oWglAaVITfyoabNZN8msc7yYdLfho6xmVzQjZ08Mf5bHgmhBEcqyr_ontRE1KmPinEI";
        String url = "https://fcm.googleapis.com/fcm/send";

        URL urlObj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

        // Setting basic post request
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "key=" + apiKey);
//        con.setRequestProperty("User-Agent", "Mozilla/5.0");
//        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        JsonObject obj = new JsonObject();
        obj.addProperty("to", instanceId);
        obj.addProperty("priority", "high");
        JsonObject notification = new JsonObject();
        notification.addProperty("title", "New message from " + message.getName());
        notification.addProperty("body", message.getText());
        obj.add("notification",notification);
        String postJsonData = obj.toString();

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(postJsonData);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post Data : " + postJsonData);
        System.out.println("Response Code : " + responseCode);

    }

    private List<String> getUsersFromChatId(String chatId) {
        final List<String> userList = new ArrayList();
        DatabaseReference chatInfoDbRef = rootDbRef.child("chats/info").child(chatId).child("users");
        chatInfoDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot user: dataSnapshot.getChildren()) {
                    userList.add(user.getKey());
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
        return userList;
    }

    private List<String> getInstanceIdForUser(String userId) {
        final List<String> instanceId = new ArrayList<>(); //Doing this because inner class is not able to modify outside variable
        DatabaseReference userInstanceRef = rootDbRef.child("users").child(userId).child("instanceId");
        userInstanceRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                instanceId.add(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));
        return instanceId;
    }

    //One time script to extract info from user_chats section and populate info section in chats
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