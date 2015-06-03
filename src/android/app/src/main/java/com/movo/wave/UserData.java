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
import android.media.ExifInterface;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.movo.wave.util.DataUtilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;


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
    final static String firebase_url = "https://ss-movo-wave-v2.firebaseio.com/";



    public static UserData getUserData(Context c) {
        if (instance == null) {
            instance = new UserData(c);
        }
        return instance;
    }


    private UserData(Context c) {
        appContext = c;
        Firebase.setAndroidContext(appContext);
        loginRef = new Firebase(UserData.firebase_url);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        boolean userExists = prefs.getBoolean("userExists", false);

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

        Firebase ref = new Firebase(UserData.firebase_url);
        ref.authWithPassword(email, pw, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                //success, save auth data
//            UserData myData = UserData.getUserData();
                setCurUID(authData.getUid());
                setCurEmail(email);
                setCurPW(pw);
                setCurToken(authData.getToken());
                downloadMetadata(authData.getUid());

                currentUserRef = new Firebase(UserData.firebase_url + "users/"+authData.getUid());
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
                prefs.edit().putBoolean("userExists", true);
                Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
                updateHomePage();
                status = true;
                downloadMetadata(authData.getUid());
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.d(TAG, "Error logging in " + firebaseError.getDetails());
                status = false;
//                prefs.edit().putBoolean("userExists",false)
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



            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putBoolean("userExists", false);
            instance = null;


            //loadNewUser(storeUID);
            return true;
        }else {
            return false;
        }
    }

    public boolean resetCurrentUserValues(){
        currentUID = "Error";
        currentToken = "Error";
        currentEmail = "Error";
        currentPW = "Error";
        currentHeight1 = "Error";
        currentHeight2 = "Error";
        currentWeight = "Error";
        currentGender = "Error";
        currentFullName = "Error";
        currentBirthdate = "Error";
        currentUsername = "Error";
        return true;
    }




    public boolean logoutCurrentUser(){
//        String storeUID = currentUID;


        SharedPreferences allUsers = appContext.getSharedPreferences("allUsers", Context.MODE_PRIVATE);
//        ArrayList<String> allUsersReturn = new ArrayList<>();


        SharedPreferences.Editor allUsersEditor = allUsers.edit();
        allUsersEditor.remove(currentUID);

        allUsersEditor.commit();

//        ArrayList<String> allUsersReturn = new ArrayList<>();
//        Map<String,?> keys = allUsers.getAll();
//        for(Map.Entry<String,?> entry : keys.entrySet()){
//            if(entry.getKey().equals(currentUID)){
//                keys.remove(entry);
//            }
//        }
        resetCurrentUserValues();




        ArrayList<String> users = new ArrayList<String>();
        users = getUserList();
        if(!users.isEmpty()) {

            String uid = getUIDByEmail(users.get(0));
            setCurrentUser(uid);
            loadNewUser(uid);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putBoolean("userExists", true);
            return true;
        }else{
            setCurrentUser("");
            resetUserList();

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



           reAuthenticate(currentEmail, currentPW);
            return true;


        }

        return false;
//        return true;

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

    public String getCurrentUser(){
        SharedPreferences allUsers = appContext.getSharedPreferences("currentActiveUser", Context.MODE_PRIVATE);
        String currentUserId = allUsers.getString("user", null);

        return currentUserId;
    }
    public boolean setCurrentUser(String uid){
        SharedPreferences user = appContext.getSharedPreferences("currentActiveUser", Context.MODE_PRIVATE);
        SharedPreferences.Editor userActive = user.edit();
        userActive.putString("user", uid);
        return true;
    }

    public void resetUserList(){
        //this resets the user list to insure data deletion.
          currentUID = "Error";
          currentToken = "Error";
          currentEmail = "Error";
          currentPW = "Error";
          currentBirthdate = "Error";
          currentHeight1 = "Error";
          currentHeight2 = "Error";
          currentWeight = "Error";
          currentGender = "Error";
          currentFullName = "Error";
          currentUsername = "Error";


        SharedPreferences allUsers = appContext.getSharedPreferences("allUsers", Context.MODE_PRIVATE);
        SharedPreferences.Editor allUsersEditor = allUsers.edit();


        allUsersEditor.clear();



        allUsersEditor.commit();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().clear();
//        prefs.edit().putBoolean("userExists", false);
//        instance = null;
//        return allUsersReturn;
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
        userDataString.put("currentUID", currentUID);
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


        Firebase ref = new Firebase(UserData.firebase_url + "users/" +getCurUID() + "/metadata/");
        ref.setValue(userDataString);


    }


    public void downloadProfilePic(){
        Log.d(TAG, "Loading image from firebase");
//        fsaf
        Firebase ref = new Firebase(UserData.firebase_url + "users/" + currentUID + "/photos/profilepic");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //                    System.out.println(snapshot.getValue());
                if(snapshot.getChildrenCount()==3){
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    ArrayList<String> result =((ArrayList<String>)snapshot.getValue());
                    byte[] decodedString = Base64.decode(result.get(2), Base64.NO_WRAP);

                    DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();

                    String md5 = result.get(1);
                    Calendar profile = Calendar.getInstance();
                    profile.setTimeInMillis(0);
                    ContentValues syncValues = new ContentValues();
                    syncValues.put(Database.PhotoStore.DATE, 0);
                    syncValues.put(Database.PhotoStore.USER, currentUID);
                    syncValues.put(Database.PhotoStore.MD5, md5);
                    String guid = UUID.randomUUID().toString();
                    syncValues.put(Database.PhotoStore.GUID,guid);
                    long newRowId;
                    newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                            null,
                            syncValues);
                    Log.d(TAG, "Photo database add from firebase: "+newRowId);
                    db.close();

                    storePhoto(decodedString, profile.getTimeInMillis(), md5);
//
                }else if(snapshot.getChildrenCount()>3){
                    //multipart file upload
                    DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();

                    ArrayList<String> result = ((ArrayList<String>) snapshot.getValue());
                    try {
                        Calendar profile = Calendar.getInstance();
                        profile.setTimeInMillis(0);
                        String wholeString = "";
                        for(int i =2;i<result.size();i++){
                            wholeString += result.get(i);


                        }
                        String md5 = result.get(1);
                        byte[] decodedString = Base64.decode(wholeString, Base64.NO_WRAP);
//                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, options);
                        ContentValues syncValues = new ContentValues();
                        syncValues.put(Database.PhotoStore.DATE, 0);
                        syncValues.put(Database.PhotoStore.USER, currentUID);
//                        syncValues.put(Database.PhotoStore.PHOTOBLOB, decodedString);
                        syncValues.put(Database.PhotoStore.MD5, md5);
                        syncValues.put(Database.PhotoStore.GUID,UUID.randomUUID().toString());
                        long newRowId;
                        newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                                null,
                                syncValues);
                        Log.d(TAG, "Photo database add from firebase: " + newRowId);
                        db.close();

                        storePhoto(decodedString,0,md5 );
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                }


            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    public void downloadPhotoForDate(long today){

        Log.d(TAG, "Loading image from firebase");
        final Calendar monthCal = Calendar.getInstance();
        monthCal.setTimeInMillis(today);
        String monthChange = "";
        String dayChange = "";
        if ((monthCal.get(Calendar.MONTH)) < 11) {
            monthChange = "0" + (monthCal.get(Calendar.MONTH) + 1);
        } else {
            monthChange = String.valueOf(monthCal.get(Calendar.MONTH) + 1);
        }
        if ((monthCal.get(Calendar.DATE)) < 10) {
            dayChange = "0" + (monthCal.get(Calendar.DATE));
        } else {
            dayChange = String.valueOf(monthCal.get(Calendar.DATE));
        }
        //Checking firebase photo

//        Log.d(TAG, "Loading image from firebase");
        Firebase ref = new Firebase(UserData.firebase_url + "users/" + getCurUID() + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthChange + "/" + dayChange);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {

                    // TODO: Discuss changes below with Phil -- comment from Michael

                    Object obj = snapshot.getValue();
                    if (obj == null) {
                        Log.d(TAG, "MBA: Value from FB is null.");
                        return;
                    }

                    Log.d(TAG, snapshot.getValue().toString());
                    if (snapshot.getChildrenCount() == 3) {
                        ArrayList<String> result = ((ArrayList<String>) snapshot.getValue());

                        Object pictureObject = result.get(2);
                        String pictureString = String.valueOf(pictureObject);
                        String md5 = result.get(1);


                        try {
                            byte[] decodedString = Base64.decode(pictureString, Base64.NO_WRAP);


                            DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
                            SQLiteDatabase db = mDbHelper.getWritableDatabase();
//


                            //file doesn't exist on local device
                            Date curDay = DataUtilities.trim(new Date(monthCal.getTimeInMillis()));
                            ContentValues syncValues = new ContentValues();
                            syncValues.put(Database.PhotoStore.DATE, curDay.getTime());
                            syncValues.put(Database.PhotoStore.USER, getCurUID());
//                            syncValues.put(Database.PhotoStore.PHOTOBLOB, decodedString);
                            syncValues.put(Database.PhotoStore.MD5, md5);
                            syncValues.put(Database.PhotoStore.GUID,UUID.randomUUID().toString());
                            long newRowId;
                            newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                                    null,
                                    syncValues);
                            Log.d(TAG, "Photo database add from firebase: " + newRowId);
                            db.close();


                            storePhoto(decodedString,curDay.getTime(),md5 );

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

//

                    } else if (snapshot.getChildrenCount() > 3) {
                        Log.d(TAG, "Photo multipart");
                        //multipart file upload
                        DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
                        SQLiteDatabase db = mDbHelper.getWritableDatabase();
                        ArrayList<String> result = ((ArrayList<String>) snapshot.getValue());
                        try {
                            String wholeString = "";
                            for (int i = 2; i < result.size(); i++) {
                                wholeString += result.get(i);

                            }
                            String md5 = result.get(1);
                            byte[] decodedString = Base64.decode(wholeString, Base64.NO_WRAP);
//                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, options);
//                                background.setImageBitmap(decodedByte);
//                                background.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            Date curDay = DataUtilities.trim(new Date(monthCal.getTimeInMillis()));
                            ContentValues syncValues = new ContentValues();
                            syncValues.put(Database.PhotoStore.DATE, curDay.getTime());
                            syncValues.put(Database.PhotoStore.USER, getCurUID());
//                            syncValues.put(Database.PhotoStore.PHOTOBLOB, decodedString);
                            syncValues.put(Database.PhotoStore.MD5, md5);
                            syncValues.put(Database.PhotoStore.GUID,UUID.randomUUID().toString());
                            long newRowId;
                            newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                                    null,
                                    syncValues);
                            Log.d(TAG, "Photo database add from firebase: " + newRowId);
                            db.close();
                            storePhoto(decodedString,curDay.getTime(),md5 );

                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }





    public void setMetadata(Firebase child, String useruid){

//        setCurUID(useruid);
        child.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot.hasChildren()) {
                    setCurEmail(snapshot.child("currentEmail").getValue(String.class));
                    setCurHeight1(snapshot.child("currentHeight1").getValue(String.class));
                    setCurHeight2(snapshot.child("currentHeight2").getValue(String.class));
                    setCurWeight(snapshot.child("currentWeight").getValue(String.class));
                    setCurGender(snapshot.child("currentGender").getValue(String.class));
                    setCurName(snapshot.child("currentFullName").getValue(String.class));
                    setCurBirthdate(snapshot.child("currentBirthdate").getValue(String.class));
                    setCurUsername(snapshot.child("currentUsername").getValue(String.class));
                }
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });



    }
    public void downloadMetadata(String useruid){
        setCurUID(useruid);
        Firebase child = new Firebase(UserData.firebase_url + "users/"+currentUID+"/metadata");
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
                addCurUserTolist();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }


    public void insertStepsFromDB(DataSnapshot snapshot, Context c, String curMonth, String curYear){
        UserData myData = UserData.getUserData(c);
        Iterable<DataSnapshot> children = snapshot.getChildren();
        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        for (DataSnapshot child : children) {
            String date = child.getKey();
            Iterable<DataSnapshot> syncEvents = child.getChildren();
            for (DataSnapshot syncsForToday : syncEvents) {
                String syncName = syncsForToday.getKey();
                Iterable<DataSnapshot> stepEvents = syncsForToday.getChildren();
                for (DataSnapshot stepChunk : stepEvents) {
                    String stepTime = stepChunk.getKey();
                    Iterable<DataSnapshot> step = syncsForToday.getChildren();
                    Object stepEvent = stepChunk.getValue();
                    Map<String, String> monthMap = new HashMap<String, String>(); //day<minutes,steps>>
                    monthMap = (Map<String, String>) stepChunk.getValue();
                    Log.d(TAG, "Monthmap test" + monthMap);
                    Calendar thisCal = Calendar.getInstance();
                    thisCal.setTimeZone(TimeZone.getTimeZone("UTC"));

//                    Date curDate = monthMap.get("starttime").toString();
                    String dateConcatStart = curYear + "-" + curMonth + "-" + date + "" + monthMap.get("starttime").toString();
                    String dateConcatStop = curYear + "-" + curMonth + "-" + date + "" + monthMap.get("endtime").toString();


                    try {
                       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date curDateStart = sdf.parse(dateConcatStart);

                        Date curDateStop = sdf.parse(dateConcatStop);
//                        Log.d("TAG", "date is "+curDate);
                        thisCal.setTime(curDateStart);

                        ContentValues values = new ContentValues();
                        values.put(Database.StepEntry.GUID, UUID.randomUUID().toString());
                        values.put(Database.StepEntry.STEPS, Integer.parseInt(monthMap.get("count").toString()));
                        values.put(Database.StepEntry.START, thisCal.getTimeInMillis());
                        Log.d(TAG, "Inserting step count "+monthMap.get("count") + " into DB from firebase. Time zone set is "+thisCal.getTimeZone() + " And start is "+thisCal.getTime());
                        thisCal.setTime(curDateStop);
                        values.put(Database.StepEntry.END, thisCal.getTimeInMillis());
                        values.put(Database.StepEntry.USER,  UserData.getUserData(c).getCurUID());
                        values.put(Database.StepEntry.IS_PUSHED, 1); //this is downloaded from the cloud, it obviously has been pushed.
                        values.put(Database.StepEntry.SYNC_ID, monthMap.get("syncid"));
                        values.put(Database.StepEntry.DEVICEID, monthMap.get("deviceid"));
                        //        values.put(Database.StepEntry.WORKOUT_TYPE, point.Mode.);
                        //TODO: add workout type

                        long newRowId;

                        newRowId = db.insert(Database.StepEntry.STEPS_TABLE_NAME,
                                null,
                                values);
                        Log.d(TAG, "Database insert result: " + newRowId + " for: " + values);


                    } catch (Exception e) {
                        e.printStackTrace();
                        ;
                    }

                }

            }

        }

        db.close();
    }



    public void shouldDownloadNewPhoto(long today,String md5In){
        final String md5 = md5In;
        final long todayFinal = today;
        final Calendar monthCal = Calendar.getInstance();
        monthCal.setTimeInMillis(today);
        String monthChange = "";
        String dayChange = "";
        if ((monthCal.get(Calendar.MONTH)) < 11) {
            monthChange = "0" + (monthCal.get(Calendar.MONTH) + 1);
        } else {
            monthChange = String.valueOf(monthCal.get(Calendar.MONTH) + 1);
        }
        if ((monthCal.get(Calendar.DATE)) < 10) {
            dayChange = "0" + (monthCal.get(Calendar.DATE));
        } else {
            dayChange = String.valueOf(monthCal.get(Calendar.DATE));
        }
        //Checking firebase photo
//        final String byteString = Base64.encodeToString(photo,Base64.NO_WRAP);

        Log.d(TAG, "Checking md5 from firebase firebase");
        Firebase ref = new Firebase(UserData.firebase_url + "users/" + getCurUID() + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthChange + "/" + dayChange + "/1");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot.getValue()!=null) {
//                    String md5 = DataUtilities.getMD5EncryptedString(byteString);

                    if (md5.equals(snapshot.getValue())) {
                        //md5s match, don't download
                    } else {
                        //download new image
                        downloadPhotoForDate(todayFinal);
                    }
                }
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    public byte[] loadPhotoFromGuid(String guid){
        final File dir = new File(appContext.getFilesDir() +"/");
        File imageFile = new File(dir, guid);
        int size = (int) imageFile.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(imageFile));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bytes;
    }

    public byte[] retrievePhoto(long date){
        boolean localFile = false;
        DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Date currentDay = new Date(date);
        byte[] returnByte=null;
        currentDay = DataUtilities.trim(currentDay);
//        UserData myData = UserData.getUserData(c);
        String user =  UserData.getUserData(appContext).getCurUID();
        String photo = Database.PhotoStore.DATE + " =? AND " + Database.PhotoStore.USER + " =?";
        Cursor curPhoto = db.query(
                Database.PhotoStore.PHOTO_TABLE_NAME,  // The table to query
                new String[]{
                        Database.StepEntry.USER, //string
                        Database.PhotoStore.DATE, //int
//                        Database.PhotoStore.PHOTOBLOB, //blob
                        Database.PhotoStore.MD5, //string
                        Database.PhotoStore.GUID},
                // The columns to return
                photo,                                // The columns for the WHERE clause
                new String[]{currentDay.getTime() + "", user},                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        try{
//            if(curPhoto.getCount())
            curPhoto.moveToFirst();
            localFile = false;

            int uniquePic =0;
            if (curPhoto.getCount() != 0) {

                String md5 = curPhoto.getString(2);
                String guid = curPhoto.getString(3);

                Log.d(TAG, "Found photo for today "+md5);
                if (md5 != null) {
                    //pull photo from file via guid.
                    final File dir = new File(appContext.getFilesDir() +"/");
                    File imageFile = new File(dir, guid);

                    ExifInterface ei = new ExifInterface(imageFile.getPath());
                    int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);


                    int size = (int) imageFile.length();
                    byte[] bytes = new byte[size];
                    try {
                        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(imageFile));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();

                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    returnByte = bytes;
                } else {
//                    UserData.getUserData(appContext).downloadPhotoForDate(date);
                    return null;
                }

//
            }else {
                shouldDownloadNewPhoto(date, "");
                return null;

            }
        }catch(Exception e){
            shouldDownloadNewPhoto(date, "");
            e.printStackTrace();
        }finally {
            curPhoto.close();
//            shouldDownloadNewPhoto(date, "");
        }

        return returnByte;
    }

//    public void checkImageRotation(long date){
//        DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
//        SQLiteDatabase db = mDbHelper.getReadableDatabase();
//        Date currentDay = new Date(date);
//        currentDay = DataUtilities.trim(currentDay);
//        String user =  UserData.getUserData(appContext).getCurUID();
//        String photo = Database.PhotoStore.DATE + " =? AND " + Database.PhotoStore.USER + " =?";
//        Cursor curPhoto = db.query(
//                Database.PhotoStore.PHOTO_TABLE_NAME,  // The table to query
//                new String[]{
//                        Database.StepEntry.USER, //string
//                        Database.PhotoStore.DATE, //int
////                        Database.PhotoStore.PHOTOBLOB, //blob
//                        Database.PhotoStore.MD5, //string
//                        Database.PhotoStore.GUID},
//                // The columns to return
//                photo,                                // The columns for the WHERE clause
//                new String[]{currentDay.getTime() + "", user},                            // The values for the WHERE clause
//                null,                                     // don't group the rows
//                null,                                     // don't filter by row groups
//                null                                 // The sort order
//        );
//        try{
//            curPhoto.moveToFirst();
//            if (curPhoto.getCount() != 0) {
//
//                String md5 = curPhoto.getString(2);
//                String guid = curPhoto.getString(3);
//
//                Log.d(TAG, "Found photo for today "+md5);
//                if (md5 != null) {
//                    //pull photo from file via guid.
//                    final File dir = new File(appContext.getFilesDir() +"/");
//                    File imageFile = new File(dir, guid);
//
//                    ExifInterface ei = new ExifInterface(imageFile.getPath());
//                    int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//
//
//                    int size = (int) imageFile.length();
//                    byte[] bytes = new byte[size];
//                    try {
//                         BufferedInputStream buf = new BufferedInputStream(new FileInputStream(imageFile));
//                        buf.read(bytes, 0, bytes.length);
//                        buf.close();
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                        ByteArrayOutputStream stream;
//                        byte[] byteArray;
//                        switch(orientation) {
//                            case ExifInterface.ORIENTATION_ROTATE_90:
//                                Log.d(TAG, "Image rotated 90");
//                                bitmap = DataUtilities.RotateBitmap(bitmap, 90);
//                                stream = new ByteArrayOutputStream();
//                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//                                byteArray = stream.toByteArray();
//                                storePhoto(byteArray, date, md5);
//                                break;
//                            case ExifInterface.ORIENTATION_ROTATE_180:
//                                Log.d(TAG, "Image rotated 180");
//                                bitmap = DataUtilities.RotateBitmap(bitmap, 180);
//                                stream = new ByteArrayOutputStream();
//                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//                                byteArray = stream.toByteArray();
//                                storePhoto(byteArray, date, md5);
//                                break;
//                        }
//
//
//
//                    } catch (FileNotFoundException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }catch(Exception e){
//
//            e.printStackTrace();
//        }finally {
//            curPhoto.close();
//        }
//
//    }


    //overriden method
    public void storePhoto(ByteArrayOutputStream is, long date, String md5){
        boolean success = false;

        final String photoGuid = UUID.randomUUID().toString();
        try{
            final File dir = new File(appContext.getFilesDir() +"/");
            dir.mkdirs();
            final File imageFile = new File(dir, photoGuid);
            OutputStream outStream = new FileOutputStream(imageFile);
            Log.d(TAG, appContext.getFilesDir() + "/images/"+photoGuid);
            is.writeTo(outStream);
            success = true;
        }catch(Exception e){
//                        outStream.close();
            success = false;
            Log.d(TAG, "Photo storage failed - storePhoto via outputStream");
            e.printStackTrace();
        }

        if(success) {
            DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
            SQLiteDatabase db = mDbHelper.getWritableDatabase();


            Date curDay = DataUtilities.trim(new Date(date));
            ContentValues photoValues = new ContentValues();
            photoValues.put(Database.PhotoStore.DATE, curDay.getTime());
            photoValues.put(Database.PhotoStore.USER, currentUID);
            photoValues.put(Database.PhotoStore.MD5, md5);
            photoValues.put(Database.PhotoStore.GUID, photoGuid);


            long newRowId;
            newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                    null,
                    photoValues);
            Log.d(TAG, "New Photo database add: " + newRowId);
            db.close();
//            checkImageRotation(date);
        }
    }

    //overriden method
    public void storePhoto(byte[] byteArray, long date, String md5){
        boolean success = false;
        ByteArrayInputStream bos = new ByteArrayInputStream(byteArray);

        final String photoGuid = UUID.randomUUID().toString();
        try{
            final File dir = new File(appContext.getFilesDir() +"/");
            dir.mkdirs();
            final File imageFile = new File(dir, photoGuid);
            OutputStream outStream = new FileOutputStream(imageFile);
            Log.d(TAG, appContext.getFilesDir() + "/images/"+photoGuid);
            outStream.write(byteArray);
            success = true;
        }catch(Exception e){
//                        outStream.close();
            success = false;
            e.printStackTrace();
            Log.d(TAG, "Photo storage failed - storePhoto via byte[]");
        }

        if(success) {
            DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
            SQLiteDatabase db = mDbHelper.getWritableDatabase();


            Date curDay = DataUtilities.trim(new Date(date));
            ContentValues photoValues = new ContentValues();
            photoValues.put(Database.PhotoStore.DATE, curDay.getTime());
            photoValues.put(Database.PhotoStore.USER, currentUID);
            photoValues.put(Database.PhotoStore.MD5, md5);
            photoValues.put(Database.PhotoStore.GUID, photoGuid);


            long newRowId;
            newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                    null,
                    photoValues);
            Log.d(TAG, "New Photo database add: " + newRowId);
            db.close();
        }
    }

}