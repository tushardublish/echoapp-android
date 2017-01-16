/*
    Dated: 16th Jan 2017
    Copied from library, as I was not able to figure out how to import gradle project in maven project.
    It was required when we use maven commands to build and deploy backend project.
    Either GCP might soon migrate AppEngine Flexible to gradle soon or we might have to figure out a way later.
*/
package in.letsecho.appengine.library;

import java.util.HashMap;

public class ChatMessage {

    private String text;
    private String name;
    private String senderUid;
    private String photoUrl;
    private Boolean notified;
    private HashMap<String,Boolean> seen;

    public ChatMessage() {
        this.seen = new HashMap();
    }

    public ChatMessage(String text, String name, String senderUid, String photoUrl, HashMap<String, Boolean> seen) {
        this.text = text;
        this.name = name;
        this.senderUid = senderUid;
        this.photoUrl = photoUrl;
        this.seen = seen;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSenderUid() {return senderUid; }

    public void setSenderUid(String senderUid) {this.senderUid = senderUid; }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Boolean getNotified() { return notified; }

    public void setNotified(Boolean notified) { this.notified = notified; }

    public HashMap<String,Boolean> getSeen() { return seen; }

    public void setSeen(HashMap<String, Boolean> seen) { this.seen = seen; }

    public boolean getSeenForUser(String userId) {
        if(seen.containsKey(userId)) {
            return seen.get(userId);
        } else {
            return Boolean.FALSE;
        }
    }

    public void setSeenForUser(String userId) {
        seen.put(userId, Boolean.TRUE);
    }
}
