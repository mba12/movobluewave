package com.movo.wave;
/**
 * Created by PhilG on 3/23/2015.
 */
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.firebase.client.Firebase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.movo.wave.comms.BLEAgent;
import com.movo.wave.comms.WaveAgent;
import com.movo.wave.comms.WaveRequest;
import com.movo.wave.util.UTC;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class Home extends ActionBarActivity {
    static Context c;
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
    private static ProgressBar syncProgressBar;
    private static TextView syncText;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    Firebase currentUserRef;
    String[] menuOptions;
    TextView currentUserTV;
    public static String TAG = "Movo Wave V2";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        c = this.getApplicationContext();

        // Setup BLE context
        BLEAgent.open(c);

        mTitle = "Movo Wave";
        //Set up date works for calendar display
        calendar = Calendar.getInstance();
        curDay = calendar.get(Calendar.DAY_OF_MONTH);
        curMonth = calendar.get(Calendar.MONTH);
        curYear = calendar.get(Calendar.YEAR);


        DatabaseHelper mDbHelper = new DatabaseHelper(c);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();



        UserData myData = UserData.getUserData(c);
        gridview= (GridView) findViewById(R.id.gridview);
        final ProgressBar pbBar = (ProgressBar) findViewById(R.id.progressBar);
        syncProgressBar = (ProgressBar) findViewById(R.id.syncProgressBar);
        syncText = (TextView) findViewById(R.id.syncingText);
        //this gets our user steps. We will save the data out and display it
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        boolean userExists = prefs.getBoolean("userExists", false);

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

//        UserData myUserData = UserData.getUserData(c);
        ArrayList<String> users = new ArrayList<String>();
        users = myData.getUserList();
        if(!users.isEmpty()) {
            if(userExists==true) {
                setUpCharts(c);
                TextView currentUserTV = (TextView) findViewById(R.id.nameText);
                currentUserTV.setText(myData.getCurrentUserEmail());
            }else{
                String uid = UserData.getUserData(c).getUIDByEmail(users.get(0));
                UserData.getUserData(c).loadNewUser(uid);
                TextView currentUserTV = (TextView) findViewById(R.id.nameText);
                currentUserTV.setText(myData.getCurrentUserEmail());
                setUpCharts(c);
            }
        }else{

            Intent intent = new Intent(getApplicationContext(),
                    FirstLaunch.class);
            startActivity(intent);



        }

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                TextView tv = (TextView) v.findViewById(R.id.wholeDate);
                tv.getText();
                Intent intent = new Intent(getApplicationContext(),
                        DailyActivity.class);
                Bundle extras = new Bundle();
//                extras.putString(*/
                intent.putExtra("date",tv.getText().toString());
                startActivity(intent);
                // DO something

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

//        upload();
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
        TextView currentUserTV = (TextView) findViewById(R.id.nameText);
        currentUserTV.setText(UserData.getUserData(c).getCurrentUserEmail());
    }


    private void setUpChart(){
        ArrayList<Entry> valsComp1 = new ArrayList<Entry>();
        ArrayList<String> xVals = new ArrayList<String>();

        numberOfDaysTotal = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int difference = numberOfDaysTotal - curDay;
        numberOfDaysLeft = numberOfDaysTotal - difference;

        for(int i=0;i<numberOfDaysLeft;i++){

            //Grab today's data//
            Calendar monthCal = Calendar.getInstance();
            monthCal.set(2015,calendar.get(Calendar.MONTH),i+1,0,0,0);
            long monthRangeStart = monthCal.getTimeInMillis();
            monthCal.set(2015,calendar.get(Calendar.MONTH),i+1,calendar.getActualMaximum(Calendar.HOUR_OF_DAY),calendar.getActualMaximum(Calendar.MINUTE),calendar.getActualMaximum(Calendar.MILLISECOND));
            long monthRangeStop = monthCal.getTimeInMillis();


            UserData myData = UserData.getUserData(c);
            Cursor curSteps = getStepsForDateRange(monthRangeStart, monthRangeStop, myData.getCurUID());

            if(curSteps!=null&&curSteps.getCount()!=0){
                int totalStepsForToday = 0;
                while (curSteps.isAfterLast() == false) {
                    totalStepsForToday+=curSteps.getInt(4);

                    curSteps.moveToNext();
//                    Log.d(TAG, "Counting steps for today: "+totalStepsForToday);
                    //works
                }
                curSteps.close();
                Entry curEntry = new Entry(totalStepsForToday, i);
                valsComp1.add(curEntry);
            }else{
                //no steps for this time period.
                Entry curEntry = new Entry(0, i);
                valsComp1.add(curEntry);

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
//            DataSnapshot data = myData.getUserSnapshot();

            int dayToDisplay = (numberOfDaysLeft - (position));


            TextView day = (TextView) gridView.findViewById(R.id.day);


            if(dayToDisplay == curDay){
                day.setText("Today");
            }else {
                day.setText(dayToDisplay+"");
            }
            TextView steps = (TextView) gridView.findViewById(R.id.steps);

            TextView date = (TextView) gridView.findViewById(R.id.wholeDate);
            Calendar today = Calendar.getInstance();
            today.set(2015,calendar.get(Calendar.MONTH),dayToDisplay,0,0,0);
            String wholeDate = today.getTimeInMillis()+"";
            date.setText(wholeDate);
            //Grab today's data//
            Calendar monthCal = Calendar.getInstance();
            monthCal.set(2015,calendar.get(Calendar.MONTH),dayToDisplay,0,0,0);
            long monthRangeStart = monthCal.getTimeInMillis();
            monthCal.set(2015,calendar.get(Calendar.MONTH),dayToDisplay,calendar.getActualMaximum(Calendar.HOUR_OF_DAY),calendar.getActualMaximum(Calendar.MINUTE),calendar.getActualMaximum(Calendar.MILLISECOND));
            long monthRangeStop = monthCal.getTimeInMillis();

            Cursor curSteps = getStepsForDateRange(monthRangeStart, monthRangeStop, myData.getCurUID() );

            if(curSteps!=null&&curSteps.getCount()!=0){
                int totalStepsForToday = 0;
                while (curSteps.isAfterLast() == false) {
                    totalStepsForToday+=curSteps.getInt(4);

                    curSteps.moveToNext();
//                    Log.d(TAG, "Counting steps for today: "+totalStepsForToday);
                    //works
                }



                steps.setText(totalStepsForToday+"");


            }else{
                steps.setText(0+"");


            }
            curSteps.close();
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

                upload();
//                testMeSql();
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
                FirstLogin.class);
        startActivity(intent);
    }
    public void users(){
        Intent intent = new Intent(getApplicationContext(),
                UserActivity.class);
        startActivity(intent);
    }
    public void logout(){
        UserData mUD = UserData.getUserData(c);
        boolean status = mUD.logoutCurrentUser();
        if(!status){
            Intent intent = new Intent(getApplicationContext(),
                    FirstLaunch.class);
            startActivity(intent);
        }else{
            setContentView(R.layout.activity_home);
        }

    }

    public static void setUpChartsExternalCall(Context c){
        Home h = new Home();
//        h.setUpCharts(c);
    }

    public void setUpCharts(Context c){
//        UserData myData = UserData.getUserData(c);
        gridview= (GridView) findViewById(R.id.gridview);
        final ProgressBar pbBar = (ProgressBar) findViewById(R.id.progressBar);

        gridview.invalidate();
                setUpChart();
                pbBar.setVisibility(View.GONE);
                if (gridview.getVisibility() == View.GONE && chart.getVisibility() == View.GONE) {
                    gridview.setVisibility(View.VISIBLE);
                }

    }

    public void refreshCharts(){
//        currentUserRef
//        gridview.deferNotifyDataSetChanged();
        setUpCharts(c);
        gridview.invalidate();
        chart.invalidate();
    }

    public static void upload(){
        /* NOTE: Just a copy-paste. Relogic later....
         */
        syncProgressBar.setProgress(0);
        syncProgressBar.setVisibility(View.VISIBLE);
        syncText.setVisibility(View.VISIBLE);

        final String  syncUniqueID = UUID.randomUUID().toString();
        final String currentUserId = UserData.getUserData(c).getCurUID();

        final Date start = new Date();
        final String startSyncDate = start.toString();

        final WaveAgent.DataSync.Callback syncCallback = new WaveAgent.DataSync.Callback() {
            @Override
            public void notify( final WaveAgent.DataSync sync,
                                final WaveAgent.DataSync.SyncState state,
                                final boolean status) {
                Log.d(TAG, "Upload notify: " + state + " (" + status + ")" );
            }

            @Override
            public void complete( final WaveAgent.DataSync sync,
                                  final List<WaveRequest.WaveDataPoint> data) {
                DatabaseHelper mDbHelper = new DatabaseHelper(c);
                SQLiteDatabase db = mDbHelper.getWritableDatabase();



                if( data != null ) {

                    final int result = insertPoints( db, syncUniqueID, currentUserId, data, sync.device.device.getAddress());

                    Log.d( TAG, "Database insertion status: " + result );


                    Date stop = new Date();

                    ContentValues syncValues = new ContentValues();
                    syncValues.put(Database.SyncEntry.GUID, syncUniqueID);
                    syncValues.put(Database.SyncEntry.SYNC_START, start.getTime());
                    syncValues.put(Database.SyncEntry.SYNC_END, stop.getTime());
                    syncValues.put(Database.SyncEntry.USER,currentUserId);
                    syncValues.put(Database.SyncEntry.STATUS, 0);
                    long newRowId;
                    newRowId = db.insert(Database.SyncEntry.SYNC_TABLE_NAME,
                            null,
                            syncValues);
                    Log.d(TAG, "Sync database add:\n"+syncValues.toString());

//                    FirebaseCalls fbc = new FirebaseCalls(c);
//                    fbc.uploadSync(syncUniqueID);

                    String selection =  Database.SyncEntry.GUID + "=?";
                    ContentValues valuesRead = new ContentValues();
                    Cursor cur = db.query(
                            Database.SyncEntry.SYNC_TABLE_NAME,  // The table to query
                            new String[] { Database.SyncEntry.GUID, //blob
                                    Database.SyncEntry.SYNC_START, //int
                                    Database.SyncEntry.SYNC_END, //int
                                    Database.SyncEntry.USER, //string
                                    Database.SyncEntry.STATUS }, //bool                          // The columns to return
                            selection,                                // The columns for the WHERE clause
                            new String[] { syncUniqueID },                            // The values for the WHERE clause
                            null,                                     // don't group the rows
                            null,                                     // don't filter by row groups
                            null                                 // The sort order
                    );

                    cur.moveToFirst();
                    //start
                    long itemId = cur.getLong(
                            cur.getColumnIndexOrThrow(Database.SyncEntry.GUID)

                    );
                    //firebase upload sync
//                    UserData myData = UserData.getUserData(c);
                    //sync ref
                    Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +cur.getString(3) + "/sync/"+cur.getString(0));


                    Map<String,Object > syncData = new HashMap<String, Object>();
//                    syncData.put(Database.SyncEntry.GUID, cur.getString(0));
                    syncData.put(Database.SyncEntry.SYNC_START, UTC.isoFormat(Long.parseLong(cur.getString(1))));
                    syncData.put(Database.SyncEntry.SYNC_END, UTC.isoFormat(Long.parseLong(cur.getString(2))));
                    syncData.put(Database.SyncEntry.USER, cur.getString(3));
                    syncData.put(Database.SyncEntry.STATUS, cur.getString(4));

                    Log.d(TAG, "Sync ID is "+cur.getString(0));
                    ref.setValue(syncData);
                    cur.close();
                    //*****************steps***********************//
                    Cursor curSteps = getStepsForSync(syncUniqueID);


                    Map<String, Map<String, Map<String, Object>>> monthMap = new HashMap<String, Map<String, Map<String, Object>>>(); //day<minutes,steps>>
//                    List<Object>[] monthList = new List[];
                    Map<String, Map<String, String>>  minuteMap = new HashMap<String,Map<String, String>>(); //minutes, steps
                    Map<String, Map<String, String>> dayMap = new HashMap<String, Map<String, String>>(); //day<minutes,steps>>

                    Calendar cal = UTC.newCal();

                    ArrayList list = new ArrayList();
                    int date = -1;
                    int oldDate =-1;
                    String username = "";
                    while (curSteps.isAfterLast() == false) {
                        if(Integer.parseInt(curSteps.getString(4))!=0) {
                        username = curSteps.getString(3);


                        long stepTime = Long.parseLong(curSteps.getString(2));
                        Date curDate = new Date(stepTime);

                        cal.set( Calendar.YEAR, 2015);
                        cal.set( Calendar.MONTH, curDate.getMonth());
                        cal.set( Calendar.DATE, curDate.getDate() );
                        cal.set( Calendar.HOUR_OF_DAY, curDate.getHours() );
                        cal.set( Calendar.MINUTE, curDate.getMinutes() );
                        cal.set( Calendar.SECOND, 0 );
                        cal.set( Calendar.MILLISECOND, 0 );
                        String dayMinute = (curDate.getMinutes() + (curDate.getHours() *60))+"";

                        if((date!=curDate.getDate()) &&(date!=-1)){
                            String startTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(1)));
                            String endTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(2)));
                            oldDate = date;
                            Firebase refStep2 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +curSteps.getString(3) + "/steps/"+(curDate.getYear()+1900)+"/"+(curDate.getMonth()+1)+"/"+oldDate).child(curSteps.getString(0)); //to modify child node
                            refStep2.setValue(minuteMap);



                            minuteMap = new HashMap<String,Map<String, String>>(); //minutes, steps
                            Map<String,String > stepData = new HashMap<String, String>();
//                            stepData.put(Database.StepEntry.SYNC_ID, curSteps.getString(0));
                            stepData.put(Database.StepEntry.START, startTime);
                            stepData.put(Database.StepEntry.END, endTime);
//                            stepData.put(Database.StepEntry.USER, curSteps.getString(3));
                            stepData.put(Database.StepEntry.STEPS, curSteps.getString(4));
                            stepData.put(Database.StepEntry.DEVICEID, curSteps.getString(5));


                                minuteMap.put(startTime, stepData);

                            date = curDate.getDate();

                        }else{
                            oldDate = date;
                            String startTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(1)));
                            String endTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(2)));
                            Map<String,String > stepData = new HashMap<String, String>();
                            stepData.put(Database.StepEntry.SYNC_ID, curSteps.getString(0));
                            stepData.put(Database.StepEntry.START, startTime);
                            stepData.put(Database.StepEntry.END, endTime);
//                            stepData.put(Database.StepEntry.USER, curSteps.getString(3));
                            stepData.put(Database.StepEntry.STEPS, curSteps.getString(4));
                            stepData.put(Database.StepEntry.DEVICEID, curSteps.getString(5));

                            if(Integer.parseInt(curSteps.getString(4))!=0) {
                                minuteMap.put(startTime, stepData);
                            }

                            date = curDate.getDate();
                        }
                        }



//                        Firebase refStep2 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +curSteps.getString(3) + "/steps/"+curDate.getYear()+"/"+curDate.getMonth()+"/"+curDate.getDate());
////                            refStep2.updateChildren( minuteMap);
//                        list.add(curDate.getDate()+"",stepData);

//
//

                        curSteps.moveToNext();

                    }
                    try {
                        curSteps.moveToLast();
                        long stepTime = Long.parseLong(curSteps.getString(2));
                        Date curDate = new Date(stepTime);

                        String startTime = UTC.isoFormat(Long.parseLong(curSteps.getString(1)));
                        String endTime = UTC.isoFormat(Long.parseLong(curSteps.getString(2)));


                        cal.set(Calendar.YEAR, 2015);
                        cal.set(Calendar.MONTH, curDate.getMonth());
                        cal.set(Calendar.DATE, curDate.getDate());
                        cal.set(Calendar.HOUR_OF_DAY, curDate.getHours());
                        cal.set(Calendar.MINUTE, curDate.getMinutes());
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        String dayMinute = (curDate.getMinutes() + (curDate.getHours() * 60)) + "";


                        Firebase refStep2 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + curSteps.getString(3) + "/steps/" + (curDate.getYear() + 1900) + "/" + (curDate.getMonth() + 1) + "/" + oldDate).child(curSteps.getString(0));
                        refStep2.setValue(minuteMap);
//                    refStep.setValue(list);
                    }catch(Exception e){
                        e.printStackTrace();
                        Log.d(TAG, "No new entries to upload");
                    }
                    curSteps.close();

//                    Home.setUpCharts(c);
//                    Log.d("TAG", "Found sync id "+syncUniqueID+": "+cur.getString(0));//0 is sync, 1 is Timestamp
                } else {
                    Log.w(TAG, "OH noes! " + sync);
                }


//                setUpCharts(c);
                gridview.invalidate();
                Log.d(TAG, "Upload data complete");
                syncProgressBar.setProgress(0);
                syncProgressBar.setVisibility(View.GONE);
                syncText.setVisibility(View.GONE);
            }
            //            myData.getCurUID()
            @Override
            public void notify( final WaveAgent.DataSync sync, float progress) {
                int intProgress = (int)(progress *100);
                syncProgressBar.setProgress(intProgress);
                Log.d(TAG, "Progress % " + progress * 100 );
            }
        };

        // Look for all wave devices.....
        WaveAgent.scanForWaveDevices(10000, new WaveAgent.WaveScanCallback() {
            {
                final String TAG = "WaveTest";
            }

            @Override
            public void notify(WaveAgent.WaveDevice wave) {
                Log.i(TAG, "Found wave device: " + wave.ble.device.getAddress());
                new WaveAgent.DataSync(wave.ble, syncCallback);
            }

            @Override
            public void onComplete() {

            }
        });

        // Or we can scan for a specific device directly....
        final String address = "C2:4C:53:BB:CD:FC"; //phil new
        //final String address = "ED:09:F5:BB:E9:FF"; //alex brick
        //final String address = "EB:3B:2D:61:17:44"; //alex new
        // final WaveAgent.DataSync sync0 = WaveAgent.DataSync.byAddress( 10000, address, syncCallback );
//        final WaveAgent.DataSync sync1 = WaveAgent.DataSync.bySerial( 10000, "UNKNOWN", syncCallback );

    }


    private static boolean insertPoint( final SQLiteDatabase db,
                                        final String guid,
                                        final String userID,
                                        final WaveRequest.WaveDataPoint point,
                                        final String deviceAddress) {

        long TWO_MINUTES_IN_MILLIS=120000;//millisecs
        long endLong = point.date.getTime();
        endLong = endLong + TWO_MINUTES_IN_MILLIS;

        ContentValues values = new ContentValues();
        values.put(Database.StepEntry.GUID, UUID.randomUUID().toString());
        values.put(Database.StepEntry.STEPS, point.value);
        values.put(Database.StepEntry.START, point.date.getTime());
        values.put(Database.StepEntry.END,endLong);
        values.put(Database.StepEntry.USER,userID);
        values.put(Database.StepEntry.IS_PUSHED, 0);
        values.put(Database.StepEntry.SYNC_ID, guid);
        values.put(Database.StepEntry.DEVICEID, deviceAddress);
//        values.put(Database.StepEntry.WORKOUT_TYPE, point.Mode.);
        //TODO: add workout type

        long newRowId;

            newRowId = db.insert(Database.StepEntry.STEPS_TABLE_NAME,
                    null,
                    values);


            final boolean ret = newRowId >= 0;
            if (ret) {
                Log.d(TAG, "Inserted into database: new row " + newRowId + " guid: " + guid);
                Log.d(TAG, "Inserted data: " + point);
            }
            return ret;

    }


    private static int insertPoints( final SQLiteDatabase db,
                                     final String guid,
                                     final String userID,
                                     Collection<WaveRequest.WaveDataPoint> points,
                                     final String deviceAddress) {
        //http://www.vogella.com/tutorials/AndroidSQLite/article.html


        db.beginTransaction();
        boolean success = false;
        int ret = 0;
        int skippedForZero = 0;
        try {
            for (WaveRequest.WaveDataPoint point : points) {
                if(point.value!=0) {
                if (insertPoint(db, guid, userID, point, deviceAddress)) {
                    ret += 1;
                }
                }else{
                    skippedForZero += 1;

                }
            }
            Log.d(TAG, "Skipped " + skippedForZero + " objects with 0 steps.");
            db.setTransactionSuccessful();
            success = true;
        } finally {
            db.endTransaction();
        }
        return success ? ret : -1;
    }


    public Cursor getStepsForDateRange(long monthRangeStart, long monthRangeStop, String userID){

        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String selectionSteps =  Database.StepEntry.START + " > ? AND "+Database.StepEntry.END + " < ? AND "+Database.StepEntry.USER + " =? ";
        Cursor curSteps = db.query(
                Database.StepEntry.STEPS_TABLE_NAME,  // The table to query
                new String[] { Database.StepEntry.SYNC_ID, //blob
                        Database.StepEntry.START, //int
                        Database.StepEntry.END, //int
                        Database.StepEntry.USER, //string
                        Database.StepEntry.STEPS, //int
                        Database.StepEntry.DEVICEID }, //blob                          // The columns to return
                selectionSteps,                                // The columns for the WHERE clause
                new String[] { monthRangeStart+"", monthRangeStop+"",userID },                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        curSteps.moveToFirst();

        return curSteps;
    }

    public static Cursor getStepsForSync(String syncID){
        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String selectionSteps =  Database.StepEntry.SYNC_ID + "=? AND "+Database.StepEntry.IS_PUSHED +"=?";
        Cursor curSteps = db.query(
                Database.StepEntry.STEPS_TABLE_NAME,  // The table to query
                new String[] { Database.StepEntry.SYNC_ID, //blob
                        Database.StepEntry.START, //int
                        Database.StepEntry.END, //int
                        Database.StepEntry.USER, //string
                        Database.StepEntry.STEPS, //int
                        Database.StepEntry.DEVICEID, //blob
                        Database.StepEntry.GUID}, //blob                          // The columns to return
                selectionSteps,                                // The columns for the WHERE clause
                new String[] { syncID, "0" },                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        curSteps.moveToFirst();
        return curSteps;
    }
}
