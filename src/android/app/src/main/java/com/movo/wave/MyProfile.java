package com.movo.wave;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.movo.wave.util.DataUtilities;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by P on 4/27/2015.
 */
public class MyProfile extends MenuActivity {
    String TAG = "Movo MyProfile";

    private static ProgressBar syncProgressBar;
    private static TextView syncText;
    int mYear;
    int mMonth;
    int mDay;
    Button birthdateButton;
    private static final int SELECT_PHOTO = 100;
    long birthdateInput;
    //    int height1;
//    int height2;
//    int weight;
//    String gender;
    Spinner mSpinnerHeight1;
    Spinner mSpinnerHeight2;
    Spinner mSpinnerWeight;
    Spinner mSpinner;
    Button profileSave;
    Button profileCancel;
    ImageView profilePic;
    EditText edName;
    String height1;
    String height2;
    String weight;
    String birth;
    String fullName;
    String gender;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMenu(R.layout.activity_my_profile);
//        UserData myUserData = UserData.getUserData(c);
        edName = (EditText) findViewById(R.id.edName);
        profileSave = (Button) findViewById(R.id.profileSave);
        profileCancel = (Button) findViewById(R.id.profileCancel);
        profilePic = (ImageView) findViewById(R.id.profilePic);
        height1 =  UserData.getUserData(c).getCurrentHeight1();
        height2 =  UserData.getUserData(c).getCurrentHeight2();
        weight =  UserData.getUserData(c).getCurrentWeight();
        birth =  UserData.getUserData(c).getCurrentBirthdate();
        fullName =  UserData.getUserData(c).getCurrentFullName();
        gender =  UserData.getUserData(c).getCurrentGender();
        Log.d(TAG, "User profile load "+
                " "+height1+
                " "+height2+
                " "+weight+
                " "+fullName+
                " "+gender+
                " "+birth);

        try {
            if (!fullName.equals("Error")) {
                edName.setText(fullName);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        byte[] prof =  UserData.getUserData(c).retrievePhoto(0);
        if (prof != null && prof.length !=0 ) {
            Glide.with(c)
                    .load(prof)
//                            .override(1080,1920)
                    .thumbnail(0.1f)
                    .centerCrop()
                    .into(profilePic);
//            profilePic.setImageBitmap(prof);
        }

        //---------------Set up Arraylists------------//
        ArrayList<String> options=new ArrayList<String>();
        options.add("Gender");
        options.add("Male");
        options.add("Female");

        ArrayList<String> height1Options=new ArrayList<String>();
//        height1.add("Select Height in Feet");
        for(int i = 0;i<8;i++){
            height1Options.add((i+1)+"");
        }
        ArrayList<String> height2Options=new ArrayList<String>();
//        height2.add("Select Height in Inches");
        for(int i = 0;i<12;i++){
            height2Options.add((i) + "");
        }
        ArrayList<String> weightOptions=new ArrayList<String>();
//        weight.add("Select Weight in Pounds");
        for(int i = 50;i<=300;i=i+5){
            weightOptions.add((i)+"");
        }



        mSpinner = (Spinner)findViewById(R.id.spGender);
        mSpinnerHeight1 = (Spinner) findViewById(R.id.spHeight);
        mSpinnerHeight2 = (Spinner) findViewById(R.id.spHeight2);
        mSpinnerWeight = (Spinner) findViewById(R.id.spWeight);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_layout,options);
        mSpinner.setAdapter(adapter); // this will set list of values to spinner
        ArrayAdapter<String> adapterHeight = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_layout,height1Options);
        mSpinnerHeight1.setAdapter(adapterHeight);
        ArrayAdapter<String> adapterHeight2 = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_layout,height2Options);
        mSpinnerHeight2.setAdapter(adapterHeight2);
        ArrayAdapter<String> adapterWeight = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_layout,weightOptions);
        mSpinnerWeight.setAdapter(adapterWeight);

        try {
            if (!gender.equals("Error")) {
                mSpinner.setSelection(options.indexOf(gender));//set selected value in spinner
            } else {
                mSpinner.setSelection(options.indexOf("Gender"));//set selected value in spinner
            }
        }catch(Exception e){
            e.printStackTrace();
            mSpinner.setSelection(options.indexOf("Gender"));//set selected value in spinner
        }
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                Log.d(TAG, "SELECTED GENDER");
                gender = parentView.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });
        try{
            if(!height1.equals("Error")){
                mSpinnerHeight1.setSelection(height1Options.indexOf(height1));//set selected value in spinner
            }else{
                mSpinnerHeight1.setSelection(height1Options.indexOf("1"));//set selected value in spinner
            }
        }catch(Exception e){
            e.printStackTrace();
            mSpinnerHeight1.setSelection(height1Options.indexOf("1"));//set selected value in spinner
        }

//        mSpinnerHeight1.setSelection(options.indexOf("Gender"));//set selected value in spinner
        mSpinnerHeight1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                Log.d(TAG, "SELECTED HEIGHT1");
                height1 = parentView.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });
        try{
            if(!height2.equals("Error")){
                mSpinnerHeight2.setSelection(height2Options.indexOf(height2));//set selected value in spinner
            }else{
                mSpinnerHeight2.setSelection(height2Options.indexOf("0"));//set selected value in spinner
            }
        }catch(Exception e){
            e.printStackTrace();
            mSpinnerHeight2.setSelection(height2Options.indexOf("0"));//set selected value in spinner
        }
//        mSpinnerHeight2.setSelection(options.indexOf("Gender"));//set selected value in spinner
        mSpinnerHeight2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                Log.d(TAG, "SELECTED HEIGHT2");
                height2 = parentView.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });
        try{
            if(!weight.equals("Error")){
                mSpinnerWeight.setSelection(weightOptions.indexOf(weight));//set selected value in spinner
            }else{
                mSpinnerWeight.setSelection(weightOptions.indexOf("50"));//set selected value in spinner
            }
        }catch(Exception e){
            e.printStackTrace();
            mSpinnerWeight.setSelection(weightOptions.indexOf("50"));//set selected value in spinner
        }
//        mSpinner.setSelection(options.indexOf("Gender"));//set selected value in spinner
        mSpinnerWeight.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                Log.d(TAG, "SELECTED WEIGHT");
                weight = parentView.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });


        //---------------Set up Arraylists------------//

        birthdateButton = (Button) findViewById(R.id.birthday);
        try{
            if(!birth.equals("Error")&&(!birth.equals("null"))){
                Calendar birthCal = Calendar.getInstance();
                birthCal.setTimeInMillis(Long.parseLong(birth));
                String birthDisplay = (birthCal.get(Calendar.MONTH)+1)+"-"+(birthCal.get(Calendar.MONTH)+1)+"-"+birthCal.get(Calendar.YEAR);
                birthdateButton.setText(birthDisplay);
            }
        }catch(Exception e){
            e.printStackTrace();

        }

        birthdateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DatePickerDialog dpd = new DatePickerDialog(MyProfile.this,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                birthdateButton.setText((monthOfYear + 1)+"-"+dayOfMonth + "-" + year);

                                Calendar birthCal = Calendar.getInstance();
                                birthCal.set(Calendar.YEAR, year);
                                birthCal.set(Calendar.MONTH, monthOfYear);
                                birthCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                Calendar checkCal = Calendar.getInstance();
                                checkCal.add(Calendar.YEAR, -13);
                                if(birthCal.getTimeInMillis()>=checkCal.getTimeInMillis()){

                                    Log.d(TAG,"Account not 13");
                                    Toast.makeText(c, "You must be 13 years of age or older to use this application.", Toast.LENGTH_LONG).show();
                                }else{
                                    Log.d(TAG,"Account IS 13");
                                    birthdateInput = birthCal.getTimeInMillis();
                                    birth = birthdateInput+"";

                                }

                            }
                        }, mYear, mMonth, mDay);
                if(!birth.equals("Error")&&(!birth.equals("Null"))) {
                    Calendar birthCal = Calendar.getInstance();
                    try {
                        birthCal.setTimeInMillis(Long.parseLong(birth));
                        dpd.updateDate(birthCal.get(Calendar.YEAR), birthCal.get(Calendar.MONTH), birthCal.get(Calendar.DAY_OF_MONTH));
                    }catch (Exception e){
                        dpd.updateDate(1980,0,0);
                    }
                }else{
                    dpd.updateDate(1980,0,0);
                }
                dpd.show();
            }
        });

        profileCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        profileSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Saving new user data");
                fullName = edName.getText().toString();
//                UserData myUserData = UserData.getUserData(c);
                UserData.getUserData(c).setCurName(fullName);
                UserData.getUserData(c).setCurHeight1(height1);
                UserData.getUserData(c).setCurHeight2(height2);
                UserData.getUserData(c).setCurWeight(weight);
                UserData.getUserData(c).setCurGender(gender);
                UserData.getUserData(c).setCurBirthdate(birth);
//                myUserData.setCurUsername()
                UserData.getUserData(c).uploadToFirebase();

                Toast.makeText(c,"User data has been saved.",Toast.LENGTH_LONG).show();
                finish();

            }
        });

        profilePic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(photoPickerIntent(), SELECT_PHOTO);
            }
        });
    }
    //    protected void onActivityResult(int requestCode, int resultCode,
//                                    Intent imageReturnedIntent) {
//        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
//
//        Log.d(TAG, "Recieved result intent from photo");
//        switch(requestCode) {
//            case SELECT_PHOTO:
//                if(resultCode == RESULT_OK){
//                    if( imageReturnedIntent == null ) {
//                        Log.e( TAG, "NULL image intent result!");
//                    }
//                    Uri selectedImage = imageReturnedIntent.getData();
//                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
//
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//                    Log.i( TAG, "Resolving URI: " + selectedImage);
//
//                    try {
//                        final InputStream is = getContentResolver().openInputStream(selectedImage);
//                        BitmapFactory.Options options = new BitmapFactory.Options();
////                        options.inJustDecodeBounds = true;
//                        int inSampleSize = 2;
//                        options.inSampleSize = inSampleSize;
////                        options.inSampleSize = 8;  //This will reduce the image size by a power of 8
//                        BitmapFactory.decodeStream(is, null, options).compress(Bitmap.CompressFormat.JPEG, 50, baos);
//                    }catch(Exception e){
//                        e.printStackTrace();
//                    }
//                    byte[] b = baos.toByteArray();
//                    String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
//
////                    UserData myData = UserData.getUserData(c);
//                    String user =  UserData.getUserData(c).getCurUID();
//
//                    Firebase ref = new Firebase(UserData.firebase_url + "users/" + user + "/photos/profilepic");
//
//
//
//                    Calendar profile = Calendar.getInstance();
//                    profile.setTimeInMillis(0); // user photos will be stored at the dawn of time.
////                    Date curDay = trim(new Date(profile.getTimeInMillis()));
//                    //db insert
//                    //database insert
//                    String md5 = DataUtilities.getMD5EncryptedString(encodedImage);
//                    UserData.getUserData(c).storePhoto(baos, profile.getTimeInMillis(), md5);
//
//                    //end database insert
//
//                    //photo upload
//                    DataUtilities.uploadPhotoToFB(ref, encodedImage);
//
//                    Log.d(TAG, "End image upload "+ref);
//                    byte[] prof = UserData.getUserData(c).getCurUserPhoto();
////                     myUserData.getCurUserPhoto(c);
//                    if(prof != null && prof.length!=0){
////                        profilePic.setImageBitmap(prof);
//                        Glide.with(c)
//                                .load(prof)
////                            .override(1080,1920)
//                                .thumbnail(0.1f)
//                                .centerCrop()
//                                .into(profilePic);
//                    }
////                    ref.setValue(encodedImage);
//                }else {
//                    final String error = "No photo selected";
//                    Log.e( TAG, error );
//                    Toast.makeText(c, error, Toast.LENGTH_SHORT).show();
//                }
//                break;
//            default:
//                Log.e(TAG, "Error, unexpected intent result for " + requestCode);
//        }
//    }
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
//                    byte[] byteArrayBit = null;
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
                    }
                    bitmapBit.recycle();


                    String encodedImage = Base64.encodeToString(b, Base64.NO_WRAP);
                    Glide.with(c)
                            .load(b)
                            .fitCenter()
//                                .override(1080,1920)
                            .into(profilePic);
//                    UserData myData = UserData.getUserData(c);
                    String user =  UserData.getUserData(c).getCurUID();


                    Log.d(TAG, "Loading image from firebase");
                    Firebase ref = new Firebase(UserData.firebase_url + "users/" + user + "/photos/profilepic");
                    //database insert
                    String md5 = DataUtilities.getMD5EncryptedString(encodedImage);
                    UserData.getUserData(c).storePhoto(b, 0, md5);

                    //end database insert





                    //upload call
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

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        byte[] prof =  UserData.getUserData(c).retrievePhoto(0);
        if (prof != null && prof.length !=0 ) {
            Glide.with(c)
                    .load(prof)
//                            .override(1080,1920)
                    .thumbnail(0.1f)
                    .centerCrop()
                    .into(profilePic);
//            profilePic.setImageBitmap(prof);
        }

    }


}
