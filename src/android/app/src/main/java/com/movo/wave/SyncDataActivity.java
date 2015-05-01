package com.movo.wave;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.movo.wave.comms.WaveAgent;
import com.movo.wave.comms.WaveInfo;
import com.movo.wave.comms.WaveRequest;
import com.movo.wave.util.LazyLogger;
import com.movo.wave.util.UTC;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by alex on 4/30/2015.
 */
public class SyncDataActivity extends MenuActivity {

    private SQLiteDatabase db;

    boolean destroyed = false;


    public static final LazyLogger lazyLog = new LazyLogger( "SyncDataActivity",
            MenuActivity.lazyLog );

    private final WaveAgent.DataSync.Callback syncCallback = new WaveAgent.DataSync.Callback() {
        @Override
        public void notify(WaveAgent.DataSync sync, WaveAgent.DataSync.SyncState state, boolean status) {
            lazyLog.d( sync.info, " Sync: ", sync, " state: ", state, " status: ", status);
            updateSyncState( state );
        }

        @Override
        public void notify(WaveAgent.DataSync sync, float progress) {
            updateSyncProgress( progress );
        }

        @Override
        public void complete(WaveAgent.DataSync sync, List<WaveRequest.WaveDataPoint> data) {
            syncState.setText( resources.getString(R.string.sync_sql_save) );
            onSyncComplete(sync, data);
            updateSyncState(WaveAgent.DataSync.SyncState.COMPLETE);
            updateSyncProgress( 1.0f );
            db.close();
            db = null;
            if( ! destroyed ) {
                //startActivity( new Intent( c, Home.class ));
                finish();
            }
        }
    };

    public  Cursor getStepsForSync(String syncID){
        String selectionSteps =  Database.StepEntry.SYNC_ID + "=? AND "+Database.StepEntry.IS_PUSHED +"=?";
        Cursor curSteps = db.query(
                Database.StepEntry.STEPS_TABLE_NAME,  // The table to query
                new String[] { Database.StepEntry.SYNC_ID, //blob
                        Database.StepEntry.START, //int
                        Database.StepEntry.END, //int
                        Database.StepEntry.USER, //string
                        Database.StepEntry.STEPS, //int
                        Database.StepEntry.DEVICEID, //blob
                        Database.StepEntry.GUID}, //blob                          // The columns to return
                selectionSteps,                                // The columns for the WHERE clause
                new String[] { syncID, "0" },                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        curSteps.moveToFirst();
        return curSteps;
    }

    protected void onSyncComplete( WaveAgent.DataSync sync, List<WaveRequest.WaveDataPoint> data) {
        final String syncUniqueID = UUID.randomUUID().toString();
        final String currentUserId = sync.info.user;
        if (data != null) {

            final int result = insertPoints(db, syncUniqueID, currentUserId, data, sync.device.device.getAddress());

            lazyLog.d("Database insertion status: " + result);


            Date stop = new Date();

            ContentValues syncValues = new ContentValues();
            syncValues.put(Database.SyncEntry.GUID, syncUniqueID);
            syncValues.put(Database.SyncEntry.SYNC_START, start.getTime());
            syncValues.put(Database.SyncEntry.SYNC_END, stop.getTime());
            syncValues.put(Database.SyncEntry.USER, currentUserId);
            syncValues.put(Database.SyncEntry.STATUS, 0);
            long newRowId;
            newRowId = db.insert(Database.SyncEntry.SYNC_TABLE_NAME,
                    null,
                    syncValues);
            lazyLog.d("Sync database add:\n" + syncValues.toString());

//                    FirebaseCalls fbc = new FirebaseCalls(c);
//                    fbc.uploadSync(syncUniqueID);

            String selection = Database.SyncEntry.GUID + "=?";
            ContentValues valuesRead = new ContentValues();
            Cursor cur = db.query(
                    Database.SyncEntry.SYNC_TABLE_NAME,  // The table to query
                    new String[]{Database.SyncEntry.GUID, //blob
                            Database.SyncEntry.SYNC_START, //int
                            Database.SyncEntry.SYNC_END, //int
                            Database.SyncEntry.USER, //string
                            Database.SyncEntry.STATUS}, //bool                          // The columns to return
                    selection,                                // The columns for the WHERE clause
                    new String[]{syncUniqueID},                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                 // The sort order
            );

            cur.moveToFirst();
            //start
            long itemId = cur.getLong(
                    cur.getColumnIndexOrThrow(Database.SyncEntry.GUID)

            );
            //firebase upload sync
//                    UserData myData = UserData.getUserData(c);
            //sync ref
            Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + cur.getString(3) + "/sync/" + cur.getString(0));


            Map<String, Object> syncData = new HashMap<String, Object>();
//                    syncData.put(Database.SyncEntry.GUID, cur.getString(0));
            syncData.put(Database.SyncEntry.SYNC_START, UTC.isoFormat(Long.parseLong(cur.getString(1))));
            syncData.put(Database.SyncEntry.SYNC_END, UTC.isoFormat(Long.parseLong(cur.getString(2))));
            syncData.put(Database.SyncEntry.USER, cur.getString(3));
            syncData.put(Database.SyncEntry.STATUS, cur.getString(4));

            lazyLog.d("Sync ID is " + cur.getString(0));
            ref.setValue(syncData);
            cur.close();
            //*****************steps***********************//
            Cursor curSteps = getStepsForSync(syncUniqueID);


            Map<String, Map<String, Map<String, Object>>> monthMap = new HashMap<String, Map<String, Map<String, Object>>>(); //day<minutes,steps>>
//                    List<Object>[] monthList = new List[];
            Map<String, Map<String, String>> minuteMap = new HashMap<String, Map<String, String>>(); //minutes, steps
            Map<String, Map<String, String>> dayMap = new HashMap<String, Map<String, String>>(); //day<minutes,steps>>

            Calendar cal = UTC.newCal();

            ArrayList list = new ArrayList();
            int date = -1;
            int oldDate = -1;
            String username = "";
            while (curSteps.isAfterLast() == false) {
                if (Integer.parseInt(curSteps.getString(4)) != 0) {
                    username = curSteps.getString(3);


                    long stepTime = Long.parseLong(curSteps.getString(2));
                    Date curDate = new Date(stepTime);

                    cal.set(Calendar.YEAR, 2015);
                    cal.set(Calendar.MONTH, curDate.getMonth());
                    cal.set(Calendar.DATE, curDate.getDate());
                    cal.set(Calendar.HOUR_OF_DAY, curDate.getHours());
                    cal.set(Calendar.MINUTE, curDate.getMinutes());
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    String dayMinute = (curDate.getMinutes() + (curDate.getHours() * 60)) + "";

                    if ((date != curDate.getDate()) && (date != -1)) {
                        String startTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(1)));
                        String endTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(2)));
                        oldDate = date;
                        Firebase refStep2 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + curSteps.getString(3) + "/steps/" + (curDate.getYear() + 1900) + "/" + (curDate.getMonth() + 1) + "/" + oldDate).child(curSteps.getString(0)); //to modify child node
                        refStep2.setValue(minuteMap);


                        minuteMap = new HashMap<String, Map<String, String>>(); //minutes, steps
                        Map<String, String> stepData = new HashMap<String, String>();
//                            stepData.put(Database.StepEntry.SYNC_ID, curSteps.getString(0));
                        stepData.put(Database.StepEntry.START, startTime);
                        stepData.put(Database.StepEntry.END, endTime);
//                            stepData.put(Database.StepEntry.USER, curSteps.getString(3));
                        stepData.put(Database.StepEntry.STEPS, curSteps.getString(4));
                        stepData.put(Database.StepEntry.DEVICEID, curSteps.getString(5));


                        minuteMap.put(startTime, stepData);

                        date = curDate.getDate();

                    } else {
                        oldDate = date;
                        String startTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(1)));
                        String endTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(2)));
                        Map<String, String> stepData = new HashMap<String, String>();
                        stepData.put(Database.StepEntry.SYNC_ID, curSteps.getString(0));
                        stepData.put(Database.StepEntry.START, startTime);
                        stepData.put(Database.StepEntry.END, endTime);
//                            stepData.put(Database.StepEntry.USER, curSteps.getString(3));
                        stepData.put(Database.StepEntry.STEPS, curSteps.getString(4));
                        stepData.put(Database.StepEntry.DEVICEID, curSteps.getString(5));

                        if (Integer.parseInt(curSteps.getString(4)) != 0) {
                            minuteMap.put(startTime, stepData);
                        }

                        date = curDate.getDate();
                    }
                }


//                        Firebase refStep2 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +curSteps.getString(3) + "/steps/"+curDate.getYear()+"/"+curDate.getMonth()+"/"+curDate.getDate());
////                            refStep2.updateChildren( minuteMap);
//                        list.add(curDate.getDate()+"",stepData);

//
//

                curSteps.moveToNext();

            }
            try {
                curSteps.moveToLast();
                long stepTime = Long.parseLong(curSteps.getString(2));
                Date curDate = new Date(stepTime);

                String startTime = UTC.isoFormat(Long.parseLong(curSteps.getString(1)));
                String endTime = UTC.isoFormat(Long.parseLong(curSteps.getString(2)));


                cal.set(Calendar.YEAR, 2015);
                cal.set(Calendar.MONTH, curDate.getMonth());
                cal.set(Calendar.DATE, curDate.getDate());
                cal.set(Calendar.HOUR_OF_DAY, curDate.getHours());
                cal.set(Calendar.MINUTE, curDate.getMinutes());
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                String dayMinute = (curDate.getMinutes() + (curDate.getHours() * 60)) + "";


                Firebase refStep2 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" + curSteps.getString(3) + "/steps/" + (curDate.getYear() + 1900) + "/" + (curDate.getMonth() + 1) + "/" + oldDate).child(curSteps.getString(0));
                refStep2.setValue(minuteMap);
//                    refStep.setValue(list);
            } catch (Exception e) {
                e.printStackTrace();
                lazyLog.d("No new entries to upload");
                Toast.makeText(c, "No new steps to add.", Toast.LENGTH_SHORT).show();
            }
            curSteps.close();
        } else {
            lazyLog.d("SYNC FAILED!" + sync);
        }
    }

    private static boolean insertPoint( final SQLiteDatabase db,
                                        final String guid,
                                        final String userID,
                                        final WaveRequest.WaveDataPoint point,
                                        final String deviceAddress) {

        long TWO_MINUTES_IN_MILLIS=120000;//millisecs
        long endLong = point.date.getTime();
        endLong = endLong + TWO_MINUTES_IN_MILLIS;

        ContentValues values = new ContentValues();
        values.put(Database.StepEntry.GUID, UUID.randomUUID().toString());
        values.put(Database.StepEntry.STEPS, point.value);
        values.put(Database.StepEntry.START, point.date.getTime());
        values.put(Database.StepEntry.END,endLong);
        values.put(Database.StepEntry.USER,userID);
        values.put(Database.StepEntry.IS_PUSHED, 0);
        values.put(Database.StepEntry.SYNC_ID, guid);
        values.put(Database.StepEntry.DEVICEID, deviceAddress);
//        values.put(Database.StepEntry.WORKOUT_TYPE, point.Mode.);
        //TODO: add workout type

        long newRowId;

        newRowId = db.insert(Database.StepEntry.STEPS_TABLE_NAME,
                null,
                values);


        final boolean ret = newRowId >= 0;
        if (ret) {
            lazyLog.d( "Inserted into database: new row " + newRowId + " guid: " + guid);
            lazyLog.d( "Inserted data: " + point);
        }
        return ret;

    }


    public static int insertPoints( final SQLiteDatabase db,
                                    final String guid,
                                    final String userID,
                                    Collection<WaveRequest.WaveDataPoint> points,
                                    final String deviceAddress) {
        //http://www.vogella.com/tutorials/AndroidSQLite/article.html


        db.beginTransaction();
        boolean success = false;
        int ret = 0;
        int skippedForZero = 0;
        try {
            for (WaveRequest.WaveDataPoint point : points) {
                if(point.value!=0) {
                    if (insertPoint(db, guid, userID, point, deviceAddress)) {
                        ret += 1;
                    }
                }else{
                    skippedForZero += 1;

                }
            }
            lazyLog.d( "Skipped " + skippedForZero + " objects with 0 steps.");
            db.setTransactionSuccessful();
            success = true;
        } finally {
            db.endTransaction();
        }
        return success ? ret : -1;
    }

    Date start;

    WaveAgent.DataSync sync;
    ProgressBar syncProgress;
    TextView syncState;
    TextView syncPercent;

    Resources resources;

    private void updateSyncState( WaveAgent.DataSync.SyncState state ) {
        String[] enumText = resources.getStringArray( R.array.sync_state_enum );
        syncState.setText( enumText[ state.ordinal() ] + "\u2026");
    }

    private void updateSyncProgress( float progress ) {
        int percent = (int)(100.0f * progress);
        syncProgress.setProgress(percent);
        syncPercent.setText( percent + "%");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMenu(R.layout.activity_sync_data);
        resources = getResources();

        syncProgress = (ProgressBar) findViewById( R.id.syncProgress );
        syncState = (TextView) findViewById( R.id.syncState );
        syncPercent = (TextView) findViewById(R.id.syncPercent);
        TextView syncSerial = (TextView) findViewById( R.id.syncSerial );


        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        db = mDbHelper.getWritableDatabase();
        start = new Date();

        Intent intent = getIntent();

        final String mac = intent.getStringExtra( "MAC" );
        lazyLog.i( "Starting sync with MAC ", mac );
        final WaveInfo info = new WaveInfo( db, mac );

        syncSerial.setText( resources.getString(R.string.sync_serial_label) + info.serial );
        sync = WaveAgent.DataSync.byInfo( 10000, info, syncCallback );
        updateSyncProgress( 0 );
        updateSyncState( sync.getState() );
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
    }
}