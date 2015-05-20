package com.movo.wave;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.movo.wave.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
    EditText pass1;
    EditText pass2;
    String resetPass1;
    String resetPass2;
    View dialogView;
    String mEmail;
    String mPassword;
    String usernameCust ="";
    EditText username;
    EditText pass;
    Firebase loginRef;
    Button forgotPass;
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
        forgotPass = (Button) findViewById(R.id.forgotPass);


        forgotPass.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                loginProgress.setVisibility(View.VISIBLE);
                mEmail = username.getText().toString();
                if (mEmail.equals("")) {
                    Toast.makeText(c, "Error: Email is empty", Toast.LENGTH_LONG).show();
                } else {

                    if (false) { //(!(mEmail.contains("@"))
                        Firebase lookupEmail = new Firebase("https://ss-movo-wave-v2.firebaseio.com/emailtable/");
                        Firebase child = lookupEmail.child(mEmail);
                        child.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                Log.d(TAG, snapshot + "");
                                String email = snapshot.getValue().toString();
                                usernameCust = mEmail;
                                mEmail = email;

                                loginRef.resetPassword(mEmail, new Firebase.ResultHandler() {
                                    @Override
                                    public void onSuccess() {
                                        // password reset email sent
                                        Toast.makeText(c, "Password reset email has been sent", Toast.LENGTH_LONG).show();
                                        loginProgress.setVisibility(View.GONE);
                                    }

                                    @Override
                                    public void onError(FirebaseError firebaseError) {
                                        // error encountered
                                        Toast.makeText(c, "Error: " + firebaseError.getMessage(), Toast.LENGTH_LONG).show();
                                        loginProgress.setVisibility(View.GONE);
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                Toast.makeText(c, firebaseError.getMessage(), Toast.LENGTH_LONG).show();
                            }


                        });
                    } else {
                       //is an email
                        loginRef.resetPassword(mEmail, new Firebase.ResultHandler() {
                            @Override
                            public void onSuccess() {
                                // password reset email sent
                                Toast.makeText(c, "Password reset email has been sent", Toast.LENGTH_LONG).show();
                                loginProgress.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(FirebaseError firebaseError) {
                                // error encountered
                                Toast.makeText(c, "Error: " + firebaseError.getMessage(), Toast.LENGTH_LONG).show();
                                loginProgress.setVisibility(View.GONE);
                            }
                        });
                    }



                }
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mEmail = username.getText().toString();
                mPassword = pass.getText().toString();
                loginProgress.setVisibility(View.VISIBLE);
//                loginRef.auth

                if (false) {//(!(mEmail.contains("@"))
                    Firebase lookupEmail = new Firebase("https://ss-movo-wave-v2.firebaseio.com/emailtable/");
                    Firebase child = lookupEmail.child(mEmail);
                    child.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Log.d(TAG, snapshot + "");
                            String email = snapshot.getValue().toString();
                            usernameCust = mEmail;
                            mEmail = email;


                            login();


                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            Toast.makeText(c, firebaseError.getMessage(), Toast.LENGTH_LONG).show();
                        }


                    });
                } else {
                    login();
                }
            }
        });
    }


    private static void insertSteps(DataSnapshot snapshot) {
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
//                    Date curDate = monthMap.get("starttime").toString();
                    String dateConcatStart = curYear + "-" + curMonth + "-" + date + "" + monthMap.get("starttime").toString();
                    String dateConcatStop = curYear + "-" + curMonth + "-" + date + "" + monthMap.get("endtime").toString();


                    try {
                        Date curDateStart = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateConcatStart);
                        Date curDateStop = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateConcatStop);
//                        Log.d("TAG", "date is "+curDate);
                        thisCal.setTime(curDateStart);

                        ContentValues values = new ContentValues();
                        values.put(Database.StepEntry.GUID, UUID.randomUUID().toString());
                        values.put(Database.StepEntry.STEPS, Integer.parseInt(monthMap.get("count").toString()));
                        values.put(Database.StepEntry.START, curDateStart.getTime());
                        values.put(Database.StepEntry.END, curDateStop.getTime());
                        values.put(Database.StepEntry.USER, myData.getCurUID());
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

    public void login() {
        loginRef.authWithPassword(mEmail, mPassword, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                //success, save auth data
                UserData myData = UserData.getUserData(c);
                myData.setCurUID(authData.getUid());
                myData.setCurToken(authData.getToken());
                myData.setCurEmail(mEmail);
                myData.setCurPW(mPassword);

                final Firebase currentUserRef = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + authData.getUid());
                myData.setCurrentUserRef(currentUserRef);
                Firebase metadataChild = currentUserRef.child("metadata");

                boolean firstTime = myData.addCurUserTolist();
                final Calendar cal = Calendar.getInstance();
                int monthtemp = cal.get(Calendar.MONTH);
                final int month = monthtemp + 1;
                int year = cal.get(Calendar.YEAR);
                curYear = year + "";
                curMonth = month + "";

                if ((Boolean) authData.getProviderData().get("isTemporaryPassword")) {

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(FirstLogin.this);
// ...Irrelevant code for customizing the buttons and title
                    LayoutInflater inflater = FirstLogin.this.getLayoutInflater();
                    dialogView = inflater.inflate(R.layout.reset_password_prompt, null);
                    dialogBuilder.setView(dialogView);

                    pass1 = (EditText) dialogView.findViewById(R.id.pass1);
                    pass2 = (EditText) dialogView.findViewById(R.id.pass2);
                    Button reset = (Button) dialogView.findViewById(R.id.reset);
                    Button cancel = (Button) dialogView.findViewById(R.id.cancel);


                    final AlertDialog alertDialog = dialogBuilder.create();
                    reset.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {

                            resetPass1 = pass1.getText().toString();
                            resetPass2 = pass2.getText().toString();

                            if (resetPass1.equals(resetPass2)) {
                                ProgressBar pb2 = (ProgressBar) dialogView.findViewById(R.id.progressBar2);
                                pb2.setVisibility(View.VISIBLE);
                                Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/");
                                ref.changePassword(mEmail, mPassword, resetPass1, new Firebase.ResultHandler() {
                                    @Override
                                    public void onSuccess() {
                                        mPassword = resetPass1;

                                       login();
                                        alertDialog.cancel();
//                                        UserData myData = UserData.getUserData(c);
//                                        myData.downloadProfilePic();
//
////                                                myData.setCurUID(authData.getUid());
////                                                myData.setCurToken(authData.getToken());
////                                                myData.setCurEmail(mEmail);
//                                        mPassword = resetPass1;
//                                        myData.setCurPW(resetPass1);
////                                        login();
//                                        // password changed
//                                        Firebase child = currentUserRef.child("/steps/" + cal.get(Calendar.YEAR) + "/" + month);
//                                        child.addValueEventListener(new ValueEventListener() {
//                                            @Override
//                                            public void onDataChange(DataSnapshot snapshot) {
//                                                System.out.println(snapshot.getValue());
//                                                loginProgress.setVisibility(View.INVISIBLE);
//
//
//
//                                                insertSteps(snapshot);
//
//
//
////                                Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
//                                                ProgressBar pb2 = (ProgressBar) dialogView.findViewById(R.id.progressBar2);
//                                                pb2.setVisibility(View.GONE);
//
//
//                                                Intent intent = new Intent(getApplicationContext(),
//                                                        Home.class);
//                                                startActivity(intent);
//                                                alertDialog.cancel();
//                                                finish();
//
//                                            }
//
//                                            @Override
//                                            public void onCancelled(FirebaseError firebaseError) {
//                                                ProgressBar pb2 = (ProgressBar) dialogView.findViewById(R.id.progressBar2);
//                                                pb2.setVisibility(View.GONE);
//                                                System.out.println("The read failed: " + firebaseError.getMessage());
//                                            }
//                                        });
                                    }

                                    @Override
                                    public void onError(FirebaseError firebaseError) {
                                        // error encountered
                                        Toast.makeText(c, "Reset Passwords do not match", Toast.LENGTH_LONG).show();
                                        alertDialog.cancel();
                                    }
                                });


                            } else {
                                Toast.makeText(c, "Reset Passwords do not match", Toast.LENGTH_LONG).show();
                            }

                        }
                    });

                    cancel.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            alertDialog.cancel();
                        }
                    });
                    alertDialog.show();


                } else {
//                    UserData myData = UserData.getUserData(c);
                    myData.setMetadata(metadataChild,authData.getUid());
                    myData.downloadMetadata(authData.getUid());
                    myData.downloadProfilePic();
                    if(usernameCust!=""){
                        myData.setCurUsername(usernameCust);
                    }
                    Firebase child = currentUserRef.child("/steps/" + cal.get(Calendar.YEAR) + "/" + month);
                    child.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Log.d(TAG,""+snapshot.getValue());


                            insertSteps(snapshot);
//                            child.removeEventListener(currentUserRef);
//                                Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());

                            loginProgress.setVisibility(View.INVISIBLE);
                            Intent intent = new Intent(getApplicationContext(),
                                    Home.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            System.out.println("The read failed: " + firebaseError.getMessage());
                        }
                    });
                }


            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.d(TAG, "Error logging in. ");
                loginProgress.setVisibility(View.INVISIBLE);
                Toast.makeText(c, "Could not Authenticate user", Toast.LENGTH_LONG).show();
            }
        });
    }
}

