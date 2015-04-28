package com.movo.wave;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Map;

/**
 * Created by P on 3/31/2015.
 */
public class FirstSignUp extends Activity {
    Button login;
    Button forgot;
    String mEmail;
    String mPassword;
    String mPasswordConf;
    EditText username;
    EditText pass;
    EditText passConf;
    TextView terms;
    TextView privacy;
    Firebase loginRef;
    ProgressBar loginProgress;
    Context c;
    CheckBox age;
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
         age = (CheckBox) findViewById(R.id.cbAge);
        age.setChecked(false);
        terms = (TextView) findViewById(R.id.tvTerms);
        privacy = (TextView) findViewById(R.id.tvPrivacy);

        terms.setText(
                Html.fromHtml(
                        "<a href=\"http://www.getmovo.com/terms\">Terms of Service</a> "));
        terms.setMovementMethod(LinkMovementMethod.getInstance());

        privacy.setText(
                Html.fromHtml(
                        "<a href=\"http://www.getmovo.com/privacy\">Privacy Policy</a> "));
        privacy.setMovementMethod(LinkMovementMethod.getInstance());

        username = (EditText) findViewById(R.id.username);
        pass = (EditText) findViewById(R.id.password);
        passConf = (EditText) findViewById(R.id.password2);


        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (age.isChecked()) {



                    mEmail = username.getText().toString();
                    mPassword = pass.getText().toString();
                    mPasswordConf = passConf.getText().toString();

                    if(mPassword.equals(mPasswordConf)) {
                        loginProgress.setVisibility(View.VISIBLE);
                        loginRef.createUser(mEmail, mPassword, new Firebase.ValueResultHandler<Map<String, Object>>() {
                            @Override
                            public void onSuccess(Map<String, Object> result) {
                                System.out.println("Successfully created user account with uid: " + result.get("uid"));
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
                                        myData.addCurUserTolist();
//                                myData.addCurUserTolist();
                                        loginProgress.setVisibility(View.GONE);

                                        Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
//                                updateHomePage();
                                        Intent intent = new Intent(getApplicationContext(),
                                                Home.class);
                                        startActivity(intent);

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
                    }else{
                        Toast.makeText(c, "Passwords do not match.", Toast.LENGTH_LONG).show();
                    }

                }else{
                    Toast.makeText(c, "Failed to create account. Please refer to our Terms of Service for registration guidelines, including minimum age.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
