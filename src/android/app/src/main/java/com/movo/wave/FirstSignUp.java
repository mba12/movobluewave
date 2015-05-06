package com.movo.wave;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.Policy;
import java.util.Calendar;
import java.util.Map;

/**
 * Created by P on 3/31/2015.
 */
public class FirstSignUp extends Activity {
    Button login;
    Button forgot;
    String mEmail;
    String mUsername;
    String mPassword;
    String mPasswordConf;
    EditText username;
    EditText pass;
    EditText passConf;
    EditText usernameCust;
    TextView terms;
    TextView privacy;
    Firebase loginRef;
    ProgressBar loginProgress;
    Button birthdateButton;
    long birthdateInput;
    int mYear;
    int mMonth;
    int mDay;
    boolean usernameTaken=false;
    boolean is13;
    Context c;
//    CheckBox age;
    String TAG = "Movo.FirstLogin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = this.getApplicationContext();
        setContentView(R.layout.activity_first_sign_up);
        login = (Button) findViewById(R.id.loginButton);
        forgot = (Button) findViewById(R.id.forgotPass);
        loginProgress = (ProgressBar) findViewById(R.id.progressBarLogin);
        Firebase.setAndroidContext(c);
        loginRef = new Firebase("https://ss-movo-wave-v2.firebaseio.com/");
        birthdateButton = (Button) findViewById(R.id.birthdate);
        terms = (TextView) findViewById(R.id.tvTermsWhole);
//        privacy = (TextView) findViewById(R.id.tvPrivacy);

        terms.setText(
                Html.fromHtml(
                        "By proceeding, you also agree to Movo's <a href=\"http://www.getmovo.com/terms\">Terms of Service</a> and <a href=\"http://www.getmovo.com/privacy\">Privacy Policy</a>."));



        terms.setMovementMethod(LinkMovementMethod.getInstance());

//        privacy.setText(
//                Html.fromHtml(
//                        "<a href=\"http://www.getmovo.com/privacy\">Privacy Policy</a> "));
//        privacy.setMovementMethod(LinkMovementMethod.getInstance());

        username = (EditText) findViewById(R.id.username);
        pass = (EditText) findViewById(R.id.password);
        passConf = (EditText) findViewById(R.id.password2);
        usernameCust = (EditText) findViewById(R.id.usernameCust);

//        birthdate = (Button) findViewById(R.id.birthday);
        final Calendar cal = Calendar.getInstance();
        mYear = cal.get(Calendar.YEAR);
        mMonth = cal.get(Calendar.MONTH);
        mDay = cal.get(Calendar.DAY_OF_MONTH);


        birthdateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DatePickerDialog dpd = new DatePickerDialog(FirstSignUp.this,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                birthdateButton.setText((monthOfYear + 1)+"-"+dayOfMonth + "-" + year);

                                Calendar birthCal = Calendar.getInstance();
                                birthCal.set(Calendar.YEAR, year);
                                birthCal.set(Calendar.MONTH, monthOfYear);
                                birthCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                Calendar checkCal = Calendar.getInstance();
                                checkCal.add(Calendar.YEAR, -13);
                                if(birthCal.getTimeInMillis()>=checkCal.getTimeInMillis()){
                                    is13=false;
                                    Log.d(TAG,"Account not 13");
                                    Toast.makeText(c,"You must be 13 years of age or older to use this application.",Toast.LENGTH_LONG).show();
                                }else{
                                    Log.d(TAG,"Account IS 13");
                                    birthdateInput = birthCal.getTimeInMillis();
                                    is13=true;
                                }

                            }
                        }, mYear, mMonth, mDay);
                dpd.show();
            }
        });


        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!(username.getText().equals("")) && !(pass.getText().equals("")) && !(usernameCust.getText().equals(""))) {
                    if (is13) {

                        Firebase lookupEmail = new Firebase("https://ss-movo-wave-v2.firebaseio.com/emailtable/");
                        Firebase child = lookupEmail.child(usernameCust.getText().toString());
                        child.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if(snapshot.getValue()==null){
                                    Log.d(TAG, "Username not taken");
                                    mEmail = username.getText().toString();
                                    mUsername = usernameCust.getText().toString();
                                    mPassword = pass.getText().toString();
                                    mPasswordConf = passConf.getText().toString();

                                    if (mPassword.equals(mPasswordConf)) {
                                        loginProgress.setVisibility(View.VISIBLE);
                                        loginRef.createUser(mEmail, mPassword, new Firebase.ValueResultHandler<Map<String, Object>>() {
                                            @Override
                                            public void onSuccess(Map<String, Object> result) {
                                                Log.d(TAG,"Successfully created user account with uid: " + result.get("uid"));

                                                loginRef.authWithPassword(mEmail, mPassword, new Firebase.AuthResultHandler() {
                                                    @Override
                                                    public void onAuthenticated(AuthData authData) {
                                                        //success, save auth data
                                                        UserData myData = UserData.getUserData(c);
                                                        myData.setCurUID(authData.getUid());
                                                        myData.setCurToken(authData.getToken());
                                                        myData.setCurEmail(mEmail);
                                                        myData.setCurPW(mPassword);
                                                        myData.setCurBirthdate(birthdateInput + "");
                                                        myData.setCurUsername(mUsername);
                                                        Firebase currentUserRef = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + authData.getUid());

                                                        myData.setCurrentUserRef(currentUserRef);
                                                        myData.addCurUserTolist();

                                                        //username lookup table
                                                        Firebase usernameEmailTies = new Firebase("https://ss-movo-wave-v2.firebaseio.com/emailtable");
                                                        Firebase thisUser = usernameEmailTies.child(mUsername);
                                                        thisUser.setValue(mEmail);

//                                                        https://devorders.getmovo.com/verify/user-signup?fullname=Phil%20Gandy&email=philip.gandy@gmail.com
//                                                        try {
//                                                            URL url = new URL("https://devorders.getmovo.com/verify/user-signup?fullname="+mUsername+"&email="+mEmail);
//
//                                                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
//                                                            urlConnection.disconnect();
//                                                        }catch (Exception e){
//                                                            e.printStackTrace();
//                                                        }
//                                                        URL url;
                                                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                                                        StrictMode.setThreadPolicy(policy);
                                                        try {
                                                            HttpClient httpclient = new DefaultHttpClient();
                                                            String http = "https://devorders.getmovo.com/verify/user-signup?fullname="+mUsername+"&email="+mEmail;
                                                            HttpGet request = new HttpGet();
                                                            URI website = new URI(http);
                                                            request.setURI(website);
                                                            HttpResponse response = httpclient.execute(request);
                                                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                                                    response.getEntity().getContent()));

//                                                            // NEW CODE
//                                                            String line = in.readLine();
//                                                            textv.append(" First line: " + line);
//                                                            // END OF NEW CODE

//                                                            textv.append(" Connected ");
                                                        } catch (Exception e) {
                                                            // TODO Auto-generated catch block
                                                            e.printStackTrace();
                                                        }


                                                        loginProgress.setVisibility(View.GONE);

                                                        Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
                                                        Intent intent = new Intent(getApplicationContext(),
                                                                Home.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }

                                                    @Override
                                                    public void onAuthenticationError(FirebaseError firebaseError) {
                                                        Log.d(TAG, "Error authenticating newly created user. This could be an issue. ");
                                                        loginProgress.setVisibility(View.GONE);
                                                        Toast.makeText(c, firebaseError.getMessage(), Toast.LENGTH_LONG).show();

                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(FirebaseError firebaseError) {
                                                Log.d(TAG, "Error creating user: " + firebaseError.getMessage());
                                                loginProgress.setVisibility(View.GONE);
                                                Toast.makeText(c, firebaseError.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    } else {
                                        Toast.makeText(c, "Passwords do not match.", Toast.LENGTH_LONG).show();
                                    }
                                }else{
                                    Log.d(TAG, "Username taken");
                                    String email = snapshot.getValue().toString();
                                    Toast.makeText(c, "Username already taken.", Toast.LENGTH_LONG).show();
                                }

//


                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                Toast.makeText(c, "Username not taken", Toast.LENGTH_LONG).show();
                            }


                        });




                    } else {
                        Toast.makeText(c, "Failed to create account. Please refer to our Terms of Service for registration guidelines, including minimum age.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(c, "Please fill in all fields", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
