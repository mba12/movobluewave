package com.movo.wave;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.movo.wave.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class FirstLogin extends Activity {
    Button login;
    Button forgot;
    String mEmail;
    String mPassword;
    EditText username;
    EditText pass;
    Firebase loginRef;
    static Context c;
    static String TAG = "Movo.FirstLogin";
    static String curYear;
    static String curMonth;
    ProgressBar loginProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = this.getApplicationContext();
        setContentView(R.layout.activity_first_login);
        login = (Button) findViewById(R.id.loginButton);
        forgot = (Button) findViewById(R.id.forgotPass);
        Firebase.setAndroidContext(c);
        loginRef = new Firebase("https://ss-movo-wave-v2.firebaseio.com/");



        username = (EditText) findViewById(R.id.username);
        pass = (EditText) findViewById(R.id.password);
        loginProgress = (ProgressBar) findViewById(R.id.progressBarLogin);




        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mEmail = username.getText().toString();
                mPassword = pass.getText().toString();
                loginProgress.setVisibility(View.VISIBLE);
                loginRef.authWithPassword(mEmail, mPassword, new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        //success, save auth data
                        UserData myData = UserData.getUserData(c);
                        myData.setCurUID(authData.getUid());
                        myData.setCurToken(authData.getToken());
                        myData.setCurEmail(mEmail);
                        myData.setCurPW(mPassword);
                        Firebase currentUserRef = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + authData.getUid());
                        myData.setCurrentUserRef(currentUserRef);
                        boolean firstTime = myData.addCurUserTolist();
                        Calendar cal = Calendar.getInstance();
                        int month = cal.get(Calendar.MONTH);
                        month++;
                        int year = cal.get(Calendar.YEAR);
                        curYear = year + "";
                        curMonth = month+"";


                        Firebase child =currentUserRef.child("/steps/"+cal.get(Calendar.YEAR)+"/"+month);
                        child.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                System.out.println(snapshot.getValue());
                                loginProgress.setVisibility(View.INVISIBLE);

                                insertSteps(snapshot);

//                                Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
                                Intent intent = new Intent(getApplicationContext(),
                                        Home.class);
                                startActivity(intent);
                            }
                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                System.out.println("The read failed: " + firebaseError.getMessage());
                            }
                        });


//                        if(firstTime){
//                            //we gotta download the user's data from firebase
//
//
//
//                        }else{
//                            //we should sync the user's data with firebase, but this can happen in the background
////                            loginProgress.setVisibility(View.INVISIBLE);
////
////
////                            Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
////                            Intent intent = new Intent(getApplicationContext(),
////                                    Home.class);
////                            startActivity(intent);
//                        }






                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError) {
                        Log.d(TAG,"Error logging in. ");

                        Toast.makeText(c, "Could not Authenticate user", Toast.LENGTH_SHORT);
                    }
                });

            }
        });

    }
    private static void insertSteps(DataSnapshot snapshot) {
        UserData myData = UserData.getUserData(c);
        Iterable<DataSnapshot> children = snapshot.getChildren();
        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        for (DataSnapshot child : children){
            String date = child.getKey();
            Iterable<DataSnapshot> syncEvents = child.getChildren();
            for(DataSnapshot syncsForToday : syncEvents){
                String syncName = syncsForToday.getKey();
                Iterable<DataSnapshot> stepEvents = syncsForToday.getChildren();
                for(DataSnapshot stepChunk : stepEvents){
                    String stepTime = stepChunk.getKey();
                    Iterable<DataSnapshot> step = syncsForToday.getChildren();
                    Object stepEvent = stepChunk.getValue();
                    Map<String, String> monthMap = new HashMap<String, String>(); //day<minutes,steps>>
                    monthMap = (Map<String, String>) stepChunk.getValue();
                    Log.d(TAG, "Monthmap test"+monthMap);
                    Calendar thisCal = Calendar.getInstance();
//                    Date curDate = monthMap.get("starttime").toString();
                    String dateConcatStart = curYear + "-" +curMonth+ "-" +date+ "" +monthMap.get("starttime").toString();
                    String dateConcatStop = curYear + "-" +curMonth+ "-" +date+ "" +monthMap.get("endtime").toString();


                    try {
                        Date curDateStart = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateConcatStart);
                        Date curDateStop = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateConcatStop);
//                        Log.d("TAG", "date is "+curDate);
                        thisCal.setTime(curDateStart);

                        ContentValues values = new ContentValues();
                        values.put(Database.StepEntry.GUID, UUID.randomUUID().toString());
                        values.put(Database.StepEntry.STEPS, Integer.parseInt(monthMap.get("count").toString()));
                        values.put(Database.StepEntry.START,curDateStart.getTime());
                        values.put(Database.StepEntry.END,curDateStop.getTime());
                        values.put(Database.StepEntry.USER,myData.getCurUID());
                        values.put(Database.StepEntry.IS_PUSHED, 1); //this is downloaded from the cloud, it obviously has been pushed.
                        values.put(Database.StepEntry.SYNC_ID, monthMap.get("syncid"));
                        values.put(Database.StepEntry.DEVICEID, monthMap.get("deviceid"));
                //        values.put(Database.StepEntry.WORKOUT_TYPE, point.Mode.);
                        //TODO: add workout type

                        long newRowId;

                        newRowId = db.insert(Database.StepEntry.STEPS_TABLE_NAME,
                                null,
                                values);
                        Log.d(TAG, "Database insert result: "+newRowId+" for: "+values);



                    }catch(Exception e){
                        e.printStackTrace();;
                    }

                }

            }

        }

        db.close();

    }


}
