package com.movo.wave;
/**
 * Created by PhilG on 4/22/2015.
 */

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.movo.wave.util.Calculator;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

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
    String user;
    int uniquePic;
    Calendar monthCal;
    ImageView background;
    RelativeLayout wholeView;
    boolean localFile;
    private static final int SELECT_PHOTO = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/gotham-book.otf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        setContentView(R.layout.activity_daily);
        c = this.getApplicationContext();

        miles = (TextView) findViewById(R.id.tvMiles);
        calories = (TextView) findViewById(R.id.tvCalories);
        steps = (TextView) findViewById(R.id.tvSteps);
        tvToday = (TextView) findViewById(R.id.tvCurDate);
        wholeView = (RelativeLayout) findViewById(R.id.drawer_layout);
        photo = (TextView) findViewById(R.id.tvPhoto);
        background = (ImageView) findViewById(R.id.background);

        photo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        });

        back = (TextView) findViewById(R.id.tvBack);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });




        Intent intent = getIntent();
        if (null != intent) { //Null Checking
            String date= intent.getStringExtra("date");
            Long dateLong = Long.parseLong(date);
//            Log.d(TAG, StrData);

            today = new Date(dateLong);



          monthCal = Calendar.getInstance();
            monthCal.setTime(today);
            SimpleDateFormat month_date = new SimpleDateFormat("MMM");
            String month_name = monthCal.getDisplayName(monthCal.MONTH,Calendar.SHORT, Locale.US);
            tvToday.setText((month_name+" "+monthCal.get(Calendar.DAY_OF_MONTH)).toUpperCase());
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
                    long timeTarget = today.getTime()+86400000;
                    String tomorrow = timeTarget+"";
                    intent.putExtra("date",tomorrow);
                    Calendar todayTime = Calendar.getInstance();
//                    todayTime.setTimeInMillis(today.getTime());

                    if(todayTime.getTimeInMillis()<=timeTarget){
                        //Do not pass go.
                    }else {
                        startActivity(intent);
                        finish();
                    }
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


            Date curDay = trim(new Date(monthCal.getTimeInMillis()));
            UserData myData = UserData.getUserData(c);
            user = myData.getCurUID();
            String photo =  Database.PhotoStore.DATE + " =? AND "+Database.PhotoStore.USER + " =?";
            Cursor curPhoto = db.query(
                    Database.PhotoStore.PHOTO_TABLE_NAME,  // The table to query
                    new String[] {
                            Database.StepEntry.USER, //string
                            Database.PhotoStore.DATE, //int
                            Database.PhotoStore.PHOTOBLOB }, //blob                          // The columns to return
                    photo,                                // The columns for the WHERE clause
                    new String[] { curDay.getTime()+"", user },                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                 // The sort order
            );

            curPhoto.moveToFirst();
            localFile = false;
            if(curPhoto.getCount()!=0){
                localFile = true;
                byte[] byteArray = curPhoto.getBlob(2);
//                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                uniquePic = byteArray.length;
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                options.inSampleSize = 4;
                Bitmap bm = BitmapFactory.decodeByteArray(byteArray, 0 ,byteArray.length, options);

                background.setImageBitmap(bm);
//                setContentView(R.layout.activity_daily);
            }
                Log.d(TAG, "Loading image from firebase");
                Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + user + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthCal.get(Calendar.MONTH) + "/" + (monthCal.get(Calendar.DAY_OF_MONTH)));
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
    //                    System.out.println(snapshot.getValue());
                        if(snapshot.getChildrenCount()==1){
                            final BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = false;
                            options.inSampleSize = 4;
                            ArrayList<String> result =((ArrayList<String>)snapshot.getValue());
                            byte[] decodedString = Base64.decode(result.get( 0 ), Base64.DEFAULT);

                            DatabaseHelper mDbHelper = new DatabaseHelper(c);
                            SQLiteDatabase db = mDbHelper.getWritableDatabase();
//
                            if(localFile) {
                                int comparePic = decodedString.length;
                                if(uniquePic!=comparePic){
                                    //file from cloud is different than local, save to device
                                    Date curDay = trim(new Date(monthCal.getTimeInMillis()));
                                    ContentValues syncValues = new ContentValues();
                                    syncValues.put(Database.PhotoStore.DATE, curDay.getTime());
                                    syncValues.put(Database.PhotoStore.USER, user);
                                    syncValues.put(Database.PhotoStore.PHOTOBLOB, decodedString);
                                    long newRowId;
                                    newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                                            null,
                                            syncValues);
                                    Log.d(TAG, "Photo database add from firebase: "+newRowId);
                                    db.close();
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length,options);
                                    background.setImageBitmap(decodedByte);

                                }

                            }else{
                                //file doesn't exist on local device
                                Date curDay = trim(new Date(monthCal.getTimeInMillis()));
                                ContentValues syncValues = new ContentValues();
                                syncValues.put(Database.PhotoStore.DATE, curDay.getTime());
                                syncValues.put(Database.PhotoStore.USER, user);
                                syncValues.put(Database.PhotoStore.PHOTOBLOB, decodedString);
                                long newRowId;
                                newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                                        null,
                                        syncValues);
                                Log.d(TAG, "Photo database add from firebase: "+newRowId);
                                db.close();
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length,options);
                                background.setImageBitmap(decodedByte);
                            }


                        }else{
                            //multipart file upload

                        }
                    }
                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("The read failed: " + firebaseError.getMessage());
                    }
                });


//            }


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


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
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



    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();


//                    Bitmap selectedImageToUpload; = BitmapFactory.decodeFile(filePath);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BitmapFactory.decodeFile(filePath).compress(Bitmap.CompressFormat.JPEG, 10, baos); //bm is the bitmap object
                    byte[] b = baos.toByteArray();
                    String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);

                    UserData myData = UserData.getUserData(c);
                    String user = myData.getCurUID();

                    Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + user + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthCal.get(Calendar.MONTH) + "/" + (monthCal.get(Calendar.DAY_OF_MONTH)));

                    DatabaseHelper mDbHelper = new DatabaseHelper(c);
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();


                    Date curDay = trim(new Date(monthCal.getTimeInMillis()));
                    ContentValues syncValues = new ContentValues();
                    syncValues.put(Database.PhotoStore.DATE, curDay.getTime());
                    syncValues.put(Database.PhotoStore.USER, user);
                    syncValues.put(Database.PhotoStore.PHOTOBLOB, b);

                    long newRowId;
                    newRowId = db.insert(Database.PhotoStore.PHOTO_TABLE_NAME,
                            null,
                            syncValues);
                    Log.d(TAG, "Photo database add: "+newRowId);
                    db.close();

                    if(encodedImage.length()>1000000) {
                        List<String> strings = new ArrayList<String>();
                        int index = 0;
                        while (index < encodedImage.length()) {
                            strings.add(encodedImage.substring(index, Math.min(index + 1000000, encodedImage.length())));
                            index += 1000000;
                        }


                        Log.d(TAG, "Starting image upload " + ref);
                    for(int i = 0;i<strings.size();i++){
                        ref.child(""+i).setValue(strings.get(i), new Firebase.CompletionListener() {
                            @Override
                            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                                if (firebaseError != null) {
                                    Log.i(firebaseError.toString(), firebaseError.toString());
                                }
                            }

                        });
                        Log.d(TAG, "Image upload progress "+i+" "+ref.child(""+i));
                    }
                    }else{

                    }
                    ref.child(0+"").setValue(encodedImage);
                    Log.d(TAG, "End image upload "+ref);
//                    ref.setValue(encodedImage);
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
}
