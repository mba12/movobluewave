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

public class DiscoverWaveActivity extends MenuActivity {
    ArrayList<String> waves;
    ListView waveList;

    public String TAG = "Movo Discover Wave";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMenu(R.layout.activity_discover_wave);
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
    }
}