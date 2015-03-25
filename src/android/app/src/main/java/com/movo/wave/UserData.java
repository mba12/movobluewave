package com.movo.wave;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

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

            Log.d(TAG, "User info is: "+currentUID);
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


    private boolean reAuthenticate(String email, String pw) {


        Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/");
        ref.authWithPassword(email, pw, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                //success, save auth data
//            UserData myData = UserData.getUserData();
                currentUID = authData.getUid();
                currentToken = authData.getToken();


                System.out.println("User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());

                status = true;
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                System.out.println("Error logging in " + firebaseError.getDetails());
                status = false;
            }
        });
        return status;
    }
}