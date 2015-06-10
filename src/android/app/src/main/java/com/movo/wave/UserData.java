package com.movo.wave;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;


/**
 * Created by PhilG on 3/24/2015.
 */


public class UserData extends Activity{
    private static UserData instance;
    private static String TAG = "Wave.UserData";
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
    private Home homeView;
    final static String firebase_url = "https://ss-movo-wave-v2.firebaseio.com/";



    public static UserData getUserData(Context c) {
        if (instance == null) {
            instance = new UserData(c);
        }
        return instance;
    }

    static private final Set<UpdateDelegate> listenerDelegates = new HashSet<>();
    static private boolean notifyPending = false;
    static private final Handler notifyHandler = new Handler();
    static final long notifyDelay = 1000;

    /** Generic listener interface for metadata and maybe other changes
     *
     * @param delegate for update notifications
     */
    public static void addListener( UpdateDelegate delegate ) {
        synchronized (listenerDelegates) {
            listenerDelegates.add(delegate);
        }
    }

    /** voluntary removal of listerner for the {@link com.movo.wave.UserData.UpdateDelegate( UpdateDelegate )} call
     *
     * note: invalidated delegates are usually removed at the next notify cycle
     *
     * @param delegate to remove
     */
    public static void removeListener( UpdateDelegate delegate ) {
        synchronized (listenerDelegates) {
            listenerDelegates.remove(delegate);
        }
    }

    /** Internal call to notify listeners via delegates, replete with debounce.
     *
     */
    private static void notifyListeners() {
        synchronized (listenerDelegates) {
            final long now = new Date().getTime();

            //Debounce notifications to once/notifyDelay time period.
            if( ! notifyPending ) {

                notifyHandler.postDelayed( new Runnable() {
                    @Override
                    public void run() {
                        synchronized (listenerDelegates) {
                            final List<UpdateDelegate> invalidDelegates = new LinkedList<UpdateDelegate>();

                            int notifyCount = 0;

                            for (final UpdateDelegate delegate : listenerDelegates) {
                                if( delegate.isInvalidated() ) {
                                    invalidDelegates.add( delegate );
                                } else {
                                    delegate.notifyUpdate();
                                    notifyCount += 1;
                                }
                            }

                            for( final UpdateDelegate delegate : invalidDelegates) {
                                listenerDelegates.remove( delegate );
                                Log.d( TAG, "Removing invalidated delegate " + delegate );
                            }

                            Log.d( TAG, "Notified " + notifyCount + " UpdateDelegates listeners");
                            notifyPending = false;
                        }
                    }
                }, notifyDelay );

                notifyPending = true;
            }
        }
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
        notifyListeners();
        return currentUID;
    }

    public String setCurToken(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentToken", input).commit();
        currentToken = input;
        notifyListeners();
        return currentToken;
    }

    public String setCurBirthdate(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentBirthdate", input + "").commit();
        currentBirthdate = input+"";
        notifyListeners();
        return currentToken;
    }
    public String setCurEmail(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentEmail", input).commit();
        currentEmail = input;
        notifyListeners();
        return currentEmail;
    }

    public String setCurName(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentFullName", input).commit();
        currentFullName = input;
        notifyListeners();
        return currentFullName;

    }

    public String setCurHeight1(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentHeight1", input).commit();
        currentHeight1 = input;
        notifyListeners();
        return currentHeight1;
    }

    public String setCurHeight2(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentHeight2", input).commit();
        currentHeight2 = input;
        notifyListeners();
        return currentHeight2;
    }

    public String setCurWeight(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentWeight", input).commit();
        currentWeight = input;
        notifyListeners();
        return currentWeight;
    }

    public String setCurGender(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentGender", input).commit();
        currentGender = input;
        notifyListeners();
        return currentGender;
    }
    public String setCurUsername(String input) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentUsername", input).commit();
        currentUsername = input;
        notifyListeners();
        return currentUsername;
    }



    public String setCurPW(String input) {
        //TODO: We will want to encrypt this, as android shared prefs are only secure if the device isn't rooted.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        prefs.edit().putString("currentPW", input).commit();
        currentPW = input;
        notifyListeners();
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

                notifyListeners();
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

        notifyListeners();
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


    private static Set<String> lockedPictureDigests = new HashSet<>();

    /** lock md5 digest as in progress
     *
     * @param md5Digest
     * @return lock success
     */
    private static boolean lockPicture( String md5Digest ) {
        final boolean ret;
        synchronized (lockedPictureDigests) {
            ret = lockedPictureDigests.add(md5Digest);
        }
        return ret;
    }

    /** unlock md5 digest as in progress
     *
     * @param md5Digest
     * @return unlock success
     */
    private static boolean unlockPicture( String md5Digest ) {
        final boolean ret;
        synchronized (lockedPictureDigests) {
            ret = lockedPictureDigests.remove(md5Digest);
        }
        return ret;
    }

    public void downloadProfilePic(){
        Log.d(TAG, "Loading image from firebase");
//        fsaf
        Firebase ref = new Firebase(UserData.firebase_url + "users/" + currentUID + "/photos/profilepic");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //                    System.out.println(snapshot.getValue());
                if (snapshot.getChildrenCount() == 3) {
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    ArrayList<String> result = ((ArrayList<String>) snapshot.getValue());
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
                    syncValues.put(Database.PhotoStore.GUID, guid);
                    long newRowId;
                    newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                            null,
                            syncValues);
                    Log.d(TAG, "Photo database add from firebase: " + newRowId);
                    db.close();

                    storePhoto(decodedString, profile.getTimeInMillis(), md5);
//
                } else if (snapshot.getChildrenCount() > 3) {
                    //multipart file upload
                    DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();

                    ArrayList<String> result = ((ArrayList<String>) snapshot.getValue());
                    try {
                        Calendar profile = Calendar.getInstance();
                        profile.setTimeInMillis(0);
                        String wholeString = "";
                        for (int i = 2; i < result.size(); i++) {
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
                        syncValues.put(Database.PhotoStore.GUID, UUID.randomUUID().toString());
                        long newRowId;
                        newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                                null,
                                syncValues);
                        Log.d(TAG, "Photo database add from firebase: " + newRowId);
                        db.close();

                        storePhoto(decodedString, 0, md5);
                    } catch (Exception e) {
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

    public void downloadPhotoForDate(final long today, final String expectedMd5, final UpdateDelegate delegate){

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
        Firebase ref;
        if(today==0){
            ref = new Firebase(UserData.firebase_url + "users/" + getCurUID() + "/photos/profilepic/");
        }else{
            ref = new Firebase(UserData.firebase_url + "users/" + getCurUID() + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthChange + "/" + dayChange);
        }

//        Firebase ref = new Firebase(UserData.firebase_url + "users/" + getCurUID() + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthChange + "/" + dayChange);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {

                    final String md5;
                    final boolean locked;
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
                        md5 = result.get(1);

                        //record if expectations match reality. note locked == false is possible.
                        locked = expectedMd5.equals( md5 ) || lockPicture( md5 );

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
                        md5 = result.get(1);


                        //record if expectations match reality. note locked == false is possible.
                        locked = expectedMd5.equals( md5 ) || lockPicture( md5 );
                        try {
                            String wholeString = "";
                            for (int i = 2; i < result.size(); i++) {
                                wholeString += result.get(i);

                            }
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
                    } else {
                        Log.e( TAG, "Unexpected edge case where values < 3 " + expectedMd5 + " datestamp " + today );
                        md5 = null;
                        locked = true;
                    }


                    /*
                    Lots of debugging info here. If locked is false, we lost the race condition on an unexpected digest.
                    Otherwise, if  there was an unexpected digest, we were able to lock it (and should unlock it).

                    Either way, if there is a secondary digest, is possible we're dropping update delegates elsewhere or hitting a
                    race condition.
                     */
                    if( ! locked ) {
                        Log.e(TAG, "CONFLICT!!!! we lost a data race for image " + md5 + " datestamp " + today);
                        Log.w(TAG, "We're in poorly designed space here. If this happens often, we should change the data model. "
                                + expectedMd5 + " != " + md5 + " datestamp " + today);
                    } else if( ! expectedMd5.equals(md5 ) ) {
                        if( ! unlockPicture(md5) ) {
                            Log.e( TAG, "CONFLICT: Someone unlocked secondary " + md5 + " underneath us!! datestamp " + today);
                        }
                        Log.w(TAG, "We're in poorly designed space here. If this happens often, we should change the data model. "
                                + expectedMd5 + " != " + md5  + " datestamp " + today  );
                    }

                    // Always a photo! Always update!
                    if( ! unlockPicture(expectedMd5) ) {
                        Log.e( TAG, "CONFLICT: Someone unlocked primary " + expectedMd5 + " underneath us!! datestamp " + today);
                    };
                    delegate.notifyUpdate();
                }
            } // end onDataChange

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

                    notifyListeners();
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

                notifyListeners();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    /** Dispatch updates to UI thread
     *
     */
    public static abstract class UpdateDelegate implements Runnable {

        private boolean valid = true;
        public final Handler UIHandler;
        public UpdateDelegate( Context context ) {
            UIHandler = new Handler( context.getMainLooper() );
        }

        public synchronized void invalidate() {
            valid = false;
        }

        public final synchronized void run() {
            if(valid) {
                this.onUpdate();
            }
        }

        /** Implement to receive updates
         *
         */
        abstract public void onUpdate();

        /** signal update to listener on UI thread
         *
         */
        public final void notifyUpdate() {
            UIHandler.post( this );
        }

        /** Pessimistic indicator of validity
         *
         * @return if the delegate is definitely invalid.
         */
        public boolean isInvalidated() {
            return ! valid;
        }
    }


    public void insertStepsFromDB(Firebase ref, Context c, final String curMonth, final String curYear, final UpdateDelegate delegate){
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "" + snapshot.getValue());
//                        loginProgress.setVisibility(View.INVISIBLE);

//                UserData.getUserData(c).insertStepsFromDB(snapshot, c, monthChangefinal, calendar.get(Calendar.YEAR)+"", instance);

                Iterable<DataSnapshot> children = snapshot.getChildren();
                DatabaseHelper mDbHelper = new DatabaseHelper(appContext);
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
                                values.put(Database.StepEntry.USER,  UserData.getUserData(appContext).getCurUID());
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

                delegate.notifyUpdate();
                db.close();
                Log.d(TAG, "Refreshing home UI");

                Log.d(TAG, "Inserting steps into database");


            }


            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d(TAG, "The read failed: " + firebaseError.getMessage());
            }
        });
//        homeView = home;






    }
//    public void refreshHome( Home home){
//        homeView = home;
//        homeView.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                //bah.
////                homeView.refreshCharts();
//            }
//        });
//    }



    public void shouldDownloadNewPhoto(long today,String md5In, final UpdateDelegate delegate){
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
        Firebase ref;
        if(today==0){
            ref = new Firebase(UserData.firebase_url + "users/" + getCurUID() + "/photos/profilepic/1");
        }else{
            ref = new Firebase(UserData.firebase_url + "users/" + getCurUID() + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthChange + "/" + dayChange + "/1");
        }
        Log.d(TAG, "Checking md5 from firebase firebase");
//        Firebase ref = new Firebase(UserData.firebase_url + "users/" + getCurUID() + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthChange + "/" + dayChange + "/1");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
//                    String md5 = DataUtilities.getMD5EncryptedString(byteString);

                    final String firebaseMd5 = (String)snapshot.getValue();

                    if (md5.equals(firebaseMd5)) {
                        //md5s match, don't download
                    } else if( lockPicture( firebaseMd5 )) {
                        //download new image
                        downloadPhotoForDate(todayFinal, firebaseMd5, delegate);
                    } else {
                        /*
                        Note: this SILENTLY drops the delegate with no further notifications. Even
                        though a valid image is in progress, the delegate will not be triggered when
                        it is available. It may be triggered for other reasons, but we can't count
                        on that....
                        */
                        Log.v( TAG, "Already downloading image " + firebaseMd5);
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

//    public byte[] loadPhotoFromGuid(String guid){
//        final File dir = new File(appContext.getFilesDir() +"/");
//        File imageFile = new File(dir, guid);
//        int size = (int) imageFile.length();
//        byte[] bytes = new byte[size];
//        try {
//            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(imageFile));
//            buf.read(bytes, 0, bytes.length);
//            buf.close();
//        } catch (FileNotFoundException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return bytes;
//    }

    public byte[] retrievePhoto(final SQLiteDatabase db, long date, final UpdateDelegate delegate ){
        boolean localFile = false;
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

                    shouldDownloadNewPhoto(date, md5, delegate);
                } else {
//                    UserData.getUserData(appContext).downloadPhotoForDate(date);
                    return null;
                }

//
            }else {
                shouldDownloadNewPhoto(date, "", delegate );
                return null;

            }
        }catch(Exception e){
            shouldDownloadNewPhoto(date, "", delegate);
            e.printStackTrace();
        }finally {
            curPhoto.close();
//            shouldDownloadNewPhoto(date, "");
        }

        return returnByte;
    }



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