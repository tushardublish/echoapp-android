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
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public void saveFbData(final DatabaseReference usersDbRef) {
        if(AccessToken.getCurrentAccessToken() != null) {
            Log.d("USERPROFILE", AccessToken.getCurrentAccessToken().getToken());
            GraphRequest request = GraphRequest.newMeRequest(
                    AccessToken.getCurrentAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            // Application code
                            try {
                                Map newMap = new HashMap();
                                Map map = JsonToMap.convert(object);
                                newMap.put("id", map.get("id"));
                                //Work
                                List<FbWork> newWorkList = new ArrayList<>();
                                if(map.containsKey("work")) {
                                    for (Map workObj : (ArrayList<Map>) map.get("work")) {
                                        FbWork work = new FbWork(workObj);
                                        newWorkList.add(work);
                                    }
                                    newMap.put("work", newWorkList);
                                }
                                //Education
                                List<FbEducation> newEduList = new ArrayList<>();
                                if(map.containsKey("education")) {
                                    for (Map eduObj : (ArrayList<Map>) map.get("education")) {
                                        FbEducation edu = new FbEducation(eduObj);
                                        newEduList.add(edu);
                                    }
                                    newMap.put("education", newEduList);
                                }
                                usersDbRef.child(uid).child("fbdata").updateChildren(newMap);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "id, work, education");
            request.setParameters(parameters);
            request.executeAsync();
        }
        else {
            Log.d("USERPROFILE", "Access Token NULL");
        }
    }
}
