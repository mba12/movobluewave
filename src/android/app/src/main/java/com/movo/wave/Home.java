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
import android.support.v4.widget.DrawerLayout;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.movo.wave.comms.BLEAgent;
import com.movo.wave.util.Calculator;
import com.movo.wave.util.DataUtilities;
import com.movo.wave.util.NotificationPublisher;
import com.movo.wave.util.UTC;

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
import java.util.LinkedList;
import java.util.List;
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
    final static String USER_CHANGE = "newUser";
    boolean chartVisible;
    boolean userChange;
    private static ProgressBar syncProgressBar;
    private static TextView syncText;
    private CharSequence mTitle;
    Firebase currentUserRef;
    TextView currentUserTV;
    RelativeLayout older;
    RelativeLayout newer;
    public static Home instance;
    DrawerLayout homeLayout;
    TextView curMonthDisplay;
    ImageView profilePic;

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

    UserData.UpdateDelegate delegate;

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

//    public static Home getHome() {
//        if (instance == null) {
//            instance = new Home();
//        }
//        return instance;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intentIncoming = getIntent();
//        instance = getHome();
        LaunchAnimation.apply(this, intentIncoming);

        delegate = trackDelegate( new UserData.UpdateDelegate(this) {
            @Override
            public void onUpdate() {
                onResume();
                homeLayout.invalidate();
            }
        });

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/gotham-book.otf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
        initMenu(R.layout.activity_home);
        homeLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        profilePic = (ImageView) findViewById(R.id.profilePic);
        stepsLayout = (RelativeLayout) findViewById(R.id.stepsLayout);
        milesLayout = (RelativeLayout) findViewById(R.id.milesLayout);
        caloriesLayout = (RelativeLayout) findViewById(R.id.caloriesLayout);
        stepsText = (TextView) findViewById(R.id.titleBlockA);
        milesText = (TextView) findViewById(R.id.titleBlockB);
        caloriesText = (TextView) findViewById(R.id.titleBlockC);
        // Setup BLE context

        chartVisible = intentIncoming.getBooleanExtra( EXTRA_CHART_VIEW, false );
        userChange = intentIncoming.getBooleanExtra(USER_CHANGE, false);

        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        db = mDbHelper.getReadableDatabase();

        UserData.addListener(delegate);

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

//            downloadMonthPhotos(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));

//            feawfe
        } else {
            calendar = Calendar.getInstance();
            timestamp = calendar.getTimeInMillis();
            curDay = calendar.get(Calendar.DAY_OF_MONTH);
        }
        String monthChange = "";
        String yearChange = "";

        if(calendar.get(Calendar.MONTH)<11){
            monthChange = "0"+(calendar.get(Calendar.MONTH)+1);
        }else{
            monthChange = String.valueOf(calendar.get(Calendar.MONTH)+1);
        }
        final String monthChangefinal =monthChange;
        yearChange = ""+ calendar.get(Calendar.YEAR);
        Firebase ref = new Firebase(UserData.firebase_url + "users/" +  UserData.getUserData(c).getCurUID() + "/steps/" + yearChange + "/" + monthChange);

        UserData.getUserData(c).insertStepsFromDB(ref, c, monthChangefinal, calendar.get(Calendar.YEAR)+"", delegate );


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
        chart.setDescription("");
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


        ArrayList<String> users = new ArrayList<String>();
        users =  UserData.getUserData(c).getUserList();
        if (!users.isEmpty()) {

            String uid = UserData.getUserData(c).getCurrentUser();
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
//                ProgressBar pbBar = (ProgressBar) findViewById(R.id.progressBar);
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

        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(), "Sync your Wave to find out how far you've come"), 5000, 0);
        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(), "Don't forget to sync and update your Movo calendar."), 10000, 1);
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
        homeLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        try {
            byte[] prof =  UserData.getUserData(c).retrievePhoto(db,0,delegate );
            if (prof != null) {
                Glide.with(c)
                        .load(prof)
//                            .override(1080,1920)
                        .thumbnail(0.1f)
                        .centerCrop()
                        .into(profilePic);
                Log.d(TAG, "Loading profile picture");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<String> users = new ArrayList<String>();
        users =  UserData.getUserData(c).getUserList();
        if (!users.isEmpty()) {

            String uid = UserData.getUserData(c).getCurrentUser();
            UserData.getUserData(c).loadNewUser(uid);
            TextView currentUserTV = (TextView) findViewById(R.id.nameText);
            try {
                if (UserData.getUserData(c).getCurrentUsername().equals("Error")) {
                    currentUserTV.setText("");
                } else {
                    currentUserTV.setText(UserData.getUserData(c).getCurrentUsername());
                }
            }catch(Exception e){
                e.printStackTrace();
                currentUserTV.setText("");
            }
            homeLayout.invalidate();


            setUpCharts(c);


        } else {

            Intent intent = new Intent(getApplicationContext(),
                    FirstLaunch.class);
            startActivity(intent);


        }
        Intent intentIncoming = getIntent();
//        View ourView = (View) findViewById(R.layout.activity_home);

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
        homeLayout.invalidate();

    }

    private int numberOfDigitsBase10(int n) {

        if (n < 100000)
        {
            // 5 or less
            if (n < 100)
            {
                // 1 or 2
                if (n < 10)
                    return 1;
                else
                    return 2;
            }
            else
            {
                // 3 or 4 or 5
                if (n < 1000)
                    return 3;
                else
                {
                    // 4 or 5
                    if (n < 10000)
                        return 4;
                    else
                        return 5;
                }
            }
        }
        else
        {
            // 6 or more
            if (n < 10000000)
            {
                // 6 or 7
                if (n < 1000000)
                    return 6;
                else
                    return 7;
            }
            else
            {
                // 8 to 10
                if (n < 100000000)
                    return 8;
                else
                {
                    // 9 or 10
                    if (n < 1000000000)
                        return 9;
                    else
                        return 10;
                }
            }
        }
    }

    private float roundUp(int value) {

        float roundedTo = 0;
        int digits = numberOfDigitsBase10(value);
        int exp = digits - 1;
        double ten = 10;
        double d = Math.pow(ten, exp);
        int divisor = Double.valueOf(d).intValue();

//        Log.d(TAG, "RoundUp Input: " +  value);
//        Log.d(TAG, "RoundUp digits: " +  digits);
//        Log.d(TAG, "RoundUp Divisor: " +  divisor);
        switch(digits) {
            case 1:
                roundedTo = (float) (value + 1.0);
                break;
            default:
                roundedTo = (value / divisor + 1) * divisor;
        }
//        Log.d(TAG, "RoundUp Value: " +  roundedTo);

        return roundedTo;
    }

    private void setUpChart() {
        ArrayList<Entry> valsComp1 = new ArrayList<Entry>();
        ArrayList<String> xVals = new ArrayList<String>();

        //setup a base calendar
        Calendar window = Calendar.getInstance();
        window.setTimeZone( TimeZone.getDefault() );
        window.setTimeInMillis(timestamp);
        window.set(Calendar.DATE, 1);
        window.set( Calendar.HOUR_OF_DAY, 0 );
        window.set(Calendar.MINUTE, 0);
        window.set( Calendar.SECOND, 0 );
        window.set( Calendar.MILLISECOND, 0 );

        int totalStepsForMonth = 0;
        int greatestSteps = 0;

        final int windowMonth = window.get( Calendar.MONTH );
        final Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getDefault());

        while( window.get( Calendar.MONTH ) == windowMonth ) {
            final int windowDate = window.get(Calendar.DATE);
            final long minTimestamp = window.getTimeInMillis();
            window.add(Calendar.DATE, 1);
            final long maxTimestamp = window.getTimeInMillis();

            Cursor curSteps = getStepsForDateRange(minTimestamp, maxTimestamp, UserData.getUserData(c).getCurUID());
            Entry curEntry = null;
            if (curSteps != null && curSteps.moveToFirst()) {

                int totalStepsForToday = curSteps.getInt(0);

                if (totalStepsForToday > greatestSteps) {
                    greatestSteps = totalStepsForToday;
                }
                totalStepsForMonth += totalStepsForToday;
                curSteps.close();

                if (curChart.equals(ChartType.STEPS)) {
                    curEntry = new Entry(totalStepsForToday, windowDate -1 );
                } else if (curChart.equals(ChartType.CALORIES)) {
                    float cals = (int) calculateTotalCalories(totalStepsForToday);
                    curEntry = new Entry(cals, windowDate - 1);
                } else {
                    float miles = (float) calculateTotalMiles(totalStepsForToday);
                    curEntry = new Entry(miles, windowDate - 1);
                }
                valsComp1.add(curEntry);
            } else {
                Log.d(TAG, "else i Value: " + windowDate );
                curEntry = new Entry(0, windowDate - 1);
                valsComp1.add(curEntry);
            }

            //add +1 for the 0 based day compensation.
            if (curEntry != null) {
                xVals.add((windowDate) + "");
            }
        }

        final int daysInPast;

        window.add( Calendar.DATE, -1 ); //rotate back into date range.
        lazyLog.a( window.get( Calendar.MONTH ) == windowMonth,"Error, window month mismatch!!");
        if( window.getTimeInMillis() < now.getTimeInMillis() ) {
            daysInPast = window.getActualMaximum(Calendar.DATE);
        } else {
            daysInPast = now.get( Calendar.DATE );
        }

        TextView stepsTotal = (TextView) findViewById(R.id.stepTotal);
        TextView stepsAve = (TextView) findViewById(R.id.stepAverage);
        TextView milesTotal = (TextView) findViewById(R.id.distanceTotal);
        TextView milesAve = (TextView) findViewById(R.id.distanceAverage);
        TextView calsTotal = (TextView) findViewById(R.id.caloriesTotal);
        TextView calsAve = (TextView) findViewById(R.id.caloriesAverage);
        double stepsAverageDouble = calculateAverageSteps(totalStepsForMonth, daysInPast);
        stepsAve.setText(String.format("%.1f", stepsAverageDouble));
        double milesTotalDouble = calculateTotalMiles(totalStepsForMonth);
        milesTotal.setText(String.format("%.1f", milesTotalDouble));
        double milesAverageDouble = calculateAverageMiles(totalStepsForMonth, daysInPast);
        milesAve.setText(String.format("%.1f", milesAverageDouble));
        double caloriesDouble = calculateTotalCalories(totalStepsForMonth);
        calsTotal.setText(String.format("%.1f", caloriesDouble));
        double caloriesAverage = calculateAverageCalories(totalStepsForMonth, daysInPast);
        calsAve.setText(String.format("%.1f", caloriesAverage));

        stepsTotal.setText(totalStepsForMonth + "");
//        }


        LineDataSet setComp1 = null;
        if (curChart.equals(ChartType.STEPS)) {
            setComp1 = new LineDataSet(valsComp1, "Steps taken per day");
        } else if (curChart.equals(ChartType.CALORIES)) {
            setComp1 = new LineDataSet(valsComp1, "Calories per day");
        } else {
            setComp1 = new LineDataSet(valsComp1, "Miles per day");
        }

        // LineDataSet setComp1 = new LineDataSet(valsComp1, "Steps taken per day");

        setComp1.setColor(getResources().getColor(R.color.red));
        // setComp1.setCircleColor(getResources().getColor(R.color.red));
        // setComp1.setCircleColorHole(getResources().getColor(R.color.red));
        // setComp1.setCircleSize(0f);
        setComp1.setDrawCircles(false);

        setComp1.setLineWidth(3f);
        // setComp1.setDrawCircleHole(false);
        // setComp1.setValueTextSize(0f);

        setComp1.setHighLightColor(getResources().getColor(R.color.material_blue_grey_950));
        setComp1.setDrawValues(false);
        setComp1.setDrawFilled(true);

        // setComp1.setFillAlpha(65);



        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(setComp1);

        LineData data = new LineData(xVals, dataSets);
        YAxis rightAxis = chart.getAxisRight();
        YAxis leftAxis = chart.getAxisLeft();

        XAxis xaxis = chart.getXAxis();
        xaxis.setAvoidFirstLastClipping(true);

        if (curChart.equals(ChartType.STEPS)) {
            float axisMax = roundUp(greatestSteps);

            rightAxis.setStartAtZero(true);
            rightAxis.setAxisMaxValue(axisMax);
            leftAxis.setStartAtZero(true);
            leftAxis.setAxisMaxValue(axisMax);
        } else if (curChart.equals(ChartType.CALORIES)) {
            double calories = calculateTotalCalories(greatestSteps);
            float axisMax = roundUp(Double.valueOf(calories).intValue());

            rightAxis.setStartAtZero(true);
            rightAxis.setAxisMaxValue(axisMax);
            leftAxis.setStartAtZero(true);
            leftAxis.setAxisMaxValue(axisMax);
        } else {
            double miles = calculateTotalMiles(greatestSteps);
            float axisMax = roundUp(Double.valueOf(miles).intValue());

            rightAxis.setStartAtZero(true);
            rightAxis.setAxisMaxValue(axisMax);
            leftAxis.setStartAtZero(true);
            leftAxis.setAxisMaxValue(axisMax);
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
            calendar.setTimeZone(TimeZone.getDefault());
            numberOfDaysTotal = calendar.getActualMaximum(Calendar.DATE) - calendar.get( Calendar.DATE);
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
            monthCal.setTimeZone(TimeZone.getDefault());
            monthCal.set( Calendar.HOUR_OF_DAY, 0 );
            monthCal.set( Calendar.MINUTE, 0 );
            monthCal.set( Calendar.SECOND, 0 );
            monthCal.set( Calendar.MILLISECOND, 0 );

            long monthRangeStart = monthCal.getTimeInMillis();
//            monthCal.set(monthCal.get(Calendar.YEAR), monthCal.get(Calendar.MONTH), dayToDisplay, monthCal.getActualMaximum(Calendar.HOUR_OF_DAY), monthCal.getActualMaximum(Calendar.MINUTE), monthCal.getActualMaximum(Calendar.SECOND));
            monthCal.add(Calendar.DATE, 1);
            long monthRangeStop = monthCal.getTimeInMillis();
            ImageView background = (ImageView) gridView.findViewById(R.id.cellBackground);
//            Bitmap bm = null;
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
            if (curSteps != null && curSteps.moveToFirst()) {
                int totalStepsForToday = curSteps.getInt(0);

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
        GridView gridview = (GridView) findViewById(R.id.gridview);
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

        final String query = "SELECT SUM(" +Database.StepEntry.STEPS +
                ") FROM " + Database.StepEntry.STEPS_TABLE_NAME + " WHERE " +
                Database.StepEntry.START + " >=? AND " + Database.StepEntry.END +
                "<=? AND " + Database.StepEntry.USER + " =? ";

        final String[] args = new String[]{
                Long.toString(monthRangeStart),
                Long.toString(monthRangeStop),
                userID};

        Cursor curSteps = db.rawQuery(query, args);

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

//    private void insertSteps(DataSnapshot snapshot, int year, int month, Context c) {
//        String monthChange = "";
//        String yearChange = "";
//
//
//        if(month<11){
//            monthChange = "0"+(month+1);
//        }else{
//            monthChange = String.valueOf(month+1);
//        }
//        yearChange = ""+ year;
//
//        UserData.getUserData(c).insertStepsFromDB(snapshot, c, monthChange, yearChange);
//    }







    public byte[] dailyPhotoFetch(long today) {
        return UserData.getUserData(c).retrievePhoto(db, today, delegate);

    }

//    public void downloadMonthPhotos(int month, int year){
//        int monthFix = month + 1;
//        Calendar thisMonth = Calendar.getInstance();
//        thisMonth.set(Calendar.MONTH, monthFix);
////        thisMonth.add(Calendar.MONTH, 1);
//        thisMonth.set(Calendar.YEAR, year);
//
//        for(int i = 0; i < thisMonth.getActualMaximum(Calendar.DATE); i++){
//            thisMonth.set(Calendar.DATE, (i+1));
//            UserData.getUserData(c).downloadPhotoForDate(thisMonth.getTimeInMillis(), delegate);
//
//        }
//
//    }








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
