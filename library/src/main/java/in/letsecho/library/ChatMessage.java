/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package in.letsecho.library;

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
