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

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.io.ByteArrayOutputStream;
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
        UserData myUserData = UserData.getUserData(c);
        edName = (EditText) findViewById(R.id.edName);
        profileSave = (Button) findViewById(R.id.profileSave);
        profileCancel = (Button) findViewById(R.id.profileCancel);
        profilePic = (ImageView) findViewById(R.id.profilePic);
        height1 = myUserData.getCurrentHeight1();
        height2 = myUserData.getCurrentHeight2();
        weight = myUserData.getCurrentWeight();
        birth = myUserData.getCurrentBirthdate();
        fullName = myUserData.getCurrentFullName();
        gender = myUserData.getCurrentGender();
        Log.d(TAG, "User profile load "+
                " "+height1+
                " "+height2+
                " "+weight+
                " "+fullName+
                " "+gender+
                " "+birth);


        if(!fullName.equals("Error")){
            edName.setText(fullName);
        }
        Bitmap prof = myUserData.getCurUserPhoto();
        if(prof!=null){
            profilePic.setImageBitmap(prof);
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


        if(!gender.equals("Error")){
            mSpinner.setSelection(options.indexOf(gender));//set selected value in spinner
        }else{
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
        if(!height1.equals("Error")){
            mSpinnerHeight1.setSelection(height1Options.indexOf(height1));//set selected value in spinner
        }else{
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
        if(!height2.equals("Error")){
            mSpinnerHeight2.setSelection(height2Options.indexOf(height2));//set selected value in spinner
        }else{
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

        if(!weight.equals("Error")){
            mSpinnerWeight.setSelection(weightOptions.indexOf(weight));//set selected value in spinner
        }else{
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

        if(!birth.equals("Error")){
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTimeInMillis(Long.parseLong(birth));
            String birthDisplay = (birthCal.get(Calendar.MONTH)+1)+"-"+(birthCal.get(Calendar.MONTH)+1)+"-"+birthCal.get(Calendar.YEAR);
            birthdateButton.setText(birthDisplay);
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
                if(!birth.equals("Error")) {
                    Calendar birthCal = Calendar.getInstance();
                    birthCal.setTimeInMillis(Long.parseLong(birth));
                    dpd.updateDate(birthCal.get(Calendar.YEAR), birthCal.get(Calendar.MONTH), birthCal.get(Calendar.DAY_OF_MONTH));
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
                UserData myUserData = UserData.getUserData(c);
                myUserData.setCurName(fullName);
                myUserData.setCurHeight1(height1);
                myUserData.setCurHeight2(height2);
                myUserData.setCurWeight(weight);
                myUserData.setCurGender(gender);
                myUserData.setCurBirthdate(birth);
                myUserData.uploadToFirebase();

            }
        });

        profilePic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                Log.d(TAG, "Clicking profile picture");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        });
    }
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Log.d(TAG, "Recieved result intent from photo");
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

                    Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + user + "/metadata/profilepic");

                    DatabaseHelper mDbHelper = new DatabaseHelper(c);
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();

                    Calendar profile = Calendar.getInstance();
                    profile.setTimeInMillis(0); // user photos will be stored at the dawn of time.
//                    Date curDay = trim(new Date(profile.getTimeInMillis()));
                    ContentValues syncValues = new ContentValues();
                    syncValues.put(Database.PhotoStore.DATE, profile.getTimeInMillis());
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
                    Bitmap prof =UserData.getUserData(c).getCurUserPhoto();
//                     myUserData.getCurUserPhoto(c);
                    if(prof!=null){
                        profilePic.setImageBitmap(prof);
                    }
//                    ref.setValue(encodedImage);
                }
        }
    }
}
