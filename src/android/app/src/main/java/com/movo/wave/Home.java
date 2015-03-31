package com.movo.wave;
/**
 * Created by PhilG on 3/23/2015.
 */
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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
import android.widget.ProgressBar;
import android.widget.TextView;


import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;


public class Home extends ActionBarActivity {
    Context c;
    static LineChart chart;
    int curYear;
    int curMonth;
    int curDay;
    int numberOfDaysLeft;
    int numberOfDaysTotal;
    Calendar calendar;
    static GridView gridview;
    boolean toggle = true;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    Firebase currentUserRef;
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

        UserData myData = UserData.getUserData(c);
        gridview= (GridView) findViewById(R.id.gridview);
        final ProgressBar pbBar = (ProgressBar) findViewById(R.id.progressBar);

        //this gets our user steps. We will save the data out and display it
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        boolean userExists = prefs.getBoolean("userExists", false);
        if(userExists) {
            setUpCharts();
        }else{
            //umm.

        Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/");
        ref.authWithPassword("philg@sensorstar.com", "testpassword", new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                //success, save auth data
            UserData myData = UserData.getUserData(c);
                myData.setCurUID(authData.getUid());
                myData.setCurEmail("philg@sensorstar.com");
                myData.setCurPW("testpassword");
                myData.setCurToken(authData.getToken());
                currentUserRef = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/"+authData.getUid());
                myData.setCurrentUserRef(currentUserRef);
                myData.addCurUserTolist();
                Log.d(TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider() + ", Expires:" + authData.getExpires());
                setUpCharts();

            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.d(TAG, "Error logging in " + firebaseError.getDetails());

            }
        });
        }




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



        //**********************Set Up slider menu******************//
        menuOptions = new String[6];
        menuOptions[0] = "Login";
        menuOptions[1] = "Upload Data";
        menuOptions[2] = "Users";
        menuOptions[3] = "FAQ";
        menuOptions[4] = "Contact Us";
        menuOptions[5] = "Logout";

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList.setBackgroundResource(R.drawable.splash);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(Home.this,
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

        Log.d(TAG, "Cur user data: "+myData.getCurUID());


    }


    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        calendar = Calendar.getInstance();
        curDay = calendar.get(Calendar.DAY_OF_MONTH);
        curMonth = calendar.get(Calendar.MONTH);
        curYear = calendar.get(Calendar.YEAR);
        try {
            gridview.setAdapter(new GridViewCalendar(Home.this));
            setUpChart();
        }catch(Exception e){
            e.printStackTrace();
        }
        gridview.invalidate();
        chart.invalidate();
    }


    private void setUpChart(){
        ArrayList<Entry> valsComp1 = new ArrayList<Entry>();
        ArrayList<String> xVals = new ArrayList<String>();

        UserData myData = UserData.getUserData(c);
        DataSnapshot snapdata = myData.getUserSnapshot();
        DataSnapshot monthlyData = snapdata.child(calendar.get(Calendar.YEAR)+"/"+(calendar.get(Calendar.MONTH)+1));

        numberOfDaysTotal = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int difference = numberOfDaysTotal - curDay;
        numberOfDaysLeft = numberOfDaysTotal - difference;

        for(int i=0;i<numberOfDaysLeft;i++){
            for(DataSnapshot child : monthlyData.getChildren()){
                float x = Float.parseFloat(child.getKey());
                int y = Integer.parseInt(child.getValue().toString());
                if(x == (i+1)) {
                    Log.d(TAG, "X value is: " + x + " Y value is: " + y);
                    Entry curEntry = new Entry(y, i);
                    valsComp1.add(curEntry);
                }

            }
            xVals.add((i+1)+"");
        }



        LineDataSet setComp1 = new LineDataSet(valsComp1, "Steps taken per day");

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(setComp1);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.invalidate(); // refresh
    }



    public class GridViewCalendar extends BaseAdapter {
        private Context mContext;

        public GridViewCalendar(Context c) {
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

            UserData myData = UserData.getUserData(c);
//            Firebase fb = myData.getCurrentUserRef()
            DataSnapshot data = myData.getUserSnapshot();

            int dayToDisplay = (numberOfDaysLeft - (position));


            TextView day = (TextView) gridView.findViewById(R.id.day);
            if(dayToDisplay == curDay){
                day.setText("Today");
            }else {
                day.setText(dayToDisplay+"");
            }
            TextView steps = (TextView) gridView.findViewById(R.id.steps);
            if(data!=null){
                try {
                    //String what = calendar.get(Calendar.YEAR)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+dayToDisplay;
                    DataSnapshot todaysData = data.child(calendar.get(Calendar.YEAR)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+dayToDisplay);
                    if(!todaysData.getValue().equals("")){
                        steps.setText(todaysData.getValue()+"");
                    }else{
                        steps.setText("0");
                    }
                    steps.setText(todaysData.getValue()+"");
                }catch(Exception e){
                    steps.setText("0");
//                    e.printStackTrace();
                }
            }else{
                steps.setText("1234");

            }

            return gridView;
        }


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
                UserData myData = UserData.getUserData(c);
                Log.d(TAG, "Cur user data: "+myData.getCurUID());

//                login();
                break;
            case 2:
                Log.d(TAG, "Users pressed");
                users();

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
                logout();
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
    public void users(){
        Intent intent = new Intent(getApplicationContext(),
                UserActivity.class);
        startActivity(intent);
    }
    public void logout(){
        UserData mUD = UserData.getUserData(c);
        mUD.storeCurrentUser();
    }

    public void setUpCharts(){
        UserData myData = UserData.getUserData(c);
        gridview= (GridView) findViewById(R.id.gridview);
        final ProgressBar pbBar = (ProgressBar) findViewById(R.id.progressBar);
        currentUserRef = myData.getCurrentUserRef();
        currentUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, snapshot.getValue() + "");
                UserData myData = UserData.getUserData(c);
                myData.setUserSnapshot(snapshot);
                gridview.setAdapter(new GridViewCalendar(Home.this));
                gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v, int position1, long id) {
                        // name = getResources().getResourceEntryName(mThumbIds[position1]);
                        //do stuff here on grid click
                        UserData myData = UserData.getUserData(c);
//                        calendar.get(Calendar.YEAR)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+curDay
//                        Log.d(TAG, "Transaction function start: "+myData.getCurUID()+"/"+calendar.get(Calendar.YEAR)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+curDay);
                        //This function increments steps taken today when clicking on the gridview. This will sync with server as soon as it can.
                        Firebase ref3 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + myData.getCurUID() + "/" + calendar.get(Calendar.YEAR) + "/" + (calendar.get(Calendar.MONTH) + 1) + "/" + curDay);
                        ref3.runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData currentData) {
                                if (currentData.getValue() == null) {
                                    currentData.setValue(1);
                                } else {
                                    currentData.setValue((Long) currentData.getValue() + 1);
                                }
                                return Transaction.success(currentData); //we can also abort by calling Transaction.abort()
                            }

                            @Override
                            public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot currentData) {
                                //This method will be called once with the results of the transaction.
                                Log.d(TAG, "Transaction increment successful");
                                gridview.invalidate();
                                chart.invalidate();
                            }
                        });


                    }
                });
                gridview.invalidate();
                setUpChart();
                pbBar.setVisibility(View.GONE);
                if (gridview.getVisibility() == View.GONE && chart.getVisibility() == View.GONE) {
                    gridview.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d(TAG, "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    public static void refreshCharts(){
//        currentUserRef
//        gridview.deferNotifyDataSetChanged();
//        setUpCharts();
        gridview.invalidate();
        chart.invalidate();
    }


}