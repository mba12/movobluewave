package com.movo.wave.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.movo.wave.Database;
import com.movo.wave.UserData;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alex on 12/15/2015.
 */
public class FirebaseSync {

    final static public LazyLogger lazyLog = new LazyLogger( "FirebaseSync", true );

    static public void insertStepsIntoFirebase(Context c, final String curUser, String syncId){

        final DatabaseHandle dbHandle = new DatabaseHandle(c);
        dbHandle.acquire();

        Cursor cur =  UserData.getUserData(c).getStepsToUpload(dbHandle.db, curUser);


        while(cur.moveToNext()) {
            final ContentValues localValues = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(cur, localValues);

            final String deviceSerial = localValues.getAsString(Database.StepEntry.DEVICEID);

            Map<String, Object> stepMap = new HashMap<String, Object>();
            stepMap.put(Database.StepEntry.START, UTC.isoFormatShort(localValues.getAsLong(Database.StepEntry.START)));
            stepMap.put(Database.StepEntry.END, UTC.isoFormatShort(localValues.getAsLong(Database.StepEntry.END)));
            stepMap.put(Database.StepEntry.STEPS, localValues.getAsString(Database.StepEntry.STEPS));
            stepMap.put(Database.StepEntry.DEVICEID, localValues.getAsString(Database.StepEntry.DEVICEID) );
            stepMap.put(Database.StepEntry.SYNC_ID, localValues.getAsString(Database.StepEntry.SYNC_ID ) );

            Long stepTimeLong = localValues.getAsLong(Database.StepEntry.START);
//        Long stepTimeLong = stepTime.getTime();
            Calendar cal = UTC.newCal();
            cal.setTimeInMillis(stepTimeLong);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
//            long endLong = Long.parseLong(cur.getString(2));
            String monthChange = "";
            String dayChange = "";
            if ((cal.get(Calendar.MONTH)) < 11) {
                monthChange = "0" + (cal.get(Calendar.MONTH) + 1);
            } else {
                monthChange = String.valueOf(cal.get(Calendar.MONTH) + 1);
            }
            if ((cal.get(Calendar.DATE)) < 10) {
                dayChange = "0" + (cal.get(Calendar.DATE));
            } else {
                dayChange = String.valueOf(cal.get(Calendar.DATE));
            }
            String startTime = UTC.isoFormatShort(stepTimeLong);
//            String endTime = UTC.isoFormatShort(endLong);

            Map minuteMap = new HashMap<String, Map<String, String>>(); //minutes, steps

            minuteMap.put(deviceSerial, stepMap);

            dbHandle.acquire();



            //Implement barrier to only mark synced locally when both FB calls complete successfully.
            final Firebase.CompletionListener listener = new Firebase.CompletionListener() {

                int callCount = 0;

                @Override
                public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                    if (firebaseError != null) {
                        lazyLog.w("Data could not be saved. " + firebaseError.getMessage());
                    } else if( (callCount += 1) == 2  ) {
                        lazyLog.d("Data saved successfully.");

                        localValues.put( Database.StepEntry.IS_PUSHED, "1");
                        final long row = dbHandle.db.replace( Database.StepEntry.STEPS_TABLE_NAME, null, localValues );
                        if( row < 0 ) {
                            lazyLog.e( "Can't mark step entry uploaded in database!!!!! ", row, " ", curUser, " ", localValues.getAsString( Database.StepEntry.START));
                        }
                    } else {
                        lazyLog.d( "Data save pending next invocation ");
                    }
                    dbHandle.release();
                }
            };

            // push data to firebase
            Firebase refStep2 = new Firebase(UserData.firebase_url + "users/" + curUser + "/steps/" + (cal.get(Calendar.YEAR)) + "/" + monthChange + "/" + dayChange + "/" +startTime +"/");//to modify child node
            refStep2.updateChildren(minuteMap, listener );

            //
            Firebase refSyncSteps = new Firebase(UserData.firebase_url + "users/" + curUser + "/sync/" + syncId + "/steps/" + (cal.get(Calendar.YEAR)) + "/" + monthChange + "/" + dayChange + "/" + startTime + "/");//to modify child node
            refSyncSteps.updateChildren(minuteMap, listener);
        }

        cur.close();
        dbHandle.release();
    }
}
