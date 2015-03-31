package com.movo.wave;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.movo.wave.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;


public class FirstLogin extends Activity {
    Button login;
    Button forgot;
    String mEmail;
    String mPassword;
    EditText username;
    EditText pass;
    Firebase loginRef;
    Context c;
    String TAG = "Movo.FirstLogin";
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




        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mEmail = username.getText().toString();
                mPassword = pass.getText().toString();
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

                        Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
                        Intent intent = new Intent(getApplicationContext(),
                                Home.class);
                        startActivity(intent);

                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError) {
                        System.out.println("Error logging in. ");

                        Toast.makeText(c, "Could not Authenticate user", Toast.LENGTH_SHORT);
                    }
                });

            }
        });





    }
}
