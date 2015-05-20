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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
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
                BLEAgent.close();
                //startActivity( new Intent( c, Home.class ));
//
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

    /*
    Note: We could parametrize DataSync to accept a max-delta comment
     */
    static long MAX_TIME_DELTA = 100*60*60*24*7;

    protected void onSyncComplete( WaveAgent.DataSync sync, List<WaveRequest.WaveDataPoint> data) {
        final String syncUniqueID = UUID.randomUUID().toString();
        final String currentUserId = sync.info.user;

        final Long timeDelta;
        if( sync.localDate != null && sync.deviceDate != null ) {
            timeDelta = sync.localDate.getTime() - sync.deviceDate.getTime();
        } else {
            timeDelta = null;
        }

        if( timeDelta != null && ( timeDelta ) > MAX_TIME_DELTA ) {
            lazyLog.w("Time delta ", timeDelta, " exceeds ", MAX_TIME_DELTA, " Ignoring data!");
        } else if (data != null) {

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
            Firebase ref = new Firebase(Home.firebase_url + "users/" + cur.getString(3) + "/sync/" + cur.getString(0));


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
                        Firebase refStep2 = new Firebase(Home.firebase_url + "users/" + curSteps.getString(3) + "/steps/" + (curDate.getYear() + 1900) + "/" + (curDate.getMonth() + 1) + "/" + oldDate).child(curSteps.getString(0)); //to modify child node
                        refStep2.setValue(minuteMap);
                        Firebase refSyncSteps =  new Firebase(Home.firebase_url + "users/" + curSteps.getString(3) + "/sync/"+syncUniqueID+"/steps/" + (curDate.getYear() + 1900) + "/" + (curDate.getMonth() + 1) + "/" + oldDate).child(curSteps.getString(0));
                        refSyncSteps.setValue(minuteMap);

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


                Firebase refStep2 = new Firebase(Home.firebase_url + "users/" + curSteps.getString(3) + "/steps/" + (curDate.getYear() + 1900) + "/" + (curDate.getMonth() + 1) + "/" + oldDate).child(curSteps.getString(0));
                refStep2.setValue(minuteMap);
                Firebase refSyncSteps =  new Firebase(Home.firebase_url + "users/" + curSteps.getString(3) + "/sync/"+syncUniqueID+"/steps/" + (curDate.getYear() + 1900) + "/" + (curDate.getMonth() + 1) + "/" + oldDate).child(curSteps.getString(0));
                refSyncSteps.setValue(minuteMap);

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


        final boolean status = BLEAgent.open(c);
        lazyLog.i( "Opened BLE agent with status ", status);

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
