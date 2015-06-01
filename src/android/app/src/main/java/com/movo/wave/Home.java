package com.movo.wave;
/**
 * Created by PhilG on 3/23/2015.
 */
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
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


import com.bumptech.glide.Glide;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.movo.wave.comms.BLEAgent;
import com.movo.wave.util.Calculator;
import com.movo.wave.util.DataUtilities;
import com.movo.wave.util.NotificationPublisher;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    final static String EXTRA_CHART_VIEW = "com.movo.wave.home.EXTRA_CHART_VIEW";
    boolean chartVisible;
    private static ProgressBar syncProgressBar;
    private static TextView syncText;
    private CharSequence mTitle;
    Firebase currentUserRef;
    TextView currentUserTV;
    RelativeLayout older;
    RelativeLayout newer;
    TextView curMonthDisplay;

    public enum ChartType {
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

    protected void setChartVisible( boolean visible ) {
        if(visible){
            gridview.setVisibility(View.INVISIBLE);
            chartView.setVisibility(View.VISIBLE);
        }else{
            gridview.setVisibility(View.VISIBLE);
            chartView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
        db = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intentIncoming = getIntent();

        LaunchAnimation.apply(this, intentIncoming);



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

        chartVisible = intentIncoming.getBooleanExtra( EXTRA_CHART_VIEW, false );

        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        db = mDbHelper.getReadableDatabase();

        mTitle = "Movo Wave";
        //Set up date works for calendar display

        String date = intentIncoming.getStringExtra("date");
        if (date != null) {
            timestamp = Long.parseLong(date);
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp);
            if ((calendar.get(Calendar.MONTH)) != (Calendar.getInstance().get(Calendar.MONTH))) {
//                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
//                    curDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            }else{
                calendar.set(Calendar.DATE,Calendar.getInstance().get(Calendar.DATE));
                timestamp = calendar.getTimeInMillis();
            }
//            UserData myData = UserData.getUserData(c);
            String monthChange = "";
            String yearChange = "";


            if(calendar.get(Calendar.MONTH)<11){
                monthChange = "0"+(calendar.get(Calendar.MONTH)+1);
            }else{
                monthChange = String.valueOf(calendar.get(Calendar.MONTH)+1);
            }
            yearChange = ""+ calendar.get(Calendar.YEAR);
            Firebase ref = new Firebase(UserData.firebase_url + "users/" +  UserData.getUserData(c).getCurUID() + "/steps/" + yearChange + "/" + monthChange);
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Log.d(TAG, "" + snapshot.getValue());
//                        loginProgress.setVisibility(View.INVISIBLE);

                    insertSteps(snapshot, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), c);

                    Log.d(TAG, "Inserting steps into database");



                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    Log.d(TAG, "The read failed: " + firebaseError.getMessage());
                }
            });
        } else {
            calendar = Calendar.getInstance();
            timestamp = calendar.getTimeInMillis();
            curDay = calendar.get(Calendar.DAY_OF_MONTH);
        }
//        curDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);


        curMonth = calendar.get(Calendar.MONTH);
        curYear = calendar.get(Calendar.YEAR);

        older = (RelativeLayout) findViewById(R.id.previous);
        newer = (RelativeLayout) findViewById(R.id.next);
        if (calendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {
            newer.setVisibility(View.GONE);

        } else {
            newer.setVisibility(View.VISIBLE);
        }

        curMonthDisplay = (TextView) findViewById(R.id.tvCurMonth);
        String month_name = calendar.getDisplayName(calendar.MONTH, Calendar.LONG, Locale.US);
        curMonthDisplay.setText((month_name + "").toUpperCase());


        ContentValues values = new ContentValues();


//        UserData myData = UserData.getUserData(c);


        gridview = (GridView) findViewById(R.id.gridview);
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
                chartVisible = ! chartVisible;
                setChartVisible( chartVisible );
            }
        });
        setChartVisible( chartVisible );


//        UserData myUserData = UserData.getUserData(c);
        ArrayList<String> users = new ArrayList<String>();
        users =  UserData.getUserData(c).getUserList();
        if (!users.isEmpty()) {
//            if (userExists == true) {
//                setUpCharts(c);
//                TextView currentUserTV = (TextView) findViewById(R.id.nameText);
//                currentUserTV.setText(myData.getCurrentUsername());
//            } else {
            String uid = UserData.getUserData(c).getUIDByEmail(users.get(0));
            UserData.getUserData(c).loadNewUser(uid);
            TextView currentUserTV = (TextView) findViewById(R.id.nameText);
            if(UserData.getUserData(c).getCurrentUsername().equals("Error")){
                currentUserTV.setText( "" );
            }else{
                currentUserTV.setText( UserData.getUserData(c).getCurrentUsername());
            }


            setUpCharts(c);
//            }


        } else {

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
                LaunchAnimation.SLIDE_RIGHT.setIntent(intent);
                intent.putExtra( EXTRA_CHART_VIEW, chartVisible );
                final Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(timestamp);
                newCal.set(Calendar.DATE, 1);
                newCal.add(Calendar.MONTH, -1);

                long monthForwardMillis = newCal.getTimeInMillis();
                String lastMonth = (monthForwardMillis) + "";
                intent.putExtra("date", lastMonth);
//                UserData myData = UserData.getUserData(c);
                startActivity(intent);
                finish();

            }
        });
        newer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ProgressBar pbBar = (ProgressBar) findViewById(R.id.progressBar);
                pbBar.setVisibility(View.VISIBLE);

                final Intent intent = new Intent(getApplicationContext(),
                        Home.class);
                LaunchAnimation.SLIDE_LEFT.setIntent( intent );
                intent.putExtra( EXTRA_CHART_VIEW, chartVisible );
                final Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(timestamp);
                newCal.set(Calendar.DATE, 1);
                newCal.add(Calendar.MONTH, +1);

                long monthForwardMillis = newCal.getTimeInMillis();
                String lastMonth = (monthForwardMillis) + "";
                intent.putExtra("date", lastMonth);
                startActivity(intent);
                finish();
//                UserData myData = UserData.getUserData(c);
//                Firebase ref = new Firebase(firebase_url + "/users/" + myData.getCurUID() + "/steps/" + newCal.get(Calendar.YEAR) + "/" + newCal.get(Calendar.MONTH) + "/");
//                ref.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot snapshot) {
//                        Log.d(TAG, "" + snapshot.getValue());
////                        loginProgress.setVisibility(View.INVISIBLE);
//
//                        insertSteps(snapshot, newCal.get(Calendar.YEAR), newCal.get(Calendar.MONTH), c);
//
//                        Log.d(TAG, "Inserting steps into database");
//
//
//
//                    }
//
//                    @Override
//                    public void onCancelled(FirebaseError firebaseError) {
//                        Log.d(TAG, "" + "The read failed: " + firebaseError.getMessage());
//                    }
//                });
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

        Log.d(TAG, "Cur user data: " +  UserData.getUserData(c).getCurUID());

        try {
            Bitmap prof =  UserData.getUserData(c).getCurUserPhoto();
            if (prof != null) {
                profilePic.setImageBitmap(prof);
            }
        } catch (Exception e) {
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

//scheduleSyncReminders();


//        upload();
    }
    public void scheduleSyncReminders(){
        long oneDay = TimeUnit.DAYS.toMillis(1);     // 1 day to milliseconds.

        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(),"Sync your Wave to find out how far you've come"), 5000,0 );
        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(),"Don't forget to sync and update your Movo calendar."), 10000,1 );
        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(),"Where have your steps taken you? Sync your Wave now"), 15000,2 );
        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(),"You will lose data if you do not sync at least once a week."), 20000,3 );
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this)
//                        .setSmallIcon(R.drawable.app_icon)
//                        .setContentTitle("Sync a fool")
//                        .setContentText("Don't forget to sync! kthnx");
//        int mNotificationId = 002;
//// Gets an instance of the NotificationManager service
//        NotificationManager mNotifyMgr =
//                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//// Builds the notification and issues it.
//        mNotifyMgr.notify(mNotificationId, mBuilder.build());


    }
    private void scheduleNotification(Notification notification, int delay, int id) {

        Intent notificationIntent = new Intent(this, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, id);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    private Notification getNotification(String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(title);
        builder.setContentText("Please Sync");
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        builder.setSmallIcon(R.drawable.app_icon);
        return builder.build();
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
        if (date != null) {
            Long dateLong = Long.parseLong(date);
            calendar = Calendar.getInstance();
            calendar.setTime(new Date(dateLong));
            if ((calendar.get(Calendar.MONTH)) != (Calendar.getInstance().get(Calendar.MONTH))) {
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                curDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            } else {
                calendar = Calendar.getInstance();
                curDay = calendar.get(Calendar.DAY_OF_MONTH);
            }
        } else {
//
        }
        try {
            if (UserData.getUserData(c).getCurUID() != null ) {
                gridview.setAdapter(new GridViewCalendar(Home.this));
                setUpChart();
                gridview.invalidate();
                chart.invalidate();
                TextView currentUserTV = (TextView) findViewById(R.id.nameText);
                if(UserData.getUserData(c).getCurrentUsername().equals("Error")){
                    currentUserTV.setText( "" );
                }else{
                    currentUserTV.setText( UserData.getUserData(c).getCurrentUsername());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private void setUpChart() {
        ArrayList<Entry> valsComp1 = new ArrayList<Entry>();
        ArrayList<String> xVals = new ArrayList<String>();

        numberOfDaysTotal = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int difference = numberOfDaysTotal - curDay;
        numberOfDaysLeft = numberOfDaysTotal - difference;
        int totalStepsForMonth = 0;
        int greatestSteps = 0;
        for (int i = 0; i < numberOfDaysLeft; i++) {

            //Grab today's data by setting i to day, then adding the hours/mins/secs for the rest of the day and grabbing all steps in the range as a sum
            Calendar monthCal = calendar;
            monthCal.setTimeZone( TimeZone.getTimeZone("UTC"));
            monthCal.setTimeInMillis(timestamp);
            monthCal.set(monthCal.get(Calendar.YEAR), monthCal.get(Calendar.MONTH), i, 0, 0, 0);
            long monthRangeStart = monthCal.getTimeInMillis();
            monthCal.set(monthCal.get(Calendar.YEAR), monthCal.get(Calendar.MONTH), i, monthCal.getActualMaximum(Calendar.HOUR_OF_DAY), monthCal.getActualMaximum(Calendar.MINUTE), monthCal.getActualMaximum(monthCal.MILLISECOND));
            long monthRangeStop = monthCal.getTimeInMillis();


//            UserData myData = UserData.getUserData(c);
            Cursor curSteps = getStepsForDateRange(monthRangeStart, monthRangeStop,  UserData.getUserData(c).getCurUID());

            if (curSteps != null && curSteps.getCount() != 0) {
                int totalStepsForToday = 0;
                while (curSteps.isAfterLast() == false) {
                    totalStepsForToday += curSteps.getInt(4);//step count


                    curSteps.moveToNext();
//                    Log.d(TAG, "Counting steps for today: "+totalStepsForToday);
                    //works
                }
                if (totalStepsForToday > greatestSteps) {
                    greatestSteps = totalStepsForToday;
                }
                totalStepsForMonth += totalStepsForToday;
                curSteps.close();
                Entry curEntry;
                if (curChart.equals(ChartType.STEPS)) {
                    curEntry = new Entry(totalStepsForToday, i);
                } else if (curChart.equals(ChartType.CALORIES)) {
                    float cals = (int) calculateTotalCalories(totalStepsForToday);
                    curEntry = new Entry(cals, i);
                } else {
                    float miles = (int) calculateTotalMiles(totalStepsForToday);
                    curEntry = new Entry(miles, i);
                }
                valsComp1.add(curEntry);
            } else {
                //no steps for this time period.
                Entry curEntry = new Entry(0, i);
                valsComp1.add(curEntry);

            }


            //add +1 for the 0 based day compensation.
            xVals.add((i + 1) + "");
        }
        Calendar monthCal = calendar;
        monthCal.setTimeInMillis(timestamp);

        TextView stepsTotal = (TextView) findViewById(R.id.stepTotal);
        TextView stepsAve = (TextView) findViewById(R.id.stepAverage);
        TextView milesTotal = (TextView) findViewById(R.id.distanceTotal);
        TextView milesAve = (TextView) findViewById(R.id.distanceAverage);
        TextView calsTotal = (TextView) findViewById(R.id.caloriesTotal);
        TextView calsAve = (TextView) findViewById(R.id.caloriesAverage);
        double stepsAverageDouble = calculateAverageSteps(totalStepsForMonth, numberOfDaysLeft);
        stepsAve.setText(String.format("%.1f", stepsAverageDouble));
        double milesTotalDouble = calculateTotalMiles(totalStepsForMonth);
        milesTotal.setText(String.format("%.1f", milesTotalDouble));
        double milesAverageDouble = calculateAverageMiles(totalStepsForMonth, numberOfDaysLeft);
        milesAve.setText(String.format("%.1f", milesAverageDouble));
        double caloriesDouble = calculateTotalCalories(totalStepsForMonth);
        calsTotal.setText(String.format("%.1f", caloriesDouble));
        double caloriesAverage = calculateAverageCalories(totalStepsForMonth, numberOfDaysLeft);
        calsAve.setText(String.format("%.1f", caloriesAverage));

        stepsTotal.setText(totalStepsForMonth + "");
//        }


        LineDataSet setComp1 = new LineDataSet(valsComp1, "Steps taken per day");
        setComp1.setColor(getResources().getColor(R.color.red));


        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(setComp1);

        LineData data = new LineData(xVals, dataSets);
        YAxis rightAxis = chart.getAxisRight();
        YAxis leftAxis = chart.getAxisLeft();
        if (curChart.equals(ChartType.STEPS)) {

            rightAxis.setStartAtZero(true);
            rightAxis.setAxisMaxValue((float) greatestSteps);
            leftAxis.setStartAtZero(true);
            leftAxis.setAxisMaxValue((float) greatestSteps);

        } else if (curChart.equals(ChartType.CALORIES)) {

            rightAxis.setStartAtZero(true);
            rightAxis.setAxisMaxValue((float) calculateTotalCalories(greatestSteps));
            leftAxis.setStartAtZero(true);
            leftAxis.setAxisMaxValue((float) calculateTotalCalories(greatestSteps));
        } else {

            rightAxis.setStartAtZero(true);
            rightAxis.setAxisMaxValue((float) calculateTotalMiles(greatestSteps));
            leftAxis.setStartAtZero(true);
            leftAxis.setAxisMaxValue((float) calculateTotalMiles(greatestSteps));
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
            View gridView;
            LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);



            if (convertView == null) {

                gridView = new View(c);

                // get layout from mobile.xml
                gridView = inflater.inflate(R.layout.home_calendar_cell, null);


            } else {
                gridView = (View) convertView;
                //Log.d(TAG,""+"View not null, loading postion "+position+" out of "+mThumbIds.length);
            }

//            UserData myData = UserData.getUserData(c);
//            Firebase fb = myData.getCurrentUserRef()
//            DataSnapshot data = myData.getUserSnapshot();

            int dayToDisplay = (numberOfDaysLeft - (position));


            TextView day = (TextView) gridView.findViewById(R.id.day);

            calendar.setTimeInMillis(timestamp);
            if (dayToDisplay == curDay) {
                if (calendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {
                    day.setText("Today");
                } else {
                    day.setText(dayToDisplay + "");
                }

            } else {
                day.setText(dayToDisplay + "");
            }
            TextView steps = (TextView) gridView.findViewById(R.id.steps);


            TextView date = (TextView) gridView.findViewById(R.id.wholeDate);
            Calendar today = Calendar.getInstance();

            today.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), dayToDisplay, 0, 0, 0);
            String wholeDate = today.getTimeInMillis() + "";
            date.setText(wholeDate);
            //Grab today's data//
            Calendar monthCal = Calendar.getInstance();


            monthCal.setTimeInMillis(today.getTimeInMillis());
//            monthCal.setTimeZone( TimeZone.getTimeZone( "UTC" ));


            long monthRangeStart = monthCal.getTimeInMillis();
//            monthCal.set(monthCal.get(Calendar.YEAR), monthCal.get(Calendar.MONTH), dayToDisplay, monthCal.getActualMaximum(Calendar.HOUR_OF_DAY), monthCal.getActualMaximum(Calendar.MINUTE), monthCal.getActualMaximum(Calendar.SECOND));
            monthCal.add(Calendar.DATE, 1);
            long monthRangeStop = monthCal.getTimeInMillis();
            ImageView background = (ImageView) gridView.findViewById(R.id.cellBackground);
            Bitmap bm = null;
            try {
                byte[] photo = dailyPhotoFetch(monthRangeStart);

                if (photo != null) {
                    Glide.with(c)
                            .load(photo)
//                            .override(1080,1920)
                            .thumbnail(0.1f)
                            .centerCrop()
                            .into(background);
//                    background.setImageBitmap(bm);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Cursor curSteps = getStepsForDateRange(monthRangeStart, monthRangeStop,  UserData.getUserData(c).getCurUID());
            curSteps.moveToFirst();
            if (curSteps != null && curSteps.getCount() != 0) {
                int totalStepsForToday = 0;
                while (curSteps.isAfterLast() == false) {
                    totalStepsForToday += curSteps.getInt(4);

                    curSteps.moveToNext();
//                    Log.d(TAG, "Counting steps for today: "+totalStepsForToday);
                    //works
                }


                steps.setText(totalStepsForToday + "");


            } else {
                steps.setText(0 + "");


            }
            curSteps.close();
            return gridView;
        }


    }


    public void myProfile() {
        Intent intent = new Intent(getApplicationContext(),
                MyProfile.class);
        startActivity(intent);

    }

    public void login() {
        Intent intent = new Intent(getApplicationContext(),
                FirstLogin.class);
        startActivity(intent);
    }

    public void users() {
        Intent intent = new Intent(getApplicationContext(),
                UserActivity.class);
        startActivity(intent);
    }

    public void logout() {
//        UserData mUD = UserData.getUserData(c);
        boolean status =  UserData.getUserData(c).logoutCurrentUser();
        if (!status) {
            Intent intent = new Intent(getApplicationContext(),
                    FirstLaunch.class);
            startActivity(intent);
        } else {
            setContentView(R.layout.activity_home);
        }

    }

    public void discover() {
        Intent intent = new Intent(getApplicationContext(), WaveScanActivity.class);
        startActivity(intent);
    }

    public static void setUpChartsExternalCall(Context c) {
        Home h = new Home();
//        h.setUpCharts(c);
    }

    public void setUpCharts(Context c) {
//        UserData myData = UserData.getUserData(c);
        gridview = (GridView) findViewById(R.id.gridview);
        final ProgressBar pbBar = (ProgressBar) findViewById(R.id.progressBar);

        gridview.invalidate();
        setUpChart();
        pbBar.setVisibility(View.GONE);
        if (gridview.getVisibility() == View.GONE && chart.getVisibility() == View.GONE) {
            if( chartVisible) {
                chart.setVisibility( View.VISIBLE );
            } else {
                gridview.setVisibility(View.VISIBLE);
            }
        }

    }

    public void refreshCharts() {
//        currentUserRef
//        gridview.deferNotifyDataSetChanged();
        setUpCharts(c);
        gridview.invalidate();
        chart.invalidate();
    }


    public Cursor getStepsForDateRange(long monthRangeStart, long monthRangeStop, String userID) {

        String selectionSteps = Database.StepEntry.START + " >= ? AND " + Database.StepEntry.END + " <= ? AND " + Database.StepEntry.USER + " =? ";
        Cursor curSteps = db.query(
                Database.StepEntry.STEPS_TABLE_NAME,  // The table to query
                new String[]{Database.StepEntry.SYNC_ID, //blob
                        Database.StepEntry.START, //int
                        Database.StepEntry.END, //int
                        Database.StepEntry.USER, //string
                        Database.StepEntry.STEPS, //int
                        Database.StepEntry.DEVICEID}, //blob                          // The columns to return
                selectionSteps,                                // The columns for the WHERE clause
                new String[]{monthRangeStart + "", monthRangeStop + "", userID},                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        curSteps.moveToFirst();

        return curSteps;
    }

    public Cursor getStepsForSync(String syncID) {
        String selectionSteps = Database.StepEntry.SYNC_ID + "=? AND " + Database.StepEntry.IS_PUSHED + "=?";
        Cursor curSteps = db.query(
                Database.StepEntry.STEPS_TABLE_NAME,  // The table to query
                new String[]{Database.StepEntry.SYNC_ID, //blob
                        Database.StepEntry.START, //int
                        Database.StepEntry.END, //int
                        Database.StepEntry.USER, //string
                        Database.StepEntry.STEPS, //int
                        Database.StepEntry.DEVICEID, //blob
                        Database.StepEntry.GUID}, //blob                          // The columns to return
                selectionSteps,                                // The columns for the WHERE clause
                new String[]{syncID, "0"},                            // The values for the WHERE clause
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
        String monthChange = "";
        String yearChange = "";


        if(month<11){
            monthChange = "0"+(month+1);
        }else{
            monthChange = String.valueOf(month+1);
        }
        yearChange = ""+ year;

        UserData.getUserData(c).insertStepsFromDB(snapshot, c, monthChange, yearChange);
    }





//    public Bitmap dailyBitmapFetch(long today) {
//        boolean localFile = false;
////        today = trim(today);
//        Date currentDay = new Date(today);
//        Bitmap returnBM=null;
//        currentDay = trim(currentDay);
////        UserData myData = UserData.getUserData(c);
//        String user =  UserData.getUserData(c).getCurUID();
//        String photo = Database.PhotoStore.DATE + " =? AND " + Database.PhotoStore.USER + " =?";
//        Cursor curPhoto = db.query(
//                Database.PhotoStore.PHOTO_TABLE_NAME,  // The table to query
//                new String[]{
//                        Database.StepEntry.USER, //string
//                        Database.PhotoStore.DATE, //int
//                        Database.PhotoStore.PHOTOBLOB}, //blob                          // The columns to return
//                photo,                                // The columns for the WHERE clause
//                new String[]{currentDay.getTime() + "", user},                            // The values for the WHERE clause
//                null,                                     // don't group the rows
//                null,                                     // don't filter by row groups
//                null                                 // The sort order
//        );
//        try{
//            curPhoto.moveToFirst();
//            localFile = false;
////        localFile = true;
////        awefawef
////        String wholePhoto = "";
////        byte[] byteArray;
////        if(curPhoto.getCount()>1){
////            while (curPhoto.isAfterLast() == false) {
////                wholePhoto += curPhoto.getBlob(2);
////                curPhoto.moveToNext();
////            }
////            byteArray = wholePhoto.getBytes();
////            curPhoto.close();
////        }else{
////            byteArray = curPhoto.getBlob(2);
////            curPhoto.close();
////        }
//            int uniquePic;
//            if (curPhoto.getCount() != 0) {
//                localFile = true;
//                byte[] byteArray = curPhoto.getBlob(2);
////                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
//                uniquePic = byteArray.length;
//                final BitmapFactory.Options options = new BitmapFactory.Options();
////            options.inJustDecodeBounds = false;
////            options.inSampleSize = 10;
//                Bitmap bm = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
//                Log.d(TAG, "Found photo for today");
//                if (bm != null) {
//                    returnBM = bm;
//                } else {
//                    return null;
//                }
//
////            background.setImageBitmap(bm);
////                setContentView(R.layout.activity_daily);
//            } else {
////            return null;
//
//                Log.d(TAG, "Loading image from firebase");
//                final Calendar monthCal = Calendar.getInstance();
//                monthCal.setTimeInMillis(today);
//                String monthChange = "";
//                String dayChange = "";
//                if ((monthCal.get(Calendar.MONTH)) < 11) {
//                    monthChange = "0" + (monthCal.get(Calendar.MONTH) + 1);
//                } else {
//                    monthChange = String.valueOf(monthCal.get(Calendar.MONTH) + 1);
//                }
//                if ((monthCal.get(Calendar.DATE)) < 10) {
//                    dayChange = "0" + (monthCal.get(Calendar.DATE));
//                } else {
//                    dayChange = String.valueOf(monthCal.get(Calendar.DATE));
//                }
//                Log.d(TAG, "Loading image from firebase");
//                Firebase ref = new Firebase(UserData.firebase_url + "users/" + user + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthChange + "/" + dayChange);
//                ref.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot snapshot) {
//
//                        // TODO: Discuss changes below with Phil -- comment from Michael
//
//                        Object obj = snapshot.getValue();
//                        if (obj == null) {
//                            Log.d(TAG, "MBA: Value from FB is null.");
//                            return;
//                        }
//
//                        Log.d(TAG, snapshot.getValue().toString());
//                        if (snapshot.getChildrenCount() == 2) {
//                            final BitmapFactory.Options options = new BitmapFactory.Options();
////                        options.inJustDecodeBounds = false;
////                        options.inSampleSize = 4;
//                            ArrayList<String> result = ((ArrayList<String>) snapshot.getValue());
//
////                            Object pictureObject = result.get(0);
//                            Object pictureObject = result.get(1);
//                            String pictureString = String.valueOf(pictureObject);
//
//                            String className = pictureObject.getClass().getName();
//                            Log.d(TAG, "MBA object from arraylist is: " + className);
//                            Log.d(TAG, "MBA object value: " + pictureString);
//
//                            try {
//                                // byte[] decodedString = Base64.decode(result.get(0), Base64.DEFAULT);
//                                byte[] decodedString = Base64.decode(pictureString, Base64.DEFAULT);
//                                DatabaseHelper mDbHelper = new DatabaseHelper(c);
//                                SQLiteDatabase db = mDbHelper.getWritableDatabase();
////
//
//
//                                //file doesn't exist on local device
//                                Date curDay = trim(new Date(monthCal.getTimeInMillis()));
//                                ContentValues syncValues = new ContentValues();
//                                syncValues.put(Database.PhotoStore.DATE, curDay.getTime());
//                                syncValues.put(Database.PhotoStore.USER, UserData.getUserData(c).getCurUID());
//                                syncValues.put(Database.PhotoStore.PHOTOBLOB, decodedString);
//                                long newRowId;
//                                newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
//                                        null,
//                                        syncValues);
//                                Log.d(TAG, "Photo database add from firebase: " + newRowId);
//                                db.close();
//
//                            }catch(Exception e){
//                                e.printStackTrace();
//                            }
//
////
//
//                        } else if(snapshot.getChildrenCount()>=2){
//                            Log.d(TAG, "Photo multipart");
//                            //multipart file upload
//                            DatabaseHelper mDbHelper = new DatabaseHelper(c);
//                            SQLiteDatabase db = mDbHelper.getWritableDatabase();
//                            final BitmapFactory.Options options = new BitmapFactory.Options();
////                        options.inJustDecodeBounds = false;
////                        options.inSampleSize = 4;
//                            ArrayList<String> result = ((ArrayList<String>) snapshot.getValue());
//                            try {
//                                String wholeString = "";
//                                for(int i =1;i<result.size();i++){
//                                    wholeString += result.get(i);
//
//
//                                }
//                                byte[] decodedString = Base64.decode(wholeString, Base64.DEFAULT);
//                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, options);
////                                background.setImageBitmap(decodedByte);
////                                background.setScaleType(ImageView.ScaleType.FIT_CENTER);
//                                Date curDay = trim(new Date(monthCal.getTimeInMillis()));
//                                ContentValues syncValues = new ContentValues();
//                                syncValues.put(Database.PhotoStore.DATE, curDay.getTime());
//                                syncValues.put(Database.PhotoStore.USER, UserData.getUserData(c).getCurUID());
//                                syncValues.put(Database.PhotoStore.PHOTOBLOB, decodedString);
//                                long newRowId;
//                                newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
//                                        null,
//                                        syncValues);
//                                Log.d(TAG, "Photo database add from firebase: " + newRowId);
//                                db.close();
//
//
//                            }catch(Exception e){
//                                e.printStackTrace();
//                            }
//
//
//
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(FirebaseError firebaseError) {
//                        System.out.println("The read failed: " + firebaseError.getMessage());
//                    }
//                });
//
//
//            }
//        }finally{
//            curPhoto.close();
//        }
//        return returnBM;
//    }




    public byte[] dailyPhotoFetch(long today) {
        return UserData.getUserData(c).retrievePhoto(today);

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
