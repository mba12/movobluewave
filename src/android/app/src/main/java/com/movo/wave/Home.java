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
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
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

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


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
    public enum ChartType{
        STEPS,
        MILES,
        CALORIES
    }
    ChartType curChart = ChartType.STEPS;
    RelativeLayout stepsLayout;
    TextView stepsText;
    RelativeLayout milesLayout;
    TextView milesText;
    RelativeLayout caloriesLayout;
    TextView caloriesText;


    public static String TAG = "Movo Wave V2";


    private long timestamp;

    SQLiteDatabase db;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
        db = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/gotham-book.otf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
        initMenu(R.layout.activity_home);
        ImageView profilePic = (ImageView) findViewById(R.id.profilePic);
        stepsLayout = (RelativeLayout) findViewById(R.id.stepsLayout);
        milesLayout = (RelativeLayout) findViewById(R.id.milesLayout);
        caloriesLayout = (RelativeLayout) findViewById(R.id.caloriesLayout);
        stepsText = (TextView) findViewById(R.id.titleBlockA);
        milesText = (TextView) findViewById(R.id.titleBlockB);
        caloriesText = (TextView) findViewById(R.id.titleBlockC);
        // Setup BLE context
        BLEAgent.open(c);


        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        db = mDbHelper.getReadableDatabase();

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

        }else{
            newer.setVisibility(View.VISIBLE);
        }

        curMonthDisplay = (TextView) findViewById(R.id.tvCurMonth);
        String month_name = calendar.getDisplayName(calendar.MONTH,Calendar.LONG, Locale.US);
        curMonthDisplay.setText((month_name+"").toUpperCase());


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
                currentUserTV.setText(myData.getCurrentUsername());
            }else{
                String uid = UserData.getUserData(c).getUIDByEmail(users.get(0));
                UserData.getUserData(c).loadNewUser(uid);
                TextView currentUserTV = (TextView) findViewById(R.id.nameText);
                currentUserTV.setText(myData.getCurrentUsername());
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


                final Intent intent = new Intent(getApplicationContext(),
                        Home.class);
                Bundle extras = new Bundle();
                final Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(timestamp);
                newCal.set(Calendar.DATE, 1);
                newCal.add(Calendar.MONTH, -1);

                long monthForwardMillis = newCal.getTimeInMillis();
                String lastMonth = (monthForwardMillis)+"";
                intent.putExtra("date",lastMonth);
                UserData myData = UserData.getUserData(c);
                Firebase ref =  new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +myData.getCurUID() + "/steps/"+newCal.get(Calendar.YEAR) + "/" + newCal.get(Calendar.MONTH));
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        System.out.println(snapshot.getValue());
//                        loginProgress.setVisibility(View.INVISIBLE);

                        insertSteps(snapshot,newCal.get(Calendar.YEAR),newCal.get(Calendar.MONTH),c);

                        Log.d(TAG, "Inserting steps into database");


                        startActivity(intent);
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

                final Intent intent = new Intent(getApplicationContext(),
                        Home.class);
                Bundle extras = new Bundle();
                final Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(timestamp);
                newCal.set(Calendar.DATE, 1);
                newCal.add(Calendar.MONTH, +1);

                long monthForwardMillis = newCal.getTimeInMillis();
                String lastMonth = (monthForwardMillis) + "";
                intent.putExtra("date", lastMonth);
                UserData myData = UserData.getUserData(c);
                Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + myData.getCurUID() + "/steps/" + newCal.get(Calendar.YEAR) + "/" + newCal.get(Calendar.MONTH)+"/");
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        System.out.println(snapshot.getValue());
//                        loginProgress.setVisibility(View.INVISIBLE);

                        insertSteps(snapshot, newCal.get(Calendar.YEAR), newCal.get(Calendar.MONTH), c);

                        Log.d(TAG, "Inserting steps into database");


                        startActivity(intent);
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
                intent.putExtra("date", intentTime + "");
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

        Log.d(TAG, "Cur user data: " + myData.getCurUID());

        try{
            Bitmap prof = myData.getCurUserPhoto();
            if(prof!=null){
                profilePic.setImageBitmap(prof);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        stepsLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                curChart = ChartType.STEPS;
                milesText.setBackground(getResources().getDrawable(R.drawable.tabbarselected));
                caloriesText.setBackground(getResources().getDrawable(R.drawable.tabbarselected));
                stepsText.setBackground(getResources().getDrawable(R.drawable.redbtn_lg));
//                redbtn_lg
                refreshCharts();
            }
        });

        milesLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                curChart = ChartType.MILES;
                milesText.setBackground(getResources().getDrawable(R.drawable.redbtn_lg));
                caloriesText.setBackground(getResources().getDrawable(R.drawable.tabbarselected));
                stepsText.setBackground(getResources().getDrawable(R.drawable.tabbarselected));
                refreshCharts();
            }
        });
        caloriesLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                curChart = ChartType.CALORIES;
                milesText.setBackground(getResources().getDrawable(R.drawable.tabbarselected));
                caloriesText.setBackground(getResources().getDrawable(R.drawable.redbtn_lg));
                stepsText.setBackground(getResources().getDrawable(R.drawable.tabbarselected));
                refreshCharts();
            }
        });

//        upload();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
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
                currentUserTV.setText(UserData.getUserData(c).getCurrentUsername());
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
        int greatestSteps=0;
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
                if(totalStepsForToday>greatestSteps){
                    greatestSteps = totalStepsForToday;
                }
                totalStepsForMonth+=totalStepsForToday;
                curSteps.close();
                Entry curEntry;
                if(curChart.equals(ChartType.STEPS)) {
                    curEntry  = new Entry(totalStepsForToday, i);
                }else if(curChart.equals(ChartType.CALORIES)){
                    float cals = (int)calculateTotalCalories(totalStepsForToday);
                    curEntry = new Entry(cals, i);
                }else{
                    float miles = (int)calculateTotalMiles(totalStepsForToday);
                    curEntry = new Entry(miles, i);
                }
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
        setComp1.setColor(getResources().getColor(R.color.red));


        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(setComp1);

        LineData data = new LineData(xVals, dataSets);
        YAxis rightAxis = chart.getAxisRight();
        YAxis leftAxis = chart.getAxisLeft();
        if(curChart.equals(ChartType.STEPS)) {

            rightAxis.setStartAtZero(true);
            rightAxis.setAxisMaxValue((float) greatestSteps);
            leftAxis.setStartAtZero(true);
            leftAxis.setAxisMaxValue((float) greatestSteps);

        }else if(curChart.equals(ChartType.CALORIES)){

            rightAxis.setStartAtZero(true);
            rightAxis.setAxisMaxValue((float)calculateTotalCalories(greatestSteps));
            leftAxis.setStartAtZero(true);
            leftAxis.setAxisMaxValue((float)calculateTotalCalories(greatestSteps));
        }else{

            rightAxis.setStartAtZero(true);
            rightAxis.setAxisMaxValue((float)calculateTotalMiles(greatestSteps));
            leftAxis.setStartAtZero(true);
            leftAxis.setAxisMaxValue((float)calculateTotalMiles(greatestSteps));
        }
        rightAxis.setDrawLabels(false);
        leftAxis.setDrawLabels(true);
//        chart.getPaint(Chart.PAINT_HOLE).setColor(Color.RED);
        chart.setBackgroundColor(getResources().getColor(R.color.white));
        chart.setBorderColor(getResources().getColor(R.color.grey));
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
        Intent intent = new Intent( getApplicationContext(), WaveScanActivity.class);
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





    public Cursor getStepsForDateRange(long monthRangeStart, long monthRangeStop, String userID){

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

    private void insertSteps(DataSnapshot snapshot, int year, int month, Context c) {
        UserData myData = UserData.getUserData(c);
        Iterable<DataSnapshot> children = snapshot.getChildren();

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
