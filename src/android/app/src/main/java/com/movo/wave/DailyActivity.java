package com.movo.wave;
/**
 * Created by PhilG on 4/22/2015.
 */

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.movo.wave.util.Calculator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DailyActivity extends ActionBarActivity {
    Context c;
    String TAG = "Movo DailyActivity";
    TextView miles;
    TextView calories;
    TextView steps;
    TextView back;
    TextView photo;
    TextView tvToday;
    Date today;
    RelativeLayout wholeView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily);
        c = this.getApplicationContext();

        miles = (TextView) findViewById(R.id.tvMiles);
        calories = (TextView) findViewById(R.id.tvCalories);
        steps = (TextView) findViewById(R.id.tvSteps);
        tvToday = (TextView) findViewById(R.id.tvCurDate);
        wholeView = (RelativeLayout) findViewById(R.id.drawer_layout);


        back = (TextView) findViewById(R.id.tvBack);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

//start new dailyactivity
//        Intent intent = new Intent(getApplicationContext(),
//                DailyActivity.class);
//        Bundle extras = new Bundle();
////                extras.putString(*/
//        intent.putExtra("date",tv.getText().toString());
//        startActivity(intent);



        Intent intent = getIntent();
        if (null != intent) { //Null Checking
            String date= intent.getStringExtra("date");
            Long dateLong = Long.parseLong(date);
//            Log.d(TAG, StrData);

            today = new Date(dateLong);



            final Calendar monthCal = Calendar.getInstance();
            monthCal.setTime(today);
            SimpleDateFormat month_date = new SimpleDateFormat("MMM");
            String month_name = monthCal.getDisplayName(monthCal.MONTH,Calendar.SHORT, Locale.US);
            tvToday.setText(month_name+" "+monthCal.get(Calendar.DAY_OF_MONTH));
//            monthCal.set(2015,calendar.get(today.getMonth()),i+1,0,0,0);
            long monthRangeStart = dateLong;
            long oneDayInMillis = 86400000;
            long monthRangeStop = dateLong + oneDayInMillis;

            DatabaseHelper mDbHelper = new DatabaseHelper(c);
            SQLiteDatabase db = mDbHelper.getReadableDatabase();


            wholeView.setOnTouchListener(new OnSwipeTouchListener(c){
                @Override
                public void onSwipeLeft() {
                    Log.d(TAG, "Swipe Left");
                    //this is forward a day
                    Intent intent = new Intent(getApplicationContext(),
                            DailyActivity.class);
                    Bundle extras = new Bundle();
                    String tomorrow = (today.getTime()+86400000)+"";
                    intent.putExtra("date",tomorrow);
                    startActivity(intent);
                    finish();
                }
                @Override
                public void onSwipeRight() {
                    Log.d(TAG, "Swipe Right");
                    //this is forward a day
                    Intent intent = new Intent(getApplicationContext(),
                            DailyActivity.class);
                    Bundle extras = new Bundle();
                    String tomorrow = (today.getTime()-86400000)+"";
                    intent.putExtra("date",tomorrow);
                    startActivity(intent);
                    finish();


                }
            });


            String selectionSteps =  Database.StepEntry.START + " > ? AND "+Database.StepEntry.END + " < ?";
            ContentValues valuesReadSteps = new ContentValues();
            Cursor curSteps = db.query(
                    Database.StepEntry.STEPS_TABLE_NAME,  // The table to query
                    new String[] { Database.StepEntry.SYNC_ID, //blob
                            Database.StepEntry.START, //int
                            Database.StepEntry.END, //int
                            Database.StepEntry.USER, //string
                            Database.StepEntry.STEPS, //int
                            Database.StepEntry.DEVICEID }, //blob                          // The columns to return
                    selectionSteps,                                // The columns for the WHERE clause
                    new String[] { monthRangeStart+"", monthRangeStop+"" },                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                 // The sort order
            );

            curSteps.moveToFirst();
            int stepsTaken=0;
            if(curSteps.getCount()!=0) {


                while (curSteps.isAfterLast() == false) {
                    stepsTaken += curSteps.getInt(4);

                    curSteps.moveToNext();
//
                }


                steps.setText(stepsTaken + " STEPS");
                Calculator calc = new Calculator();

//                double caloriesUsed = calc.calculate_calories(stepsTaken, 72, 170, "Male", 1987, 24);
                double caloriesUsed = calc.simple_calculate_calories(stepsTaken);

//                    calculate_calories(int steps, int height, int weight, String gender, int birthYear, int minutes) {
                calories.setText(String.format("%.1f CAL", caloriesUsed));

                double milesTraveled = calc.calculate_distance(stepsTaken, 72);
                miles.setText(String.format("%.1f MILES", milesTraveled));
            }else{
                steps.setText(0 + " STEPS");

                calories.setText("0.0 CAL");


                miles.setText("0.0 MILES");
            }




        }else{
            //we shouldn't get here naturally unless the app was resumed in a weird state, close out of daily view

            finish();

        }

    }


    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {
        }

        public void onSwipeRight() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0)
                        onSwipeRight();
                    else
                        onSwipeLeft();
                    return true;
                }
                return false;
            }
        }
    }
}