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

import java.util.List;

public class UserDisplayModel extends UserProfile {

    private String rightAlignedInfo;

    public UserDisplayModel() {
        super();
    }

    public UserDisplayModel(String uid, String email, String displayName, String photoUrl) {
        super();
    }

    public String getRightAlignedInfo() { return rightAlignedInfo; }

    public void setRightAlignedInfo(String rightAlignedInfo) { this.rightAlignedInfo = rightAlignedInfo; }

    public static int findProfileOnUid(List<UserDisplayModel> profiles, String id) {
        for(int i = 0; i < profiles.size(); i++) {
            UserProfile obj = profiles.get(i);
            if(obj.getUID().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
