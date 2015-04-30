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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.RelativeLayout;
import android.widget.TextView;


import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.movo.wave.comms.BLEAgent;
import com.movo.wave.comms.WaveAgent;
import com.movo.wave.comms.WaveRequest;
import com.movo.wave.util.Calculator;
import com.movo.wave.util.UTC;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


public class Home extends MenuActivity {
    static LineChart chart;
    RelativeLayout chartView;
    int curYear;
    int curMonth;
    int curDay;
    int numberOfDaysLeft;
    int numberOfDaysTotal;
    Calendar calendar;
    static GridView gridview;
    boolean toggle = true;
    private static ProgressBar syncProgressBar;
    private static TextView syncText;
    private CharSequence mTitle;
    Firebase currentUserRef;
    TextView currentUserTV;
    RelativeLayout older;
    RelativeLayout newer;
    TextView curMonthDisplay;


    public static String TAG = "Movo Wave V2";


    private long timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMenu(R.layout.activity_home);
        ImageView profilePic = (ImageView) findViewById(R.id.profilePic);
        // Setup BLE context
        BLEAgent.open(c);

        mTitle = "Movo Wave";
        //Set up date works for calendar display
        Intent intentIncoming = getIntent();

            String date = intentIncoming.getStringExtra("date");
            if(date!=null) {
                timestamp = Long.parseLong(date);
                calendar = Calendar.getInstance();
                calendar.setTimeInMillis(timestamp);
                if((calendar.get(Calendar.MONTH))!=(Calendar.getInstance().get(Calendar.MONTH))){
//                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
//                    curDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                }
            }else{
                calendar = Calendar.getInstance();
                timestamp = calendar.getTimeInMillis();
                curDay = calendar.get(Calendar.DAY_OF_MONTH);
            }
//        curDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);



        curMonth = calendar.get(Calendar.MONTH);
        curYear = calendar.get(Calendar.YEAR);

        older = (RelativeLayout) findViewById(R.id.previous);
        newer = (RelativeLayout) findViewById(R.id.next);
        if(calendar.get(Calendar.MONTH)==Calendar.getInstance().get(Calendar.MONTH)){
            newer.setVisibility(View.GONE);

        }

        curMonthDisplay = (TextView) findViewById(R.id.tvCurMonth);
        String month_name = calendar.getDisplayName(calendar.MONTH,Calendar.SHORT, Locale.US);
        curMonthDisplay.setText(month_name+"");




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
        chartView = (RelativeLayout) findViewById(R.id.chartView);
        ImageView chartToggle = (ImageView) findViewById(R.id.chartButton);
//        chartToggle.setOnClickListener();
        chartToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(toggle){
                    gridview.setVisibility(View.INVISIBLE);
                    chartView.setVisibility(View.VISIBLE);
                    toggle = false;
                }else{
                    gridview.setVisibility(View.VISIBLE);
                    chartView.setVisibility(View.INVISIBLE);
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

        older.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ProgressBar pbBar = (ProgressBar) findViewById(R.id.progressBar);
                pbBar.setVisibility(View.VISIBLE);


                Intent intent = new Intent(getApplicationContext(),
                        Home.class);
                Bundle extras = new Bundle();
                Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(timestamp);
                newCal.add(Calendar.MONTH, -1);
                final Calendar editedCal = newCal;

                long monthForwardMillis = newCal.getTimeInMillis();
                String lastMonth = (monthForwardMillis)+"";
                intent.putExtra("date",lastMonth);
                final Intent editedIntent = intent;
                UserData myData = UserData.getUserData(c);
                Firebase ref =  new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +myData.getCurUID() + "/steps/"+newCal.get(Calendar.YEAR) + "/" + newCal.get(Calendar.MONTH));
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        System.out.println(snapshot.getValue());
//                        loginProgress.setVisibility(View.INVISIBLE);

                        insertSteps(snapshot,editedCal.get(Calendar.YEAR),editedCal.get(Calendar.MONTH),c);

                                Log.d(TAG, "Inserting steps into database");


                        startActivity(editedIntent);
                        finish();
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("The read failed: " + firebaseError.getMessage());
                    }
                });




            }
        });
        newer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ProgressBar pbBar = (ProgressBar) findViewById(R.id.progressBar);
                pbBar.setVisibility(View.VISIBLE);

                Intent intent = new Intent(getApplicationContext(),
                        Home.class);
                Bundle extras = new Bundle();
                Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(timestamp);
                newCal.add(Calendar.MONTH, +1);
                final Calendar editedCal = newCal;

                long monthForwardMillis = newCal.getTimeInMillis();
                String lastMonth = (monthForwardMillis) + "";
                intent.putExtra("date", lastMonth);
                final Intent editedIntent = intent;
                UserData myData = UserData.getUserData(c);
                Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + myData.getCurUID() + "/steps/" + newCal.get(Calendar.YEAR) + "/" + newCal.get(Calendar.MONTH)+"/");
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        System.out.println(snapshot.getValue());
//                        loginProgress.setVisibility(View.INVISIBLE);

                        insertSteps(snapshot, editedCal.get(Calendar.YEAR), editedCal.get(Calendar.MONTH), c);

                        Log.d(TAG, "Inserting steps into database");


                        startActivity(editedIntent);
                        finish();
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("The read failed: " + firebaseError.getMessage());
                    }
                });
            }
        });

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                TextView tv = (TextView) v.findViewById(R.id.wholeDate);
                String dateStr = tv.getText().toString();
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date(Long.parseLong(dateStr)));
//                cal.set(Calendar.MONTH,(cal.get(Calendar.MONTH)));
                long intentTime = cal.getTimeInMillis();
                Intent intent = new Intent(getApplicationContext(),
                        DailyActivity.class);
                Bundle extras = new Bundle();
//                extras.putString(*/
                intent.putExtra("date",intentTime+"");
                startActivity(intent);
                // DO something

            }
        });

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myProfile();
            }
        });

        Log.d(TAG, "Cur user data: "+myData.getCurUID());


        boolean upload = intentIncoming.getBooleanExtra("Upload",false);
        if(upload) {
            upload();
        }

        try{
            Bitmap prof = myData.getCurUserPhoto();
            if(prof!=null){
                profilePic.setImageBitmap(prof);
            }
        }catch(Exception e){
            e.printStackTrace();
        }


//        upload();
    }


    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
//        calendar = Calendar.getInstance();
//        curDay = calendar.get(Calendar.DAY_OF_MONTH);
//        curMonth = calendar.get(Calendar.MONTH);
//        curYear = calendar.get(Calendar.YEAR);
        Intent intentIncoming = getIntent();
//
        String date = intentIncoming.getStringExtra("date");
        if(date!=null) {
            Long dateLong = Long.parseLong(date);
            calendar = Calendar.getInstance();
            calendar.setTime(new Date(dateLong));
            if((calendar.get(Calendar.MONTH))!=(Calendar.getInstance().get(Calendar.MONTH))){
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                curDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            }else{
                curDay = calendar.get(Calendar.DAY_OF_MONTH);
            }
        }else {
//
        }
        try {
            if(UserData.getUserData(c).getCurUID()!=null) {
                gridview.setAdapter(new GridViewCalendar(Home.this));
                setUpChart();
                gridview.invalidate();
                chart.invalidate();
                TextView currentUserTV = (TextView) findViewById(R.id.nameText);
                currentUserTV.setText(UserData.getUserData(c).getCurrentUserEmail());
            }
        }catch(Exception e){
            e.printStackTrace();
        }


    }


    private void setUpChart(){
        ArrayList<Entry> valsComp1 = new ArrayList<Entry>();
        ArrayList<String> xVals = new ArrayList<String>();

        numberOfDaysTotal = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int difference = numberOfDaysTotal - curDay;
        numberOfDaysLeft = numberOfDaysTotal - difference;
        int totalStepsForMonth= 0;
        for(int i=0;i<numberOfDaysLeft;i++){

            //Grab today's data by setting i to day, then adding the hours/mins/secs for the rest of the day and grabbing all steps in the range as a sum
            Calendar monthCal = calendar;
            monthCal.setTimeInMillis(timestamp);
            monthCal.set(monthCal.get(Calendar.YEAR),monthCal.get(Calendar.MONTH),i,0,0,0);
            long monthRangeStart = monthCal.getTimeInMillis();
            monthCal.set(monthCal.get(Calendar.YEAR),monthCal.get(Calendar.MONTH),i,monthCal.getActualMaximum(Calendar.HOUR_OF_DAY),monthCal.getActualMaximum(Calendar.MINUTE),monthCal.getActualMaximum(monthCal.MILLISECOND));
            long monthRangeStop = monthCal.getTimeInMillis();


            UserData myData = UserData.getUserData(c);
            Cursor curSteps = getStepsForDateRange(monthRangeStart, monthRangeStop, myData.getCurUID());

            if(curSteps!=null&&curSteps.getCount()!=0){
                int totalStepsForToday = 0;
                while (curSteps.isAfterLast() == false) {
                    totalStepsForToday+=curSteps.getInt(4);//step count


                    curSteps.moveToNext();
//                    Log.d(TAG, "Counting steps for today: "+totalStepsForToday);
                    //works
                }
                totalStepsForMonth+=totalStepsForToday;
                curSteps.close();
                Entry curEntry = new Entry(totalStepsForToday, i);
                valsComp1.add(curEntry);
            }else{
                //no steps for this time period.
                Entry curEntry = new Entry(0, i);
                valsComp1.add(curEntry);

            }



            //add +1 for the 0 based day compensation.
            xVals.add((i+1)+"");
        }
        Calendar monthCal = calendar;
        monthCal.setTimeInMillis(timestamp);

        TextView stepsTotal = (TextView) findViewById(R.id.stepTotal);
        TextView stepsAve = (TextView) findViewById(R.id.stepAverage);
        TextView milesTotal = (TextView) findViewById(R.id.distanceTotal);
        TextView milesAve = (TextView) findViewById(R.id.distanceAverage);
        TextView calsTotal = (TextView) findViewById(R.id.caloriesTotal);
        TextView calsAve = (TextView) findViewById(R.id.caloriesAverage);
        double stepsAverageDouble = calculateAverageSteps(totalStepsForMonth, numberOfDaysLeft );
        stepsAve.setText(String.format("%.1f", stepsAverageDouble));
        double milesTotalDouble = calculateTotalMiles(totalStepsForMonth);
        milesTotal.setText(String.format("%.1f", milesTotalDouble));
        double milesAverageDouble = calculateAverageMiles(totalStepsForMonth, numberOfDaysLeft );
        milesAve.setText(String.format("%.1f",milesAverageDouble));
        double caloriesDouble = calculateTotalCalories(totalStepsForMonth);
        calsTotal.setText(String.format("%.1f",caloriesDouble));
        double caloriesAverage = calculateAverageCalories(totalStepsForMonth, numberOfDaysLeft);
        calsAve.setText(String.format("%.1f",caloriesAverage));

            stepsTotal.setText(totalStepsForMonth+"");
//        }


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
            calendar.setTimeInMillis(timestamp);
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

            calendar.setTimeInMillis(timestamp);
            if(dayToDisplay == curDay){
                if(calendar.get(Calendar.MONTH)==Calendar.getInstance().get(Calendar.MONTH)){
                    day.setText("Today");
                }else{
                    day.setText(dayToDisplay+"");
                }

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
            ImageView background = (ImageView) gridView.findViewById(R.id.cellBackground);
            Bitmap bm = null;
            try {
                bm = dailyBitmapFetch(monthRangeStart);
            }catch(Exception e){
                e.printStackTrace();
            }
            if(bm!=null){
                background.setImageBitmap(bm);
            }
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



    public void myProfile(){
        Intent intent = new Intent(getApplicationContext(),
                MyProfile.class);
        startActivity(intent);

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

    public void discover() {
        Intent intent = new Intent( getApplicationContext(), DiscoverWaveActivity.class);
        startActivity( intent );
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

    public void upload(){
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
                            Firebase refStep2 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +curSteps.getString(3) + "/steps/"+(curDate.getYear()+1900)+"/"+(curDate.getMonth())+"/"+oldDate).child(curSteps.getString(0)); //to modify child node
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
                        Toast.makeText(c, "No new steps to add.",Toast.LENGTH_SHORT);
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


    public static int insertPoints( final SQLiteDatabase db,
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

    public  Cursor getStepsForSync(String syncID){
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        //now getIntent() should always return the last received intent
    }

    private static void insertSteps(DataSnapshot snapshot, int year, int month, Context c) {
        UserData myData = UserData.getUserData(c);
        Iterable<DataSnapshot> children = snapshot.getChildren();
        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        for (DataSnapshot child : children){
            String date = child.getKey();
            Iterable<DataSnapshot> syncEvents = child.getChildren();
            for(DataSnapshot syncsForToday : syncEvents){
                String syncName = syncsForToday.getKey();
                Iterable<DataSnapshot> stepEvents = syncsForToday.getChildren();
                for(DataSnapshot stepChunk : stepEvents){
                    String stepTime = stepChunk.getKey();
                    Iterable<DataSnapshot> step = syncsForToday.getChildren();
                    Object stepEvent = stepChunk.getValue();
                    Map<String, String> monthMap = new HashMap<String, String>(); //day<minutes,steps>>
                    monthMap = (Map<String, String>) stepChunk.getValue();
                    Log.d(TAG, "Monthmap test"+monthMap);
                    Calendar thisCal = Calendar.getInstance();
//                    Date curDate = monthMap.get("starttime").toString();
                    String dateConcatStart = year + "-" +month+ "-" +date+ "" +monthMap.get("starttime").toString();
                    String dateConcatStop = year + "-" +month+ "-" +date+ "" +monthMap.get("endtime").toString();


                    try {
                        Date curDateStart = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateConcatStart);
                        Date curDateStop = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateConcatStop);
//                        Log.d("TAG", "date is "+curDate);
                        thisCal.setTime(curDateStart);

                        ContentValues values = new ContentValues();
                        values.put(Database.StepEntry.GUID, UUID.randomUUID().toString());
                        values.put(Database.StepEntry.STEPS, Integer.parseInt(monthMap.get("count").toString()));
                        values.put(Database.StepEntry.START,curDateStart.getTime());
                        values.put(Database.StepEntry.END,curDateStop.getTime());
                        values.put(Database.StepEntry.USER,myData.getCurUID());
                        values.put(Database.StepEntry.IS_PUSHED, 1); //this is downloaded from the cloud, it obviously has been pushed.
                        values.put(Database.StepEntry.SYNC_ID, monthMap.get("syncid"));
                        values.put(Database.StepEntry.DEVICEID, monthMap.get("deviceid"));
                        //        values.put(Database.StepEntry.WORKOUT_TYPE, point.Mode.);
                        //TODO: add workout type

                        long newRowId;

                        newRowId = db.insert(Database.StepEntry.STEPS_TABLE_NAME,
                                null,
                                values);
                        Log.d(TAG, "Database insert result: "+newRowId+" for: "+values);



                    }catch(Exception e){
                        e.printStackTrace();;
                    }

                }

            }

        }

        db.close();

    }


    public static Date trim(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.clear(); // as per BalusC comment.
        cal.setTime( date );
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    public Bitmap dailyBitmapFetch(long today){
        boolean localFile = false;
//        today = trim(today);
        Date currentDay = new Date(today);
        currentDay = trim(currentDay);
        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        UserData myData = UserData.getUserData(c);
        String user = myData.getCurUID();
        String photo =  Database.PhotoStore.DATE + " =? AND "+Database.PhotoStore.USER + " =?";
        Cursor curPhoto = db.query(
                Database.PhotoStore.PHOTO_TABLE_NAME,  // The table to query
                new String[] {
                        Database.StepEntry.USER, //string
                        Database.PhotoStore.DATE, //int
                        Database.PhotoStore.PHOTOBLOB }, //blob                          // The columns to return
                photo,                                // The columns for the WHERE clause
                new String[] { currentDay.getTime()+"", user },                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        curPhoto.moveToFirst();
        localFile = false;
        int uniquePic;
        if(curPhoto.getCount()!=0){
            localFile = true;
            byte[] byteArray = curPhoto.getBlob(2);
//                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
            uniquePic = byteArray.length;
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = 10;
            Bitmap bm = BitmapFactory.decodeByteArray(byteArray, 0 ,byteArray.length, options);
            Log.d(TAG, "Found photo for today");
            return bm;
//            background.setImageBitmap(bm);
//                setContentView(R.layout.activity_daily);
        }else{
            return null;
        }
    }

    public static double calculateAverageSteps(int steps, int daysInMonth){
        double ave=0;
        if(daysInMonth!=0){
            ave = steps/daysInMonth;
        }
        return ave;
    }

    public static double calculateTotalMiles(int steps){
        Calculator calc = new Calculator();

//                double caloriesUsed = calc.calculate_calories(stepsTaken, 72, 170, "Male", 1987, 24);
//        double caloriesUsed = calc.simple_calculate_calories(stepsTaken);

//                    calculate_calories(int steps, int height, int weight, String gender, int birthYear, int minutes) {
//        calories.setText(String.format("%.1f CAL", caloriesUsed));

        double milesTraveled = calc.calculate_distance(steps, 72);
        return milesTraveled;
    }
    public static double calculateTotalCalories(int steps){
        Calculator calc = new Calculator();

                double caloriesUsed = calc.simple_calculate_calories(steps);
            return caloriesUsed;


    }

    public static double calculateAverageMiles(int steps, int daysInMonth){
        double milesTraveled = 0;
        Calculator calc = new Calculator();


        if(daysInMonth!=0){
            milesTraveled = calc.calculate_distance(steps, 72);
            milesTraveled = milesTraveled / daysInMonth;
        }

//                double caloriesUsed = calc.calculate_calories(stepsTaken, 72, 170, "Male", 1987, 24);
//        double caloriesUsed = calc.simple_calculate_calories(stepsTaken);

//                    calculate_calories(int steps, int height, int weight, String gender, int birthYear, int minutes) {
//        calories.setText(String.format("%.1f CAL", caloriesUsed));


            return milesTraveled;

    }

    public static double calculateAverageCalories(int steps, int daysInMonth){
        double caloriesUsed = 0;
        Calculator calc = new Calculator();


        if(daysInMonth!=0){
             caloriesUsed = calc.simple_calculate_calories(steps);
            caloriesUsed = caloriesUsed / daysInMonth;
        }

//


        return caloriesUsed;
    }

}
