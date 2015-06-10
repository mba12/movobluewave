package com.movo.wave.util;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.movo.wave.Database;
import com.movo.wave.DatabaseHelper;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by P on 5/29/2015.
 */
public class DataUtilities {
    public static String TAG = "DataUtilities";

    public static String getMD5EncryptedString(String encTarget){
        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception while encrypting to md5");
            e.printStackTrace();
        } // Encryption algorithm
        mdEnc.update(encTarget.getBytes(), 0, encTarget.length());
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        while ( md5.length() < 32 ) {
            md5 = "0"+md5;
        }
        return md5;
    }

    public static boolean uploadPhotoToFB(Firebase ref, String encodedImage){

        ref.setValue(null);

        if(encodedImage.length()>1000000) {
            //multipart
            List<String> strings = new ArrayList<String>();
            int index = 0;
            while (index < encodedImage.length()) {

                strings.add(encodedImage.substring(index, Math.min(index + 1000000, encodedImage.length())));
                index += 1000000;
            }


            Log.d(TAG, "Starting image upload " + ref);
            ref.child(""+0).setValue(strings.size()+"");
            for(int i = 0;i<strings.size();i++){
                ref.child(""+(i+2)).setValue(strings.get(i), new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if (firebaseError != null) {
                            Log.i(firebaseError.toString(), firebaseError.toString());
                        }
                    }

                });
                Log.d(TAG, "Image upload progress "+i+" "+ref.child(""+i));
            }

            ref.child(""+1).setValue(DataUtilities.getMD5EncryptedString(encodedImage));
        }else{
            ref.child(""+0).setValue(1+"");

            ref.child(""+2).setValue(encodedImage);
            ref.child(""+1).setValue(DataUtilities.getMD5EncryptedString(encodedImage));
        }
        return true;
    }
    public static Date trim(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.clear(); // as per BalusC comment.
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

}
