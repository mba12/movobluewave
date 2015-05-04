package com.movo.wave;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by PhilG on 3/24/2015.
 */


public class UserData extends Activity{
    private static UserData instance;
    private String TAG = "Wave.UserData";
    boolean status = false;
    Context appContext;
    private String currentUID = "Error";
    private String currentToken = "Error";
    private String currentEmail = "Error";
    private String currentPW = "Error";
    private String currentBirthdate = "Error";
    private String currentHeight1 = "Error";
    private String currentHeight2 = "Error";
    private String currentWeight = "Error";
    private String currentGender = "Error";
    private String currentFullName = "Error";
    private String currentUsername = "Error";
    private DataSnapshot currentUserSnapshot;
    private Firebase loginRef;
    private Firebase currentUserRef;



    public static UserData getUserData(Context c) {
        if (instance == null) {
            instance = new UserData(c);
        }
        return instance;
    }


    private UserData(Context c) {
        appContext = c;
        Firebase.setAndroidContext(appContext);
        loginRef = new Firebase("https://ss-movo-wave-v2.firebaseio.com/");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        boolean userExists = prefs.getBoolean("userExists", false);
        //TODO: Make this compatible with multiple users
        if (userExists) {
            currentUID = prefs.getString("currentUID", "Error");
            currentToken = prefs.getString("currentToken", "Error");
            currentEmail = prefs.getString("currentEmail", "Error");
            currentHeight1 = prefs.getString("currentHeight1", "Error");
            currentHeight2 = prefs.getString("currentHeight2", "Error");
            currentWeight = prefs.getString("currentWeight", "Error");
            currentGender = prefs.getString("currentGender", "Error");
            currentFullName = prefs.getString("currentFullName", "Error");
            currentPW = prefs.getString("currentPW", "Error");
            currentBirthdate= prefs.getString("currentBirthdate", "Error");
            currentUsername= prefs.getString("currentUsername", "Error");
//            currentUserSnapshot = prefs.gets
//            reAuthenticate(currentEmail, currentPW);
            prefs.edit().putBoolean("userExists",reAuthenticate(currentEmail, currentPW)).commit();
            Log.d(TAG, "User info is: " + currentUID);
        } else {
            //this case is if there is a user in the list, but nobody is logged in
            ArrayList<String> users = new ArrayList<String>();
            users = getUserList();
            if(!users.isEmpty()) {
                String uid = getUIDByEmail(users.get(0));
                loadNewUser(uid);


            }else{
                prefs.edit().putBoolean("userExists",false).commit();
            }


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

    public String setCurBirthdate(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentBirthdate", input + "").commit();
        currentBirthdate = input+"";
        return currentToken;
    }
    public String setCurEmail(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentEmail", input).commit();
        currentEmail = input;
        return currentEmail;
    }

    public String setCurName(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentFullName", input).commit();
        currentFullName = input;
        return currentFullName;

    }

    public String setCurHeight1(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentHeight1", input).commit();
        currentHeight1 = input;
        return currentHeight1;
    }

    public String setCurHeight2(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentHeight2", input).commit();
        currentHeight2 = input;
        return currentHeight2;
    }

    public String setCurWeight(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentWeight", input).commit();
        currentWeight = input;
        return currentWeight;
    }

    public String setCurGender(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentGender", input).commit();
        currentGender = input;
        return currentGender;
    }
    public String setCurUsername(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentUsername", input).commit();
        currentUsername = input;
        return currentUsername;
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

                currentUserRef = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/"+authData.getUid());
                //TODO: pull user info like name from server
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
                prefs.edit().putBoolean("userExists", true);
                Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
                updateHomePage();
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
            userDataString.put("currentHeight1", currentHeight1);
            userDataString.put("currentHeight2", currentHeight2);
            userDataString.put("currentWeight", currentWeight);
            userDataString.put("currentGender", currentGender);
            userDataString.put("currentFullName", currentFullName);
            userDataString.put("currentBirthdate", currentBirthdate);
            userDataString.put("currentUsername", currentUsername);


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




    public boolean logoutCurrentUser(){
        String storeUID = currentUID;


        SharedPreferences allUsers = appContext.getSharedPreferences("allUsers", Context.MODE_PRIVATE);
        ArrayList<String> allUsersReturn = new ArrayList<>();


        SharedPreferences.Editor allUsersEditor = allUsers.edit();
        allUsersEditor.remove(currentUID);

        allUsersEditor.commit();

        allUsers = appContext.getSharedPreferences("allUsers", Context.MODE_PRIVATE);


        ArrayList<String> users = new ArrayList<String>();
        users = getUserList();
        if(!users.isEmpty()) {

            String uid = getUIDByEmail(users.get(0));
            loadNewUser(uid);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putBoolean("userExists", true);
            return true;
        }else{
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putBoolean("userExists", false);
            instance = null;
            return false;

        }
    }



    public boolean addCurUserTolist(){
        boolean userAlreadyExists = false;
        Map<String, String> userDataString = new HashMap<String, String>();
        userDataString.put("currentUID", currentUID);
        userDataString.put("currentToken", currentToken);
        userDataString.put("currentEmail", currentEmail);
        userDataString.put("currentPW", currentPW);
        userDataString.put("currentBirthdate", currentBirthdate);
        userDataString.put("currentBirthdate", currentHeight1);
        userDataString.put("currentBirthdate", currentHeight2);
        userDataString.put("currentBirthdate", currentWeight);
        userDataString.put("currentBirthdate", currentGender);
        userDataString.put("currentBirthdate", currentFullName);
        userDataString.put("currentUsername", currentUsername);


//            currentHeight1 = prefs.getString("currentHeight1", "Error");
//            currentHeight2 = prefs.getString("currentHeight2", "Error");
//            currentWeight = prefs.getString("currentWeight", "Error");
//            currentGender = prefs.getString("currentGender", "Error");
//            currentFullName = prefs.getString("currentFullName", "Error");

//        uploadToFirebase();

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
            userAlreadyExists = false;
        }else{
            userAlreadyExists = true;
        }
        allUsersEditor.commit();

        return userAlreadyExists;
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
            currentHeight1 = prefs.getString("currentHeight1", "Error");
            currentHeight2 = prefs.getString("currentHeight2", "Error");
            currentWeight = prefs.getString("currentWeight", "Error");
            currentGender = prefs.getString("currentGender", "Error");
            currentFullName = prefs.getString("currentFullName", "Error");
            currentBirthdate= prefs.getString("currentBirthdate", "Error");
            currentUsername = prefs.getString("currentUsername", "Error");



            prefs.edit().putBoolean("userExists",reAuthenticate(currentEmail, currentPW)).commit();

            downloadMetadata(UID);

        }


        return true;

    }

    public String getCurrentUserEmail(){
       return currentEmail;
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

    public Firebase getLoginRef(){
        return loginRef;
    }

    public Firebase getCurrentUserRef(){
        return currentUserRef;
    }

    public void setCurrentUserRef(Firebase ref){
        currentUserRef = ref;
    }

    public void updateHomePage() {
        new Thread() {
            public void run() {


                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Home.setUpChartsExternalCall(appContext);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();;
                }

            }
        }.start();
    }



    public String getCurrentHeight1() { return currentHeight1; }
    public String getCurrentHeight2() { return currentHeight2; }
    public String getCurrentWeight() { return currentWeight; }
    public String getCurrentGender() { return currentGender; }
    public String getCurrentFullName() { return currentFullName; }
    public String getCurrentBirthdate() { return currentBirthdate; }
    public String getCurrentUsername() { return currentUsername; }

    public void uploadToFirebase(){
        Map<String, String> userDataString = new HashMap<String, String>();
//        userDataString.put("currentUID", currentUID);
//        userDataString.put("currentToken", currentToken);
        userDataString.put("currentEmail", currentEmail);
//        userDataString.put("currentPW", currentPW);
        userDataString.put("currentHeight1", currentHeight1);
        userDataString.put("currentHeight2", currentHeight2);
        userDataString.put("currentWeight", currentWeight);
        userDataString.put("currentGender", currentGender);
        userDataString.put("currentFullName", currentFullName);
        userDataString.put("currentBirthdate", currentBirthdate);
        userDataString.put("currentUsername", currentUsername);


        Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +getCurUID() + "/metadata/");
        ref.setValue(userDataString);


    }

    public Bitmap getCurUserPhoto(){
//        UserData myData = UserData.getUserData(c);
        DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String user = getCurUID();
        Calendar profile = Calendar.getInstance();
        profile.setTimeInMillis(0); // user photos will be stored at the dawn of time.
        String photo =  Database.PhotoStore.DATE + " =? AND "+Database.PhotoStore.USER + " =?";
        Cursor curPhoto = db.query(
                Database.PhotoStore.PHOTO_TABLE_NAME,  // The table to query
                new String[] {
                        Database.StepEntry.USER, //string
                        Database.PhotoStore.DATE, //int
                        Database.PhotoStore.PHOTOBLOB }, //blob                          // The columns to return
                photo,                                // The columns for the WHERE clause
                new String[] { profile.getTimeInMillis()+"", user },                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        curPhoto.moveToFirst();
        boolean localFile = false;
        if(curPhoto.getCount()!=0){
            localFile = true;
            byte[] byteArray = curPhoto.getBlob(2);
//                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
//            uniquePic = byteArray.length;
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = 4;
            Bitmap bm = BitmapFactory.decodeByteArray(byteArray, 0 ,byteArray.length, options);
            curPhoto.close();
             return bm;

        }else{
            curPhoto.close(); 
            return null;
        }
    }

    public void downloadProfilePic(){
        Log.d(TAG, "Loading image from firebase");
        Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + currentUID + "/photos/profilepic");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //                    System.out.println(snapshot.getValue());
                if(snapshot.getChildrenCount()==1){
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = 4;
                    ArrayList<String> result =((ArrayList<String>)snapshot.getValue());
                    byte[] decodedString = Base64.decode(result.get(0), Base64.DEFAULT);

                    DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();
//

                            //file from cloud is different than local, save to device
                            Calendar profile = Calendar.getInstance();
                            profile.setTimeInMillis(0);
                            ContentValues syncValues = new ContentValues();
                            syncValues.put(Database.PhotoStore.DATE, profile.getTimeInMillis());
                            syncValues.put(Database.PhotoStore.USER, currentUID);
                            syncValues.put(Database.PhotoStore.PHOTOBLOB, decodedString);
                            long newRowId;
                            newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                                    null,
                                    syncValues);
                            Log.d(TAG, "Photo database add from firebase: "+newRowId);
                            db.close();
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length,options);
//                            background.setImageBitmap(decodedByte);

                        }


            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    public void setMetadata(Firebase child, String useruid){

        setCurUID(useruid);
        child.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                setCurEmail(snapshot.child("currentEmail").getValue(String.class));
                setCurHeight1(snapshot.child("currentHeight1").getValue(String.class));
                setCurHeight2(snapshot.child("currentHeight2").getValue(String.class));
                setCurWeight(snapshot.child("currentWeight").getValue(String.class));
                setCurGender(snapshot.child("currentGender").getValue(String.class));
                setCurName(snapshot.child("currentFullName").getValue(String.class));
                setCurBirthdate(snapshot.child("currentBirthdate").getValue(String.class));
                setCurUsername(snapshot.child("currentUsername").getValue(String.class));
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });



//
//        setCurEmail(child.child("currentEmail"));
////        currentPW =  child.child("currentEmail").toString();
////        currentToken = userData.getString("currentToken","Error");
//        setCurHeight1(child.child("currentHeight1").toString());
//        setCurHeight2(child.child("currentHeight2").toString());
//        setCurWeight(child.child("currentWeight").toString());
//        setCurGender(child.child("currentGender").toString());
//        setCurName(child.child("currentFullName").toString());
////        currentPW = prefs.getString("currentPW", "Error");
//        setCurBirthdate(child.child("currentBirthdate").toString());
    }
    public void downloadMetadata(String useruid){
        setCurUID(useruid);
                Firebase child = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/"+currentUID+"/metadata");
                child.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Log.d(TAG,""+snapshot.getValue());

                        setCurEmail(snapshot.child("currentEmail").getValue(String.class));
                        setCurHeight1(snapshot.child("currentHeight1").getValue(String.class));
                        setCurHeight2(snapshot.child("currentHeight2").getValue(String.class));
                        setCurWeight(snapshot.child("currentWeight").getValue(String.class));
                        setCurGender(snapshot.child("currentGender").getValue(String.class));
                        setCurName(snapshot.child("currentFullName").getValue(String.class));
                        setCurBirthdate(snapshot.child("currentBirthdate").getValue(String.class));
                        setCurUsername(snapshot.child("currentUsername").getValue(String.class));

                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {

                    }
                });
            }








}