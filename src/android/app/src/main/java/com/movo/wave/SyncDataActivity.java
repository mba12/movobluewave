package com.movo.wave;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.movo.wave.comms.BLEAgent;
import com.movo.wave.comms.WaveAgent;
import com.movo.wave.comms.WaveInfo;
import com.movo.wave.comms.WaveRequest;
import com.movo.wave.util.LazyLogger;
import com.movo.wave.util.UTC;

import java.util.ArrayList;
import java.util.Calendar;
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

    ArrayList<WaveInfo> waves;
    ArrayAdapter<WaveInfo> arrayAdapter;
    ListView waveList;

    private final HashMap<WaveInfo,WaveAgent.DataSync> syncMap = new HashMap<>();


    public static final LazyLogger lazyLog = new LazyLogger( "UploadDataActivity",
            MenuActivity.lazyLog );

    private final WaveAgent.DataSync.Callback syncCallback = new WaveAgent.DataSync.Callback() {
        @Override
        public void notify(WaveAgent.DataSync sync, WaveAgent.DataSync.SyncState state, boolean status) {
            lazyLog.d( "Sync: ", sync, " state: ", state, " status: ", status);
            if( status = false ) {
                syncMap.remove(sync.info);
            }
        }

        @Override
        public void notify(WaveAgent.DataSync sync, float progress) {

        }

        @Override
        public void complete(WaveAgent.DataSync sync, List<WaveRequest.WaveDataPoint> data) {
            syncMap.remove(sync.info);

            onSyncComplete( sync, data );
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
        final Date start = new Date(); //FIXME!
        if( data != null ) {

            final int result = Home.insertPoints( db, syncUniqueID, currentUserId, data, sync.device.device.getAddress());

            lazyLog.d( "Database insertion status: " + result);


            Date stop = new Date();

            ContentValues syncValues = new ContentValues();
            syncValues.put(Database.SyncEntry.GUID, syncUniqueID);
            syncValues.put(Database.SyncEntry.SYNC_START, start.getTime());
            syncValues.put(Database.SyncEntry.SYNC_END, stop.getTime());
            syncValues.put(Database.SyncEntry.USER,currentUserId);
            syncValues.put(Database.SyncEntry.STATUS, 0);
            long newRowId;
            newRowId = db.insert(Database.SyncEntry.SYNC_TABLE_NAME,
                    null,
                    syncValues);
            lazyLog.d( "Sync database add:\n"+syncValues.toString());

//                    FirebaseCalls fbc = new FirebaseCalls(c);
//                    fbc.uploadSync(syncUniqueID);

            String selection =  Database.SyncEntry.GUID + "=?";
            ContentValues valuesRead = new ContentValues();
            Cursor cur = db.query(
                    Database.SyncEntry.SYNC_TABLE_NAME,  // The table to query
                    new String[] { Database.SyncEntry.GUID, //blob
                            Database.SyncEntry.SYNC_START, //int
                            Database.SyncEntry.SYNC_END, //int
                            Database.SyncEntry.USER, //string
                            Database.SyncEntry.STATUS }, //bool                          // The columns to return
                    selection,                                // The columns for the WHERE clause
                    new String[] { syncUniqueID },                            // The values for the WHERE clause
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
            Firebase ref = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +cur.getString(3) + "/sync/"+cur.getString(0));


            Map<String,Object > syncData = new HashMap<String, Object>();
//                    syncData.put(Database.SyncEntry.GUID, cur.getString(0));
            syncData.put(Database.SyncEntry.SYNC_START, UTC.isoFormat(Long.parseLong(cur.getString(1))));
            syncData.put(Database.SyncEntry.SYNC_END, UTC.isoFormat(Long.parseLong(cur.getString(2))));
            syncData.put(Database.SyncEntry.USER, cur.getString(3));
            syncData.put(Database.SyncEntry.STATUS, cur.getString(4));

            lazyLog.d("Sync ID is "+cur.getString(0));
            ref.setValue(syncData);
            cur.close();
            //*****************steps***********************//
            Cursor curSteps = getStepsForSync(syncUniqueID);


            Map<String, Map<String, Map<String, Object>>> monthMap = new HashMap<String, Map<String, Map<String, Object>>>(); //day<minutes,steps>>
//                    List<Object>[] monthList = new List[];
            Map<String, Map<String, String>>  minuteMap = new HashMap<String,Map<String, String>>(); //minutes, steps
            Map<String, Map<String, String>> dayMap = new HashMap<String, Map<String, String>>(); //day<minutes,steps>>

            Calendar cal = UTC.newCal();

            ArrayList list = new ArrayList();
            int date = -1;
            int oldDate =-1;
            String username = "";
            while (curSteps.isAfterLast() == false) {
                if(Integer.parseInt(curSteps.getString(4))!=0) {
                    username = curSteps.getString(3);


                    long stepTime = Long.parseLong(curSteps.getString(2));
                    Date curDate = new Date(stepTime);

                    cal.set( Calendar.YEAR, 2015);
                    cal.set( Calendar.MONTH, curDate.getMonth());
                    cal.set( Calendar.DATE, curDate.getDate() );
                    cal.set( Calendar.HOUR_OF_DAY, curDate.getHours() );
                    cal.set( Calendar.MINUTE, curDate.getMinutes() );
                    cal.set( Calendar.SECOND, 0 );
                    cal.set( Calendar.MILLISECOND, 0 );
                    String dayMinute = (curDate.getMinutes() + (curDate.getHours() *60))+"";

                    if((date!=curDate.getDate()) &&(date!=-1)){
                        String startTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(1)));
                        String endTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(2)));
                        oldDate = date;
                        Firebase refStep2 = new Firebase("https://ss-movo-wave-v2.firebaseio.com/users/" +curSteps.getString(3) + "/steps/"+(curDate.getYear()+1900)+"/"+(curDate.getMonth()+1)+"/"+oldDate).child(curSteps.getString(0)); //to modify child node
                        refStep2.setValue(minuteMap);



                        minuteMap = new HashMap<String,Map<String, String>>(); //minutes, steps
                        Map<String,String > stepData = new HashMap<String, String>();
//                            stepData.put(Database.StepEntry.SYNC_ID, curSteps.getString(0));
                        stepData.put(Database.StepEntry.START, startTime);
                        stepData.put(Database.StepEntry.END, endTime);
//                            stepData.put(Database.StepEntry.USER, curSteps.getString(3));
                        stepData.put(Database.StepEntry.STEPS, curSteps.getString(4));
                        stepData.put(Database.StepEntry.DEVICEID, curSteps.getString(5));


                        minuteMap.put(startTime, stepData);

                        date = curDate.getDate();

                    }else{
                        oldDate = date;
                        String startTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(1)));
                        String endTime = UTC.isoFormatShort(Long.parseLong(curSteps.getString(2)));
                        Map<String,String > stepData = new HashMap<String, String>();
                        stepData.put(Database.StepEntry.SYNC_ID, curSteps.getString(0));
                        stepData.put(Database.StepEntry.START, startTime);
                        stepData.put(Database.StepEntry.END, endTime);
//                            stepData.put(Database.StepEntry.USER, curSteps.getString(3));
                        stepData.put(Database.StepEntry.STEPS, curSteps.getString(4));
                        stepData.put(Database.StepEntry.DEVICEID, curSteps.getString(5));

                        if(Integer.parseInt(curSteps.getString(4))!=0) {
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
            }catch(Exception e){
                e.printStackTrace();
                lazyLog.d( "No new entries to upload");
                Toast.makeText(c, "No new steps to add.", Toast.LENGTH_SHORT).show();
            }
            curSteps.close();
        } else {
            lazyLog.d( "SYNC FAILED!" + sync);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMenu(R.layout.activity_discover_wave);
        waveList = (ListView) findViewById(R.id.waveList);
        DatabaseHelper mDbHelper = new DatabaseHelper(c);

        db = mDbHelper.getWritableDatabase();

        waves = new ArrayList<>();

        // Create The Adapter with passing ArrayList as 3rd parameter
        arrayAdapter = new ArrayAdapter<>(this,R.layout.drawer_list_item, waves);

        // Set The Adapter
        waveList.setAdapter(arrayAdapter);

        arrayAdapter.setNotifyOnChange( true );

        waveList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // argument position gives the index of item which is clicked
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                lazyLog.d("Clicked " + position);

                // change association of wave to current user
                final WaveInfo info = waves.get( position );
                if( ! syncMap.containsKey(info)) {
                    syncMap.put( info, WaveAgent.DataSync.byInfo(10000, info, syncCallback));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        waves.clear();
        WaveInfo.byUser(db, waves, UserData.getUserData(c).getCurUID() );
        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
