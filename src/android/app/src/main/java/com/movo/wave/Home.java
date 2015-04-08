package com.movo.wave;
/**
 * Created by PhilG on 3/23/2015.
 */
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;

import com.firebase.client.Firebase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class Home extends ActionBarActivity {
    Context c;
    LineChart chart;
    int curYear;
    int curMonth;
    int curDay;
    int numberOfDaysLeft;
    int numberOfDaysTotal;
    Calendar calendar;
    boolean toggle = true;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    private WaveManager mWaveManager;
    String[] menuOptions;
    public String TAG = "Movo Wave V2";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        c = this.getApplicationContext();
        mTitle = "Movo Wave";
        //Set up date works for calendar display
        calendar = Calendar.getInstance();
        curDay = calendar.get(Calendar.DAY_OF_MONTH);
        curMonth = calendar.get(Calendar.MONTH);
        curYear = calendar.get(Calendar.YEAR);

        //mWaveManager = new WaveManager( c );
        //mWaveManager.scan( null );
        BLEAgent.open( c );
        BLEAgent.handle( new BLEAgent.BLERequestScan( 10000 ) {

            @Override
            public boolean filter(BLEAgent.BLEDevice device) {
                return device.device.getAddress().equals( "ED:09:F5:BB:E9:FF" );
            }

            @Override
            public void onComplete(BLEAgent.BLEDevice device) {

                Log.d( "CALLBACK", "found target " + device );
                BLEAgent.handle( new BLEAgent.BLERequest( device, 30000 ) {
                    @Override
                    public boolean dispatch() {
                        Log.d( "ED:09:F5:BB:E9:FF", "Oh hi!");
                        return true;
                    }

                    @Override
                    public Set<Pair<UUID, UUID>> listenUUIDs() {
                        Pair<UUID, UUID> service =
                                new Pair<UUID, UUID> (WaveManager.notifyServiceUUID,
                                        WaveManager.notifyCharacteristicUUID );
                        Set<Pair<UUID,UUID>> ret = new HashSet<Pair<UUID, UUID>>();
                        ret.add( service );
                        return ret;
                    }
                });
            }
        });
        BLEAgent.handle( new BLEAgent.BLERequestScan( 100000 ) {
            @Override
            public boolean filter(BLEAgent.BLEDevice device) {
                return device.device.getAddress().equals( "ED:09:F5:BB:E9:FF" );
            }

            @Override
            public void onComplete(BLEAgent.BLEDevice device) {

                Log.d( "CALLBACK2", "found target " + device );
            }
        });

        //calendar display
        final GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(Home.this));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position1, long id) {
               // name = getResources().getResourceEntryName(mThumbIds[position1]);
                //do stuff here on grid click
            }
        });


        chart = (LineChart) findViewById(R.id.chart);

        ImageView chartToggle = (ImageView) findViewById(R.id.chartButton);
//        chartToggle.setOnClickListener();
        chartToggle.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
                    if(toggle){
                        gridview.setVisibility(View.INVISIBLE);
                        chart.setVisibility(View.VISIBLE);
                        toggle = false;
                    }else{
                        gridview.setVisibility(View.VISIBLE);
                        chart.setVisibility(View.INVISIBLE);
                        toggle = true;
                    }

				}
			});
        setUpChart();


        Firebase.setAndroidContext(this);
        Firebase fb = new Firebase("https://ss-movo-wave-v2.firebaseio.com/");
        fb.child("test").setValue("This is a test of how text uploads");


        menuOptions = new String[5];
        menuOptions[0] = "Login";
        menuOptions[1] = "Upload Data";
        menuOptions[2] = "FAQ";
        menuOptions[3] = "Contact Us";
        menuOptions[4] = "Logout";

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList.setBackgroundResource(R.drawable.splash);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(Home.this,
                R.layout.drawer_list_item, menuOptions));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        Toolbar mToolbar = (Toolbar)findViewById(R.id.toolbar);
//        setSupportActionBar(mToolbar);



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


        //We'll use this to handle passing data around the application.
        UserData myData = UserData.getUserData();
        Log.d(TAG, "Cur user data: "+myData.getCurUID());


    }



    private void setUpChart(){
        //chart.invalidate();
        //this call will refresh the chart
        ArrayList<Entry> valsComp1 = new ArrayList<Entry>();
        ArrayList<Entry> valsComp2 = new ArrayList<Entry>();

        Entry c1e1 = new Entry(100.000f, 0); // 0 == quarter 1
        valsComp1.add(c1e1);
        Entry c1e2 = new Entry(50.000f, 1); // 1 == quarter 2 ...
        valsComp1.add(c1e2);
        // and so on ...

        Entry c2e1 = new Entry(120.000f, 0); // 0 == quarter 1
        valsComp2.add(c2e1);
        Entry c2e2 = new Entry(110.000f, 1); // 1 == quarter 2 ...
        valsComp2.add(c2e2);

        LineDataSet setComp1 = new LineDataSet(valsComp1, "Company 1");
        LineDataSet setComp2 = new LineDataSet(valsComp2, "Company 2");

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(setComp1);
        dataSets.add(setComp2);

        ArrayList<String> xVals = new ArrayList<String>();
        xVals.add("1.Q"); xVals.add("2.Q"); xVals.add("3.Q"); xVals.add("4.Q");

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.invalidate(); // refresh
    }



    public class ImageAdapter extends BaseAdapter {
        private Context mContext;

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            //This gets total days in month, we want days past
            //numberOfDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            numberOfDaysTotal = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            int difference = numberOfDaysTotal - curDay;
            numberOfDaysLeft = numberOfDaysTotal - difference;
            return numberOfDaysLeft;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View gridView;

            if (convertView == null) {

                gridView = new View(c);

                // get layout from mobile.xml
                gridView = inflater.inflate(R.layout.home_calendar_cell, null);





            } else {
                gridView = (View) convertView;
                //System.out.println("View not null, loading postion "+position+" out of "+mThumbIds.length);
            }

            int dayToDisplay = (numberOfDaysLeft - (position));


            TextView day = (TextView) gridView.findViewById(R.id.day);
            if(dayToDisplay == curDay){
                day.setText("Today");
            }else {
                day.setText(dayToDisplay+"");
            }
            TextView steps = (TextView) gridView.findViewById(R.id.steps);
            steps.setText("1234");

            return gridView;
        }

        // references to our images

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
                login();
                break;
            case 1:
                Log.d(TAG, "Upload pressed");
                UserData myData = UserData.getUserData();
                Log.d(TAG, "Cur user data: "+myData.getCurUID());
//                login();
                break;
            case 2:
                Log.d(TAG, "FAQ pressed");
//                logout();
                break;
            case 3:
                Log.d(TAG, "Contact pressed");
//                match();
                break;

            case 4:
                Log.d(TAG, "Logout pressed");
                break;
        }
    }
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }
//





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_home, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }





    public void login(){
        Intent intent = new Intent(getApplicationContext(),
                LoginActivity.class);
        startActivity(intent);
    }
}
