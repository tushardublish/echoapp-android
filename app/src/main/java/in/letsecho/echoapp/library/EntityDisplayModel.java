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
package in.letsecho.echoapp.library;

import com.twitter.sdk.android.core.models.User;

import java.util.List;

public class EntityDisplayModel {

    public static int USER_TYPE = 1;
    public static int GROUP_TYPE = 1;
    private String uid;
    private String title;
    private int type;
    private String photoUrl;
    private String rightAlignedInfo;

    public EntityDisplayModel() {}

    public EntityDisplayModel(String uid, String title, int type, String photoUrl) {
        this.uid = uid;
        this.title = title;
        this.type = type;
        this.photoUrl = photoUrl;
    }

    public EntityDisplayModel(UserProfile user) {
        this.uid = user.getUID();
        this.title = user.getName();
        this.photoUrl = user.getPhotoUrl();
        this.type = USER_TYPE;
    }

    public EntityDisplayModel(Group group) {
        this.uid = group.getId();
    }

    public String getRightAlignedInfo() { return rightAlignedInfo; }

    public void setRightAlignedInfo(String rightAlignedInfo) { this.rightAlignedInfo = rightAlignedInfo; }

    public static int findProfileOnUid(List<EntityDisplayModel> profiles, String id) {
        for(int i = 0; i < profiles.size(); i++) {
            EntityDisplayModel obj = profiles.get(i);
            if(obj.getUid().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
