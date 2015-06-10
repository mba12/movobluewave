package com.movo.wave;
/**
 * Created by PhilG on 4/22/2015.
 */

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.movo.wave.util.Calculator;
import com.movo.wave.util.DataUtilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class DailyActivity extends ActionBarActivity {
    Context c;
    String TAG = "Movo DailyActivity";
    TextView miles;
    TextView calories;
    TextView steps;
    ImageView back;
    TextView photo;
    TextView tvToday;
    Date today;
    String user;
    int uniquePic;
    Calendar monthCal;
    ImageView background;
    RelativeLayout wholeView;
    UserData.UpdateDelegate delegate;
    SQLiteDatabase db;
    RelativeLayout drawer_layout_daily;

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_daily);
        drawer_layout_daily = (RelativeLayout) findViewById(R.id.drawer_layout);
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        delegate = new UserData.UpdateDelegate(this) {
            @Override
            public void onUpdate() {
                DailyActivity.this.onResume();
                drawer_layout_daily.invalidate();
            }
        };













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

        //http://dimitar.me/how-to-get-picasa-images-using-the-image-picker-on-android-devices-running-any-os-version/

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    if( imageReturnedIntent == null ) {
                        Log.e( TAG, "NULL image intent result!");
                    }

                    ByteArrayOutputStream baos;
                    Uri selectedImage=null;
                    int orientation = 0;


                    try {
                        selectedImage = imageReturnedIntent.getData();


//                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        baos = new ByteArrayOutputStream();

                        Log.i( TAG, "Resolving URI: " + selectedImage);

                        final InputStream is = getContentResolver().openInputStream(selectedImage);

                        ///image rotation check
                        String[] projection = { MediaStore.Images.Media.DATA };
                        @SuppressWarnings("deprecation")
                        Cursor cursor = managedQuery(selectedImage, projection, null, null, null);
                        int column_index = cursor
                                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        cursor.moveToFirst();
                        String path = cursor.getString(column_index);

                        ExifInterface ei = new ExifInterface(path);
                        orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);


//                        ExifInterface exif = new ExifInterface(SourceFileName);     //Since API Level 5
                        String exifOrientation = ei.getAttribute(ExifInterface.TAG_ORIENTATION);

                        BitmapFactory.Options options = new BitmapFactory.Options();
//                        options.inJustDecodeBounds = true;
                        int inSampleSize = 2;
                        options.inSampleSize = inSampleSize;

//                        options.inSampleSize = 8;  //This will reduce the image size by a power of 8
                        BitmapFactory.decodeStream(is,null,options).compress(Bitmap.CompressFormat.JPEG, 50, baos);


                        int imageWidth = options.outWidth;
                        int imageHeight = options.outHeight;
//
                        if (imageHeight > 1920 || imageWidth > 1080) {

                            final int halfHeight = imageHeight / 2;
                            final int halfWidth = imageWidth / 2;

                            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                            // height and width larger than the requested height and width.
                            while ((halfHeight / inSampleSize) > 1080
                                    && (halfWidth / inSampleSize) > 1920) {
                                inSampleSize *= 2;
                            }
                        }

                        options.inJustDecodeBounds = false;


//                        is.reset();
//                        BitmapFactory.decodeStream(is,null,options).compress(Bitmap.CompressFormat.JPEG, 50, baos);


                    } catch( Exception e ) {
                        baos = null;
                        final String error = "Cannot resolve URI: " + selectedImage;
                        Log.e( TAG, error );
                        Toast.makeText(c, error, Toast.LENGTH_LONG).show();
                        break;
                    }

                    byte[] b = baos.toByteArray();
                    Bitmap bitmapBit = BitmapFactory.decodeByteArray(b, 0, b.length);
                    ByteArrayOutputStream stream = null;


                    switch(orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            Log.d(TAG, "Image rotated 90");
                            bitmapBit = DataUtilities.RotateBitmap(bitmapBit, 90);
                            stream = new ByteArrayOutputStream();
                            bitmapBit.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            b = stream.toByteArray();
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            Log.d(TAG, "Image rotated 180");
                            bitmapBit = DataUtilities.RotateBitmap(bitmapBit, 180);
                            stream = new ByteArrayOutputStream();
                            bitmapBit.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            b = stream.toByteArray();
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            Log.d(TAG, "Image rotated 270");
                            bitmapBit = DataUtilities.RotateBitmap(bitmapBit, 270);
                            stream = new ByteArrayOutputStream();
                            bitmapBit.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            b = stream.toByteArray();
                            break;
                    }
                    bitmapBit.recycle();

                    String encodedImage = Base64.encodeToString(b, Base64.NO_WRAP);




                    Glide.with(c)
                            .load(b)
                            .fitCenter()
//                                .override(1080,1920)
                            .into(background);
//                    UserData myData = UserData.getUserData(c);
                    String user =  UserData.getUserData(c).getCurUID();

                    String monthChange = "";
                    String dayChange = "";
                    if ((monthCal.get(Calendar.MONTH)) < 11) {
                        monthChange = "0" + (monthCal.get(Calendar.MONTH) + 1);
                    } else {
                        monthChange = String.valueOf(monthCal.get(Calendar.MONTH) + 1);
                    }
                    if ((monthCal.get(Calendar.DATE)) < 10) {
                        dayChange = "0" + (monthCal.get(Calendar.DATE));
                    } else {
                        dayChange = String.valueOf(monthCal.get(Calendar.DATE));
                    }




                    //database insert
                    String md5 = DataUtilities.getMD5EncryptedString(encodedImage);
                    UserData.getUserData(c).storePhoto(b, monthCal.getTimeInMillis(), md5);

                    //end database insert


                    //upload call
                    Firebase ref = new Firebase(UserData.firebase_url + "users/" + user + "/photos/" + monthCal.get(Calendar.YEAR) + "/" + monthChange + "/" + dayChange);
                    DataUtilities.uploadPhotoToFB(ref, encodedImage);


                    Log.d(TAG, "End image upload "+ref);
//                    ref.setValue(encodedImage);
                } else {
                    final String error = "No photo selected";
                    Log.e( TAG, error );
                    Toast.makeText(c, error, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Log.e(TAG, "Error, unexpected intent result for " + requestCode);
        }
    }


    public byte[] dailyPhotoFetch(long today) {
        return UserData.getUserData(c).retrievePhoto(db, today, delegate);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        delegate.disable();
        db.close();
    }
    @Override
    protected void onResume(){
        super.onResume();


        Intent intent = getIntent();
        LaunchAnimation.apply( this, intent );
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

                startActivityForResult( MenuActivity.photoPickerIntent(), SELECT_PHOTO);
            }
        });

        back = (ImageView) findViewById(R.id.tvBack);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });


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
                    LaunchAnimation.SLIDE_LEFT.setIntent( intent );
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

                    LaunchAnimation.SLIDE_RIGHT.setIntent( intent );
                    String tomorrow = (today.getTime()-86400000)+"";
                    intent.putExtra("date",tomorrow);
                    startActivity(intent);
                    finish();
                }
            });


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
/*
                // NOTE: saving for later. Michael
                String height1 =  UserData.getUserData(c).getCurrentHeight1();
                String height2 =  UserData.getUserData(c).getCurrentHeight2();
                String weight =  UserData.getUserData(c).getCurrentWeight();
                String birth =  UserData.getUserData(c).getCurrentBirthdate();
                String gender =  UserData.getUserData(c).getCurrentGender();
*/
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
