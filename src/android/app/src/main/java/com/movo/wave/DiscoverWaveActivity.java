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
 * Created by Alex Haase on 3/23/2015.
 */

public class DiscoverWaveActivity extends ActionBarActivity {
    Context c;
    ArrayList<String> waves;
    ListView waveList;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    String[] menuOptions;

    public String TAG = "Movo Discover Wave";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_wave);
        c = this.getApplicationContext();
        waveList = (ListView) findViewById(R.id.waveList);

        waves = new ArrayList<String>();
        waves.add( "Test1");
        waves.add( "Test2");

        // Create The Adapter with passing ArrayList as 3rd parameter
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<String>(this,R.layout.drawer_list_item, waves);
        // Set The Adapter
        waveList.setAdapter(arrayAdapter);

        waveList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // argument position gives the index of item which is clicked
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                // TODO Auto-generated method stub
//                Toast.makeText(c, waves.get(position)+"", Toast.LENGTH_SHORT).show();
                Log.d( TAG, "Clicked " + position);
                finish();
            }
        });


//**********************Set Up slider menu******************//
        menuOptions = new String[6];
        menuOptions[0] = "Login";
        menuOptions[1] = "Upload Data";
        menuOptions[2] = "waves";
        menuOptions[3] = "FAQ";
        menuOptions[4] = "Contact Us";
        menuOptions[5] = "Logout";

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList.setBackgroundResource(R.drawable.splash);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(DiscoverWaveActivity.this,
                R.layout.drawer_list_item, menuOptions));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        Toolbar mToolbar = (Toolbar)findViewById(R.id.toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,  mDrawerLayout, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
        //*********************************************************//


    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {


        mDrawerList.setItemChecked(position, false);
        mDrawerLayout.closeDrawer(mDrawerList);
        switch (position)
        {
            case 0:
//                create();
                Log.d(TAG, "Login pressed");
//                login();
                break;
            case 1:
                Log.d(TAG, "Upload pressed");
                UserData myData = UserData.getUserData(c);
                Log.d(TAG, "Cur user data: "+myData.getCurUID());

//                login();
                break;
            case 2:
                Log.d(TAG, "waves pressed");
//                waves();

            case 3:
                Log.d(TAG, "FAQ pressed");
//                logout();
                break;
            case 4:
                Log.d(TAG, "Contact pressed");
//                match();
                break;

            case 5:
                Log.d(TAG, "Logout pressed");
//                logout();
                break;
        }
    }
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

}