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

import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import org.json.JSONObject;

import java.util.List;

//Putting libraries which use FirebaseUser module in echoapp module.
// Because the java admin sdk do not support FirebaseUser module currently (11th Jan 2017)

public class UserProfile {

    protected String uid;
    protected String email;
    protected String displayName;
    protected String photoUrl;
    protected String instanceId;

    public UserProfile() {
    }

    public UserProfile(FirebaseUser user) {
        this.uid = user.getUid();
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
        this.photoUrl = user.getPhotoUrl().toString();

//        for (UserInfo profile : user.getProviderData()) {
//            // Id of the provider (ex: google.com)
//            if (profile.getProviderId() == "facebook.com") {
////                fetchFbData(user);
//            }
//        };

    }

    public String getUID() { return uid; }

    public void setUID(String uid) { this.uid = uid; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() { return displayName; }

    public void setName(String name) {
        this.displayName = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getInstanceId() { return instanceId; }

    public void setInstanceId(String instanceId) { this. instanceId = instanceId; }

//    private void fetchFbData(FirebaseUser user) {
//        accessToken =
//        GraphRequest request = GraphRequest.newMeRequest(
//                accessToken,
//                new GraphRequest.GraphJSONObjectCallback() {
//                    @Override
//                    public void onCompleted(JSONObject object, GraphResponse response) {
//                        // Insert your code here
//                    }
//                });
//
//        Bundle parameters = new Bundle();
//        parameters.putString("fields", "work");
//        request.setParameters(parameters);
//        request.executeAsync();
//    }
}
