package com.movo.wave;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.movo.wave.comms.BLEAgent;
import com.movo.wave.comms.WaveAgent;
import com.movo.wave.comms.WaveInfo;
import com.movo.wave.comms.WaveRequest;
import com.movo.wave.util.LazyLogger;
import com.movo.wave.util.NotificationPublisher;
import com.movo.wave.util.UTC;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by alex on 4/30/2015.
 */
public class SyncDataActivity extends MenuActivity {

    private SQLiteDatabase db;

    boolean destroyed = false;


    public static final LazyLogger lazyLog = new LazyLogger( "SyncDataActivity",
            MenuActivity.lazyLog );

    /*
    Note: We could parametrize DataSync to accept a max-delta comment
    */
    static long MAX_DATA_TIME_DELTA = 1000*60*60*24*7;
    static long MAX_SKEW_TIME_DELTA = 1000*60*10;

    private final WaveAgent.DataSync.Callback syncCallback = new WaveAgent.DataSync.Callback() {
        @Override
        public void notify(WaveAgent.DataSync sync, WaveAgent.DataSync.SyncState state, boolean status) {
            lazyLog.d( sync.info, " Sync: ", sync, " state: ", state, " status: ", status);
            updateSyncState( state.next() );
        }

        @Override
        public void notify(WaveAgent.DataSync sync, float progress) {
            updateSyncProgress( progress );
        }

        @Override
        public boolean skip(WaveAgent.DataSync sync, WaveAgent.DataSync.SyncState state) {
            final Long timeDelta;
            if( sync.localDate != null && sync.deviceDate != null ) {
                timeDelta = Math.abs(sync.localDate.getTime() - sync.deviceDate.getTime());
            } else {
                timeDelta = null;
            }

            final boolean ret;
            final long max;

            switch ( state ) {
                case REQUEST_DATA:
                    ret = timeDelta == null || timeDelta > MAX_DATA_TIME_DELTA;
                    lazyLog.i( "Time delta ", timeDelta, " exceeds ", MAX_DATA_TIME_DELTA );
                    break;
                case SET_DATE:
                    ret = timeDelta == null || timeDelta < MAX_SKEW_TIME_DELTA;
                    lazyLog.i( "Time delta ", timeDelta, " within tolerance ", MAX_SKEW_TIME_DELTA );
                    break;
                default:
                    ret = false;
                    break;
            }

            return ret;
        }

        @Override
        public void complete(WaveAgent.DataSync sync, List<WaveRequest.WaveDataPoint> data) {
            syncState.setText( resources.getString(R.string.sync_sql_save) );
            onSyncComplete(sync, data);
            updateSyncState(WaveAgent.DataSync.SyncState.COMPLETE);
            updateSyncProgress( 1.0f );
            db.close();
            db = null;
//            data=null;
            if( ! destroyed ) {
                if(data==null){
                    //sync failed
                    setContentView(R.layout.sync_finish);
                    RelativeLayout failed = (RelativeLayout) findViewById(R.id.movoBluetoothFailed);
                    failed.setVisibility(View.VISIBLE);
                    RelativeLayout succeed = (RelativeLayout) findViewById(R.id.movoBluetoothToggle);
                    succeed.setVisibility(View.GONE);
                    Button ok = (Button) findViewById(R.id.btnOk);
                    ok.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            finish();
                        }
                    });
                }else{
                    //sync succeed

                    setContentView(R.layout.sync_finish);
                    Button ok = (Button) findViewById(R.id.btnOk);
                    ok.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            finish();
                        }
                    });
                }
                BLEAgent.release();
                //startActivity( new Intent( c, Home.class ));
//
            }
        }
    };

//    public Cursor getNotUploadedSteps(){
//
//        String selectionSteps = Database.StepEntry.SYNC_ID + "=? OR " + Database.StepEntry.IS_PUSHED + "=?";
//        Cursor curSteps = db.query(
//                Database.StepEntry.STEPS_TABLE_NAME,  // The table to query
//                new String[]{Database.StepEntry.SYNC_ID, //blob
//                        Database.StepEntry.START, //int
//                        Database.StepEntry.END, //int
//                        Database.StepEntry.USER, //string
//                        Database.StepEntry.STEPS, //int
//                        Database.StepEntry.DEVICEID, //blob
//                        Database.StepEntry.GUID}, //blob                          // The columns to return
//                selectionSteps,                                // The columns for the WHERE clause
//                new String[]{syncID, "0"},                            // The values for the WHERE clause
//                null,                                     // don't group the rows
//                null,                                     // don't filter by row groups
//                null                                 // The sort order
//        );
//        curSteps.moveToFirst();
//        return curSteps;
//    }

    public  Cursor getStepsForSync(String syncID) {
        String selectionSteps = Database.StepEntry.IS_PUSHED + "=? AND "+ Database.StepEntry.USER +"=?";
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
                new String[]{"0", UserData.getUserData(c).getCurUID()},                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        curSteps.moveToFirst();
        return curSteps;
    }

    static long MAX_TIME_DELTA = 100*60*60*24*7;

    public void insertStepsIntoFirebase(List<WaveRequest.WaveDataPoint> points, String serialNumber, String syncId){

//                    Date curDate = new Date(stepTime);

//                    cal.setTimeZone();

//                    cal.set(Calendar.YEAR, curDate.getYear());
//                    cal.set(Calendar.MONTH, curDate.getMonth());
//                    cal.set(Calendar.DATE, curDate.getDate());
//                    cal.set(Calendar.HOUR_OF_DAY, curDate.getHours());
//                    cal.set(Calendar.MINUTE, curDate.getMinutes());
        for (WaveRequest.WaveDataPoint point : points) {

            if(point.value!=0) {
                //****************Date stuff*****************//
                Date stepTime = point.date;
                Long stepTimeLong = stepTime.getTime();
                Calendar cal = UTC.newCal();

                cal.setTimeInMillis(stepTimeLong);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                long THIRTY_MINUTES_IN_MILLIS = 1800000;//millisecs
                long endLong = point.date.getTime();
                endLong = endLong + THIRTY_MINUTES_IN_MILLIS;

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
                String endTime = UTC.isoFormatShort(endLong);

                //*******************************************//


                Map minuteMap = new HashMap<String, Map<String, String>>(); //minutes, steps
                Map<String, String> stepData = new HashMap<String, String>();
//                            stepData.put(Database.StepEntry.SYNC_ID, curSteps.getString(0));
                stepData.put(Database.StepEntry.START, startTime);
                stepData.put(Database.StepEntry.END, endTime);
//                            stepData.put(Database.StepEntry.USER, curSteps.getString(3));
                stepData.put(Database.StepEntry.STEPS, String.valueOf(point.value));
                stepData.put(Database.StepEntry.DEVICEID, serialNumber);


                minuteMap.put(startTime, stepData);


                Firebase refStep2 = new Firebase(UserData.firebase_url + "users/" + UserData.getUserData(c).getCurUID() + "/steps/" + (cal.get(Calendar.YEAR)) + "/" + monthChange + "/" + dayChange + "/" + syncId + "/");//to modify child node
                refStep2.updateChildren(minuteMap);

//            refStep2.setValue(minuteMap);
                Firebase refSyncSteps = new Firebase(UserData.firebase_url + "users/" + UserData.getUserData(c).getCurUID() + "/sync/" + syncId + "/steps/" + (cal.get(Calendar.YEAR)) + "/" + monthChange + "/" + dayChange + "/" + syncId + "/");//to modify child node
                refSyncSteps.updateChildren(minuteMap);

            }
//            ContentValues values = new ContentValues();
//            values.put(Database.StepEntry.STEPS, point.value);
//            values.put(Database.StepEntry.START, point.date.getTime());
//            values.put(Database.StepEntry.END,endLong);
        }

    }


    protected void onSyncComplete( WaveAgent.DataSync sync, List<WaveRequest.WaveDataPoint> data) {
        final String syncUniqueID = UUID.randomUUID().toString();
        final String currentUserId = originatingUserId;

        if (data != null) {
//            insertStepsIntoFirebase(data, sync.info.serial, syncUniqueID);
            final int result = insertPoints(db, syncUniqueID, currentUserId, data, sync.info.serial );

            lazyLog.d("Database insertion status: " + result);


            Date stop = new Date();

            scheduleSyncReminders();

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
//            db.close();
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
//            firebase upload sync
//                    UserData myData = UserData.getUserData(c);
            //sync ref
            Firebase ref = new Firebase(UserData.firebase_url + "users/" + cur.getString(3) + "/sync/" + cur.getString(0));


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

            insertStepsIntoFirebase(data, sync.info.serial, syncUniqueID);

//            Cursor curSteps = getStepsForSync(syncUniqueID);
//
//
//            Map<String, Map<String, Map<String, Object>>> monthMap = new HashMap<String, Map<String, Map<String, Object>>>(); //day<minutes,steps>>
////                    List<Object>[] monthList = new List[];
//            Map<String, Map<String, String>> minuteMap = new HashMap<String, Map<String, String>>(); //minutes, steps
//            Map<String, Map<String, String>> dayMap = new HashMap<String, Map<String, String>>(); //day<minutes,steps>>
//
//            Calendar cal = UTC.newCal();
//
//            ArrayList list = new ArrayList();
//            int date = -1;
//            int oldDate = -1;
//            String username = "";
//            while (curSteps.isAfterLast() == false) {
//                if (Integer.parseInt(curSteps.getString(4)) != 0) {
//                    username = curSteps.getString(3);
//
//
//                    long stepTime = Long.parseLong(curSteps.getString(2));
////
////                    Date curDate = new Date(stepTime);
//
//                    cal.setTimeInMillis(stepTime);
//
//                    cal.set(Calendar.SECOND, 0);
//                    cal.set(Calendar.MILLISECOND, 0);
////                    String dayMinute = (curDate.getMinutes() + (curDate.getHours() * 60)) + "";
//
//                    if ((date != cal.get(Calendar.DATE)) && (date != -1)) {
//                        String startTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(1)));
//                        String endTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(2)));
//                        oldDate = date;
//                        String monthChange = "";
//                        String dayChange = "";
//                        if((cal.get(Calendar.MONTH))<11){
//                            monthChange = "0"+(cal.get(Calendar.MONTH)+1);
//                        }else{
//                            monthChange = String.valueOf(cal.get(Calendar.MONTH)+1);
//                        }
//                        if((cal.get(Calendar.DATE))<11){
//                            dayChange = "0"+(cal.get(Calendar.DATE)+1);
//                        }else{
//                            dayChange = String.valueOf(cal.get(Calendar.DATE)+1);
//                        }
//                        Firebase refStep2 = new Firebase(UserData.firebase_url + "users/" + curSteps.getString(3) + "/steps/" + (cal.get(Calendar.YEAR)) + "/" + monthChange + "/" + dayChange).child(curSteps.getString(0)); //to modify child node
//                        refStep2.setValue(minuteMap, new Firebase.CompletionListener() {
//                            @Override
//                            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
//                                if (firebaseError != null) {
//                                    System.out.println("Step Data could not be saved. " + firebaseError.getMessage());
//                                } else {
//                                    System.out.println("Step Data saved successfully.");
//
//                                }
//                            }
//                        });
//                        Firebase refSyncSteps =  new Firebase(UserData.firebase_url + "users/" + curSteps.getString(3) + "/sync/"+syncUniqueID+"/steps/" + (cal.get(Calendar.YEAR)) + "/" + monthChange + "/" + dayChange).child(curSteps.getString(0));
//                        refSyncSteps.setValue(minuteMap);
//
//                        minuteMap = new HashMap<String, Map<String, String>>(); //minutes, steps
//                        Map<String, String> stepData = new HashMap<String, String>();
////                            stepData.put(Database.StepEntry.SYNC_ID, curSteps.getString(0));
//                        stepData.put(Database.StepEntry.START, startTime);
//                        stepData.put(Database.StepEntry.END, endTime);
////                            stepData.put(Database.StepEntry.USER, curSteps.getString(3));
//                        stepData.put(Database.StepEntry.STEPS, curSteps.getString(4));
//                        stepData.put(Database.StepEntry.DEVICEID, curSteps.getString(5));
//
//
//                        minuteMap.put(startTime, stepData);
//
//                        date = cal.get(Calendar.DATE);
//
//                    //this else block is the 1st case scenario, init minutemap and move on
//                    } else {
//                        //if steps is not 0 add to map
//
//                        if (Integer.parseInt(curSteps.getString(4)) != 0) {
//                            oldDate = date;
//                            String startTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(1)));
//                            String endTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(2)));
//                            Map<String, String> stepData = new HashMap<String, String>();
//    //                        stepData.put(Database.StepEntry.SYNC_ID, curSteps.getString(0));
//                            stepData.put(Database.StepEntry.START, startTime);
//                            stepData.put(Database.StepEntry.END, endTime);
//    //                            stepData.put(Database.StepEntry.USER, curSteps.getString(3));
//                            stepData.put(Database.StepEntry.STEPS, curSteps.getString(4));
//                            stepData.put(Database.StepEntry.DEVICEID, curSteps.getString(5));
//
//
//
//                            minuteMap.put(startTime, stepData);
//                            date = cal.get(Calendar.DATE);
//                        }
//
//
//                    }
//                }
//
//
////                        Firebase refStep2 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +curSteps.getString(3) + "/steps/"+curDate.getYear()+"/"+curDate.getMonth()+"/"+curDate.getDate());
//////                            refStep2.updateChildren( minuteMap);
////                        list.add(curDate.getDate()+"",stepData);
//
////
////
//
//                curSteps.moveToNext();
//
//            }
//            try {
//                curSteps.moveToLast();
//                long stepTime = Long.parseLong(curSteps.getString(2));
////
////                    Date curDate = new Date(stepTime);
//
//                cal.setTimeInMillis(stepTime);
////                    cal.setTimeZone();
//
////                    cal.set(Calendar.YEAR, curDate.getYear());
////                    cal.set(Calendar.MONTH, curDate.getMonth());
////                    cal.set(Calendar.DATE, curDate.getDate());
////                    cal.set(Calendar.HOUR_OF_DAY, curDate.getHours());
////                    cal.set(Calendar.MINUTE, curDate.getMinutes());
//                cal.set(Calendar.SECOND, 0);
//                cal.set(Calendar.MILLISECOND, 0);
//
//                String monthChange = "";
//                String dayChange = "";
//                if((cal.get(Calendar.MONTH))<11){
//                    monthChange = "0"+(cal.get(Calendar.MONTH)+1);
//                }else{
//                    monthChange = String.valueOf(cal.get(Calendar.MONTH)+1);
//                }
//                if((cal.get(Calendar.DATE))<11){
//                    dayChange = "0"+(cal.get(Calendar.DATE)+1);
//                }else{
//                    dayChange = String.valueOf(cal.get(Calendar.DATE)+1);
//                }
//
//                Firebase refStep2 = new Firebase(UserData.firebase_url + "users/" + curSteps.getString(3) + "/steps/" + (cal.get(Calendar.YEAR)) + "/" + monthChange + "/" + dayChange).child(curSteps.getString(0)); //to modify child node
//                refStep2.setValue(minuteMap);
//                Firebase refSyncSteps =  new Firebase(UserData.firebase_url + "users/" + curSteps.getString(3) + "/sync/"+syncUniqueID+"/steps/" + (cal.get(Calendar.YEAR)) + "/" + monthChange + "/" + dayChange).child(curSteps.getString(0));
//                refSyncSteps.setValue(minuteMap);
//
////                    refStep.setValue(list);
//            } catch (Exception e) {
//                e.printStackTrace();
//                lazyLog.d("No new entries to upload");
//                Toast.makeText(c, "No new steps to add.", Toast.LENGTH_SHORT).show();
//            }
//            curSteps.close();
        } else {
            lazyLog.d("SYNC FAILED!" + sync);
        }
    }

    private static boolean insertPoint( final SQLiteDatabase db,
                                        final String guid,
                                        final String userID,
                                        final WaveRequest.WaveDataPoint point,
                                        final String deviceSerial) {

        long THIRTY_MINUTES_IN_MILLIS =1800000;//millisecs
        final long startLong = point.date.getTime();
        final long endLong = startLong + THIRTY_MINUTES_IN_MILLIS;

        ContentValues values = new ContentValues();
        values.put(Database.StepEntry.GUID, UUID.randomUUID().toString());
        values.put(Database.StepEntry.STEPS, point.value);
        values.put(Database.StepEntry.START, startLong);
        values.put(Database.StepEntry.END,endLong);
        values.put(Database.StepEntry.USER,userID);
        values.put(Database.StepEntry.IS_PUSHED, 0);
        values.put(Database.StepEntry.SYNC_ID, guid);
        values.put(Database.StepEntry.DEVICEID, deviceSerial);
//        values.put(Database.StepEntry.WORKOUT_TYPE, point.Mode.);
        //TODO: add workout type

        long newRowId;

        newRowId = db.insert(Database.StepEntry.STEPS_TABLE_NAME,
                null,
                values);


        boolean ret = newRowId >= 0;
        if (ret) {
            lazyLog.d( "Inserted into database: new row " + newRowId + " guid: " + guid);
            lazyLog.d( "Inserted data: " + point);
        } else {
            /* AH 20150522
            I know this is a bit of a copy-pasta mess. Intent is to check and smart-replace
            conflicting rows.
             */

            //Locate conflicting row
            String selectionSteps =  Database.StepEntry.START + "=? AND "+Database.StepEntry.DEVICEID +"=?";
            Cursor curSteps = db.query(
                    Database.StepEntry.STEPS_TABLE_NAME,  // The table to query
                    new String[] {
                            Database.StepEntry.GUID, //string
                            Database.StepEntry.SYNC_ID, //blob
                            Database.StepEntry.START, //int
                            Database.StepEntry.END, //int
                            Database.StepEntry.USER, //string
                            Database.StepEntry.STEPS, //int
                            Database.StepEntry.DEVICEID, //blob
                            Database.StepEntry.GUID}, //blob                          // The columns to return
                    selectionSteps,                                // The columns for the WHERE clause
                    new String[] { String.valueOf(startLong), deviceSerial }, // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                 // The sort order
            );

            if( curSteps.moveToFirst() ) {
                // compare data if conflict exists
                final int stepIndex = curSteps.getColumnIndex(Database.StepEntry.STEPS );
                final long steps = curSteps.getLong( stepIndex );
                final int idIndex = curSteps.getColumnIndex( Database.StepEntry.GUID );
                final int syncIndex = curSteps.getColumnIndex(Database.StepEntry.SYNC_ID);

                if( steps < point.value ) {
                    //overwrite conflict
                    lazyLog.i( "Replacing ", curSteps.getString(idIndex), " from ",
                            curSteps.getString( syncIndex ), " (", steps, " < ", point.value,
                            ") with ", values.getAsString( Database.StepEntry.GUID), " from ", guid);
                    newRowId = db.replace( Database.StepEntry.STEPS_TABLE_NAME, null, values );
                    ret = newRowId >= 0;
                    lazyLog.a( ret, "Failed to replace row!");
                } else {
                    //ignore conflict
                    lazyLog.i( "Keeping ", curSteps.getString(idIndex), " from ",
                            curSteps.getString( syncIndex ), " (", steps, " < ", point.value,
                            ") with ", values.getAsString( Database.StepEntry.GUID), " from ", guid);
                }
            } else {
                lazyLog.e( "Insertion conflict without conflicting row! ", startLong, " ", deviceSerial);
            }
            
            //close cursor
            curSteps.close();
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
    Button syncCancel;

    Resources resources;

    private void updateSyncState( WaveAgent.DataSync.SyncState state ) {
        String[] enumText = resources.getStringArray( R.array.sync_state_enum );
        int ordinal = state.ordinal();
        syncState.setText( enumText[ ordinal ] + "\u2026");
    }

    private void updateSyncProgress( float progress ) {
        int percent = (int)(100.0f * progress);
        syncProgress.setProgress(percent);
        syncPercent.setText( percent + "%");
    }

    WaveInfo info;
    String originatingUserId;

    final static String EXTRA_WAVE_MAC = "com.movo.wave.SyncDataActivity::EXTRA_MAC";
    final static String EXTRA_WAVE_USER_ID = "com.movo.wave.SyncDataActivity::EXTRA_USER_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMenu(R.layout.activity_sync_data);
        resources = getResources();

        syncProgress = (ProgressBar) findViewById( R.id.syncProgress );
        syncState = (TextView) findViewById( R.id.syncState );
        syncPercent = (TextView) findViewById(R.id.syncPercent);
        TextView syncSerial = (TextView) findViewById( R.id.syncSerial );
        syncCancel = (Button) findViewById( R.id.syncCancel );
        syncCancel.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( sync != null ) {
                    sync.abort();
                }
                lazyLog.i( "User aborted via click");
                finish();
            }
        });

        if( ! bleEnabled() )
            requestBLEEnabled();

        final boolean status = BLEAgent.open(c);
        lazyLog.i( "Opened BLE agent with status ", status);

        DatabaseHelper mDbHelper = new DatabaseHelper(c);
        db = mDbHelper.getWritableDatabase();
        start = new Date();

        Intent intent = getIntent();

        final String mac = intent.getStringExtra( EXTRA_WAVE_MAC );
        originatingUserId = intent.getStringExtra( EXTRA_WAVE_USER_ID );
        lazyLog.i( "Starting sync with MAC ", mac, " for user ", originatingUserId );
        if( originatingUserId == null ) {
            lazyLog.e( "Null user provided, aborting sync...");
            Toast.makeText(c, "Error, no user provided, aborting sync....", Toast.LENGTH_LONG);
            finish();
        } else {
            info = new WaveInfo(db, mac);
            syncSerial.setText(resources.getString(R.string.sync_serial_label) + info.serial);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( bleEnabled() && info != null && sync == null ) {

            syncCancel.setVisibility( View.INVISIBLE );
            syncCancel.setEnabled(false);

            sync = WaveAgent.DataSync.byInfo( 10000, info, syncCallback );
            updateSyncProgress( 0 );
            updateSyncState( sync.getState() );
            info = null;
        }
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

    public void scheduleSyncReminders(){
       long twoDay = TimeUnit.DAYS.toMillis(2);     // 1 day to milliseconds.
        long fourDay = twoDay * 2;
        long sixDay = twoDay * 3;
        long sevenDay = TimeUnit.DAYS.toMillis(7);
        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(),"Sync your Wave to find out how far you've come"), (int)twoDay,0 );
        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(),"Don't forget to sync and update your Movo calendar."), (int)fourDay,1 );
        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(),"Where have your steps taken you? Sync your Wave now"), (int)sixDay,2 );
        scheduleNotification(getNotification(UserData.getUserData(c).getCurrentUsername(),"You will lose data if you do not sync at least once a week."), (int)sevenDay,3 );
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
        builder.setContentText("Remember to Sync.");
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        builder.setSmallIcon(R.drawable.app_icon);
        return builder.build();
    }
}
