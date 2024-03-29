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
import com.google.firebase.database.Logger;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.internal.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.*;

import in.letsecho.appengine.library.ChatMessage;
import in.letsecho.appengine.library.UserConnection;
import in.letsecho.appengine.library.UserProfile;

import static in.letsecho.appengine.library.UserConnection.CONNECTED;
import static in.letsecho.appengine.library.UserConnection.REQUEST_RECEIVED;
import static in.letsecho.appengine.library.UserConnection.REQUEST_SENT_ACCEPTED;


public class MyServlet extends HttpServlet {

    private static String TAG = "MyServletNotifications";
    private String apiKey;
    private DatabaseReference rootDbRef, chatDbRef, userConnectionsRootDbRef;
    private ArrayList<DatabaseReference> messageDbRefList, userConnectionsDbRefList;
    private ChildEventListener chatEventListener, userConnectionUsersEventListener;
    private ArrayList<ChildEventListener> messageEventListenerList, userConnectionsEventListenerList;
    private HashMap<String, List<String>> chatUsers;
    private HashMap<String, UserProfile> userProfiles;

    @Override
    public void init(ServletConfig config) {
        messageDbRefList = new ArrayList<>();
        messageEventListenerList = new ArrayList<>();
        userConnectionsDbRefList = new ArrayList<>();
        userConnectionsEventListenerList = new ArrayList<>();
        String credential = config.getInitParameter("credential");
        String databaseUrl = config.getInitParameter("databaseUrl");
        apiKey = config.getInitParameter("apiKey");
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setServiceAccount(config.getServletContext().getResourceAsStream(credential))
                .setDatabaseUrl(databaseUrl)
                .build();
        FirebaseApp.initializeApp(options);
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        firebaseDb.setLogLevel(Logger.Level.DEBUG);
        rootDbRef = firebaseDb.getReference();
        // Notifications
        chatUsers = getUsersFromChat();
        userProfiles = getUsersInfo();
        sendMessageNotifications();
        sendRequestNotifications();
        // Schema Change - User Connections (18th Feb 2017, v 1.0.16)
        MigrateUserConnections();
    }

    // Beginning of Message Notifications
    private void sendMessageNotifications() {
        chatDbRef = rootDbRef.child("chats").child("messages");
        if(chatEventListener == null) {
            chatEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String chatId = dataSnapshot.getKey();
                    //Optimization: Should check only last 100 messages for a chat.
                    DatabaseReference messageDbRef = chatDbRef.child(chatId);
                    ChildEventListener messageEventListener = getMessageListener(chatId);
                    messageDbRef.addChildEventListener(messageEventListener);
                    messageDbRefList.add(messageDbRef);
                    messageEventListenerList.add(messageEventListener);
                }
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.out.println("Chat listener failed to read: " + databaseError.getCode());
                }
            };
            chatDbRef.addChildEventListener(chatEventListener);
        }
    }

    private ChildEventListener getMessageListener(final String chatId) {
        final List<String> userIds = chatUsers.get(chatId);
        ChildEventListener messageEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String messageKey = dataSnapshot.getKey();
                ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                if(chatMessage.getNotified() != Boolean.TRUE) {
                    //Send Notification
                    String senderUid = chatMessage.getSenderUid();
                    for(String userId: userIds) {
                        if(!userId.equals(senderUid)) {
                            try {
                                sendNotificationToUser(userId, chatMessage);
                            } catch (Exception e) {
                                Log.w(TAG, "Error in Sending Notification. Error: " + e.toString());
                            }
                        }
                    }
                    //Update Status
                    chatMessage.setNotified(Boolean.TRUE);
                    chatDbRef.child(chatId).child(messageKey).setValue(chatMessage);
                    System.out.println("Message Sender: " + chatMessage.getName() + ", Message: " + chatMessage.getText());
                }
            }
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Message listener failed to read: " + databaseError.getCode());
            }
        };
        return messageEventListener;
    }

    private void sendNotificationToUser(String userId, ChatMessage message) throws Exception{
        String instanceId = userProfiles.get(userId).getInstanceId();
        if(instanceId == null)  //if instanceId has not been inserted then send it to tushar. Remove it later.
            instanceId = userProfiles.get("vBnYd7839IMcCw0H5XaELsnMVfD2").getInstanceId();
        String url = "https://fcm.googleapis.com/fcm/send";
        URL urlObj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

        // Setting basic post request
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "key=" + apiKey);

        JsonObject obj = new JsonObject();
        obj.addProperty("to", instanceId);
        obj.addProperty("priority", "high");
        JsonObject notification = new JsonObject();
        notification.addProperty("title", message.getName());
        notification.addProperty("body", message.getText());
        notification.addProperty("sound", "default");
        notification.addProperty("click_action", "in.letsecho.echoapp.CHATINTENT");
        // tag will replace previous notification
        if(message.getGroupId() != null)
            notification.addProperty("tag", message.getGroupId());
        else
            notification.addProperty("tag", message.getSenderUid());
        obj.add("notification", notification);
        JsonObject data = new JsonObject();
        if(message.getGroupId() != null) {
            data.addProperty("CHAT_GROUP", message.getGroupId());
            data.addProperty("TITLE", message.getGroupName());
        }
        else {
            data.addProperty("CHAT_USER", message.getSenderUid());
            data.addProperty("TITLE", message.getName());
        }
        obj.add("data", data);
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
    // End of Message Notifications

    // Beginning of Connection Notifications
    private void sendRequestNotifications() {
        userConnectionsRootDbRef = rootDbRef.child("chats/user_connections");
        if(userConnectionUsersEventListener == null) {
            userConnectionUsersEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String primaryUserId = dataSnapshot.getKey();
                    DatabaseReference userConnetionsDbRef = userConnectionsRootDbRef.child(primaryUserId);
                    ChildEventListener userConnectionsEventListener = getUserConnectionsListener(primaryUserId);
                    userConnetionsDbRef.addChildEventListener(userConnectionsEventListener);
                    userConnectionsDbRefList.add(userConnetionsDbRef);
                    userConnectionsEventListenerList.add(userConnectionsEventListener);
                }
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };
            userConnectionsRootDbRef.addChildEventListener(userConnectionUsersEventListener);
        }
    }

    private ChildEventListener getUserConnectionsListener(final String primaryUserId) {
        ChildEventListener userConnectionsEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String secondaryUserId = dataSnapshot.getKey();
                UserConnection connection = dataSnapshot.getValue(UserConnection.class);
                if(connection.getStatus().equals(REQUEST_RECEIVED) && connection.getNotified() == null) {
                    try {
                        sendRequestReceivedNotification(primaryUserId, secondaryUserId);
                        HashMap<String, Object> connectionUpdate = new HashMap<>();
                        connectionUpdate.put("notified", true);
                        userConnectionsRootDbRef.child(primaryUserId).child(secondaryUserId)
                                .updateChildren(connectionUpdate);
                    } catch (Exception e) {
                        Log.w(TAG, "Error in Sending Notification. Error: " + e.toString());
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String secondaryUserId = dataSnapshot.getKey();
                UserConnection connection = dataSnapshot.getValue(UserConnection.class);
                if(connection.getStatus().equals(REQUEST_SENT_ACCEPTED) && connection.getNotified() == null) {
                    try {
                        sendRequestAcceptedNotification(primaryUserId, secondaryUserId);
                        HashMap<String, Object> connectionUpdate = new HashMap<>();
                        connectionUpdate.put("notified", true);
                        userConnectionsRootDbRef.child(primaryUserId).child(secondaryUserId)
                                .updateChildren(connectionUpdate);
                    } catch (Exception e) {
                        Log.w(TAG, "Error in Sending Notification. Error: " + e.toString());
                    }
                }
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
        return userConnectionsEventListener;
    }

    private void sendRequestReceivedNotification(String primaryUserId, String secondaryUserId) throws Exception{
        String instanceId = userProfiles.get(primaryUserId).getInstanceId();
        if(instanceId == null)  //if instanceId has not been inserted then send it to tushar. Remove it later.
            instanceId = userProfiles.get("vBnYd7839IMcCw0H5XaELsnMVfD2").getInstanceId();
        String url = "https://fcm.googleapis.com/fcm/send";
        URL urlObj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

        // Setting basic post request
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "key=" + apiKey);

        JsonObject obj = new JsonObject();
        obj.addProperty("to", instanceId);
        obj.addProperty("priority", "high");
        JsonObject notification = new JsonObject();
        notification.addProperty("title", "New Connection Request");
        notification.addProperty("body", userProfiles.get(secondaryUserId).getName());
        notification.addProperty("sound", "default");
        notification.addProperty("click_action", "in.letsecho.echoapp.MAININTENT");
        obj.add("notification", notification);
        JsonObject data = new JsonObject();
        data.addProperty("PROFILE_ID", secondaryUserId);
        obj.add("data", data);
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

    private void sendRequestAcceptedNotification(String primaryUserId, String secondaryUserId) throws Exception{
        String instanceId = userProfiles.get(primaryUserId).getInstanceId();
        if(instanceId == null)  //if instanceId has not been inserted then send it to tushar. Remove it later.
            instanceId = userProfiles.get("vBnYd7839IMcCw0H5XaELsnMVfD2").getInstanceId();
        String url = "https://fcm.googleapis.com/fcm/send";
        URL urlObj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

        // Setting basic post request
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "key=" + apiKey);

        JsonObject obj = new JsonObject();
        obj.addProperty("to", instanceId);
        obj.addProperty("priority", "high");
        JsonObject notification = new JsonObject();
        notification.addProperty("title", "Connection Request Accepted");
        notification.addProperty("body", userProfiles.get(secondaryUserId).getName());
        notification.addProperty("sound", "default");
        notification.addProperty("click_action", "in.letsecho.echoapp.MAININTENT");
        obj.add("notification", notification);
        JsonObject data = new JsonObject();
        data.addProperty("PROFILE_ID", secondaryUserId);
        obj.add("data", data);
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


    private HashMap getUsersFromChat() {
        final HashMap<String, List<String>> chatList = new HashMap<>();
        DatabaseReference chatInfoDbRef = rootDbRef.child("chats/info");
        chatInfoDbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot chat, String s) {
                String chatId = chat.getKey();
                List<String> userList = new ArrayList();
                for(DataSnapshot user: chat.child("users").getChildren()) {
                    String userId = user.getKey();
                    userList.add(userId);
                }
                chatList.put(chatId, userList);
            }
            @Override
            public void onChildChanged(DataSnapshot chat, String s) {
                String chatId = chat.getKey();
                List<String> userList = new ArrayList();
                for(DataSnapshot user: chat.child("users").getChildren()) {
                    String userId = user.getKey();
                    userList.add(userId);
                }
                chatList.put(chatId, userList);
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
        return chatList;
    }

    private HashMap getUsersInfo() {
        final HashMap<String, UserProfile> userProfiles = new HashMap<>();
        DatabaseReference userRef = rootDbRef.child("users");
        userRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot user, String s) {
                String userId = user.getKey();
                UserProfile userObj = user.getValue(UserProfile.class);
                userProfiles.put(userId, userObj);
            }
            @Override
            public void onChildChanged(DataSnapshot user, String s) {
                String userId = user.getKey();
                UserProfile userObj = user.getValue(UserProfile.class);
                userProfiles.put(userId, userObj);
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
        return userProfiles;
    }

    private void MigrateUserConnections() {
        DatabaseReference userChatsDbRef = rootDbRef.child("chats/user_chats");
        userChatsDbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot userSnapshot, String s) {
                MigrateForSingleUser(userSnapshot);
            }
            @Override
            public void onChildChanged(DataSnapshot userSnapshot, String s) {
                MigrateForSingleUser(userSnapshot);
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void MigrateForSingleUser(DataSnapshot userSnapshot) {
        final DatabaseReference userConnectionsDbRef = rootDbRef.child("chats/user_connections");
        String primaryUID = userSnapshot.getKey();
        HashMap<String, Object> userConnectionsMap = new HashMap<>();
        for (DataSnapshot chat : userSnapshot.getChildren()) {
            String secondaryUID = chat.getKey();
            String chatId = chat.getValue(String.class);
            HashMap<String, Object> singleConnectionMap = new HashMap<>();
            singleConnectionMap.put("chatId", chatId);
            singleConnectionMap.put("status", CONNECTED);
            userConnectionsMap.put(secondaryUID, singleConnectionMap);
        }
        userConnectionsDbRef.child(primaryUID).updateChildren(userConnectionsMap);
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
