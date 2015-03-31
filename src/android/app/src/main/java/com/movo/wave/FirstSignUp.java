package com.movo.wave;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    EditText username;
    EditText pass;
    Firebase loginRef;
    Context c;
    String TAG = "Movo.FirstLogin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = this.getApplicationContext();
        setContentView(R.layout.activity_first_sign_up);
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
                                Firebase currentUserRef = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/"+authData.getUid());
                                myData.setCurrentUserRef(currentUserRef);
                                myData.addCurUserTolist();
//                                myData.addCurUserTolist();

                                Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
//                                updateHomePage();
                                Intent intent = new Intent(getApplicationContext(),
                                        Home.class);
                                startActivity(intent);

                            }

                            @Override
                            public void onAuthenticationError(FirebaseError firebaseError) {
                                Log.d(TAG, "Error authenticating newly created user. This could be an issue. ");

                                Toast.makeText(c, "Could not Authenticate new user", Toast.LENGTH_SHORT);

                            }
                        });
                    }
                    @Override
                    public void onError(FirebaseError firebaseError) {
                        Log.d(TAG,"Error creating user: " + firebaseError.getDetails());

                        Toast.makeText(c, "Could not create new user", Toast.LENGTH_SHORT);
                    }
                });



            }
        });
    }
}
