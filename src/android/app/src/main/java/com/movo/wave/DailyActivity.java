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
import android.widget.TextView;

import com.movo.wave.util.Calculator;

import java.util.Calendar;
import java.util.Date;

public class DailyActivity extends ActionBarActivity {
    Context c;
    String TAG = "Movo DailyActivity";
    TextView miles;
    TextView calories;
    TextView steps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily);
        c = this.getApplicationContext();
        Date today;
        miles = (TextView) findViewById(R.id.tvMiles);
        calories = (TextView) findViewById(R.id.tvCalories);
        steps = (TextView) findViewById(R.id.tvSteps);

        Intent intent = getIntent();
        if (null != intent) { //Null Checking
            String date= intent.getStringExtra("date");
            Long dateLong = Long.parseLong(date);
//            Log.d(TAG, StrData);

            today = new Date(dateLong);

//            Calendar monthCal = Calendar.getInstance();
//            monthCal.set(2015,calendar.get(today.getMonth()),i+1,0,0,0);
            long monthRangeStart = dateLong;
            long oneDayInMillis = 86400000;
            long monthRangeStop = dateLong + oneDayInMillis;

            DatabaseHelper mDbHelper = new DatabaseHelper(c);
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

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

            this.finish();

        }





    }
}