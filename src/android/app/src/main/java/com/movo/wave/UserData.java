package com.movo.wave;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by PhilG on 3/24/2015.
 */


public class UserData {
    private static UserData instance;
    private String TAG = "Wave.UserData";
    boolean status = false;
    Context appContext;
    private String currentUID;
    private String currentToken;
    private String currentEmail;
    private String currentPW;
    private DataSnapshot currentUserSnapshot;


    public static UserData getUserData(Context c) {
        if (instance == null) {
            instance = new UserData(c);
        }
        return instance;
    }


    private UserData(Context c) {
        appContext = c;
        Firebase.setAndroidContext(appContext);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        boolean userExists = prefs.getBoolean("userExists", false);
        //TODO: Make this compatible with multiple users
        if (userExists) {
            currentUID = prefs.getString("currentUID", "Error");
            currentToken = prefs.getString("currentToken", "Error");
            currentEmail = prefs.getString("currentEmail", "Error");
            currentPW = prefs.getString("currentPW", "Error");
//            currentUserSnapshot = prefs.gets
            reAuthenticate(currentEmail, currentPW);
            prefs.edit().putBoolean("userExists",reAuthenticate(currentEmail, currentPW)).commit();
            Log.d(TAG, "User info is: " + currentUID);
        } else {
            currentUID = "Error";
            currentToken = "Error";
            currentEmail = "Error";
            currentPW = "Error";
            Log.d(TAG, "User info doesn't exist");
        }


    }

    public String getCurUID() {
        return currentUID;
    }

    public String getCurToken() {
        return currentToken;
    }

    public String setCurUID(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putBoolean("userExists", true).commit();
        prefs.edit().putString("currentUID", input).commit();
        currentUID = input;
        return currentUID;
    }

    public String setCurToken(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentToken", input).commit();
        currentToken = input;
        return currentToken;
    }

    public String setCurEmail(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentEmail", input).commit();
        currentEmail = input;
        return currentEmail;
    }

    public String setCurPW(String input) {
        //TODO: We will want to encrypt this, as android shared prefs are only secure if the device isn't rooted.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentPW", input).commit();
        currentPW = input;
        return currentPW;
    }

    public DataSnapshot setUserSnapshot(DataSnapshot input){
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
//        prefs.edit().putString("currentPW", input).commit();
        currentUserSnapshot = input;
        return currentUserSnapshot;
    }
    public DataSnapshot getUserSnapshot(){
        return currentUserSnapshot;
    }


    private boolean reAuthenticate(final String email, final String pw) {
        Log.d(TAG, "Re-authenticating with user "+email+" and pass "+pw);

        Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/");
        ref.authWithPassword(email, pw, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                //success, save auth data
//            UserData myData = UserData.getUserData();
                setCurUID(authData.getUid());
                setCurEmail(email);
                setCurPW(pw);
                setCurToken(authData.getToken());


                Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());

                status = true;
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.d(TAG, "Error logging in " + firebaseError.getDetails());
                status = false;
            }
        });
        return status;
    }

    public boolean storeCurrentUser(){
        String storeUID = currentUID;
        if(!(currentUID.equals("Error"))) {
            Map<String, String> userDataString = new HashMap<String, String>();
            userDataString.put("currentUID", currentUID);
            userDataString.put("currentToken", currentToken);
            userDataString.put("currentEmail", currentEmail);
            userDataString.put("currentPW", currentPW);


            SharedPreferences userData = appContext.getSharedPreferences(currentUID, Context.MODE_PRIVATE);
            SharedPreferences.Editor userDataEditor = userData.edit();

            for (String s : userDataString.keySet()) {
                userDataEditor.putString(s, userDataString.get(s));
            }
            userDataEditor.commit();

            SharedPreferences allUsers = appContext.getSharedPreferences("allUsers", Context.MODE_PRIVATE);
            SharedPreferences.Editor allUsersEditor = allUsers.edit();

            if (!(allUsers.contains(currentUID))) {
                allUsersEditor.putString(currentUID, currentEmail);
            }
            allUsersEditor.commit();


//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
//        prefs.edit().putString(currentUID, userDataString).commit();

//        Set<String> allUsers = prefs.getStringSet("allUsers", new HashSet<String>());
//        if(!allUsers.contains(currentUID)){
//            Log.d(TAG, "Adding "+currentUID+" to users list:"+allUsers);
//            allUsers.add(currentUID);
//            prefs.edit().putStringSet("allUsers",allUsers).commit();
//        }


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putBoolean("userExists", false);
            instance = null;


            //loadNewUser(storeUID);
            return true;
        }else {
            return false;
        }
    }

    public boolean addCurUserTolist(){
        Map<String, String> userDataString = new HashMap<String, String>();
        userDataString.put("currentUID", currentUID);
        userDataString.put("currentToken", currentToken);
        userDataString.put("currentEmail", currentEmail);
        userDataString.put("currentPW", currentPW);


        SharedPreferences userData = appContext.getSharedPreferences(currentUID, Context.MODE_PRIVATE);
        SharedPreferences.Editor userDataEditor = userData.edit();

        for (String s : userDataString.keySet()) {
            userDataEditor.putString(s, userDataString.get(s));
        }
        userDataEditor.commit();

        SharedPreferences allUsers = appContext.getSharedPreferences("allUsers", Context.MODE_PRIVATE);
        SharedPreferences.Editor allUsersEditor = allUsers.edit();

        if (!(allUsers.contains(currentUID))) {
            allUsersEditor.putString(currentUID, currentEmail);
        }
        allUsersEditor.commit();

        return true;
    }


    public boolean loadNewUser(String UID){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);

        SharedPreferences allUsers = appContext.getSharedPreferences("allUsers", Context.MODE_PRIVATE);

        if(allUsers.contains(UID)) {
            SharedPreferences userData = appContext.getSharedPreferences(UID, Context.MODE_PRIVATE);
//            SharedPreferences.Editor userDataEditor = userData.edit();
            currentUID = userData.getString("currentUID","Error");
            currentEmail = userData.getString("currentEmail", "Error");
            currentPW = userData.getString("currentPW", "Error");
            currentToken = userData.getString("currentToken","Error");

            reAuthenticate(currentEmail, currentPW);
        }


        return true;

    }

    public ArrayList<String> getUserList(){
        //this returns a list of user emails on device
        SharedPreferences allUsers = appContext.getSharedPreferences("allUsers", Context.MODE_PRIVATE);
        ArrayList<String> allUsersReturn = new ArrayList<>();
        Map<String,?> keys = allUsers.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Log.d("map values",entry.getKey() + ": " +
                    entry.getValue().toString());
            allUsersReturn.add(entry.getValue().toString());
        }
        return allUsersReturn;
    }

    public String getUIDByEmail(String email){
        String pw = "";
        SharedPreferences allUsers = appContext.getSharedPreferences("allUsers", Context.MODE_PRIVATE);
        ArrayList<String> allUsersReturn = new ArrayList<>();
        Map<String,?> keys = allUsers.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Log.d("map values",entry.getKey() + ": " +
                    entry.getValue().toString());
            if(entry.getValue().toString().equals(email)){
                pw = entry.getKey();
            }
            allUsersReturn.add(entry.getValue().toString());
        }
        return pw;
    }

}