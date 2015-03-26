package com.movo.wave;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.github.mikephil.charting.charts.LineChart;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by PhilG on 3/23/2015.
 */

public class UserActivity extends ActionBarActivity {
    Context c;
    ArrayList<String> users;
    ListView userList;
    public String TAG = "Movo Wave User Select";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        c = this.getApplicationContext();
        userList = (ListView) findViewById(R.id.userList);

        users = new ArrayList<String>();
//        users.add("User 1");
//        users.add("User 2");

        UserData myUserData = UserData.getUserData(c);
        users = myUserData.getUserList();

        // Create The Adapter with passing ArrayList as 3rd parameter
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<String>(this,R.layout.drawer_list_item, users);
        // Set The Adapter
        userList.setAdapter(arrayAdapter);

        userList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // argument position gives the index of item which is clicked
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                // TODO Auto-generated method stub

            }
        });



    }

}