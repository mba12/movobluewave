package com.movo.wave;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by PhilG on 3/23/2015.
 */

public class UserActivity extends MenuActivity {
    ArrayList<String> users;
    ListView userList;

    public String TAG = "Movo Wave User Select";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMenu(R.layout.activity_user);
        userList = (ListView) findViewById(R.id.userList);

        users = new ArrayList<String>();
//        users.add("User 1");
//        users.add("User 2");

//        UserData myUserData = UserData.getUserData(c);
        users =  UserData.getUserData(c).getUserList();

        // Create The Adapter with passing ArrayList as 3rd parameter
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<String>(this, R.layout.drawer_list_item, users);
        // Set The Adapter
        userList.setAdapter(arrayAdapter);

        userList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // argument position gives the index of item which is clicked
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                // TODO Auto-generated method stub
//                Toast.makeText(c, users.get(position)+"", Toast.LENGTH_SHORT).show();
//                UserData.getUserData(c).
                String uid = UserData.getUserData(c).getUIDByEmail(users.get(position));
                UserData.getUserData(c).setCurrentUser(uid);
                UserData.getUserData(c).loadNewUser(uid);
                //startActivity( new Intent( c, Home.class ) );
                Intent intent = new Intent(getApplicationContext(),
                        Home.class);
                startActivity(intent);
                finish();
            }
        });


        Button addUser = (Button) findViewById(R.id.addUserButton);
        addUser.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(),
                        FirstLogin.class);
                startActivity(intent);

                finish();


            }
        });
    }

    private void updateHomePage() {

        new Thread() {
            public void run() {


                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Home.setUpChartsExternalCall(c);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();;
                }

            }
        }.start();
    }

}