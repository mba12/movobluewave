package com.movo.wave;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.movo.wave.comms.BLEAgent;
import com.movo.wave.comms.WaveAgent;
import com.movo.wave.comms.WaveInfo;
import com.movo.wave.comms.WaveRequest;
import com.movo.wave.util.LazyLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Alex Haase on 3/23/2015.
 */

public class DiscoverWaveActivity extends MenuActivity {

    private HashMap<String,WaveInfo> wavesByMAC = new HashMap<>();
    private SQLiteDatabase db;

    ArrayList<WaveInfo> waves;
    ArrayAdapter<WaveInfo> arrayAdapter;
    ListView waveList;

    private final BLEAgent.BLERequestScan scanRequest = new BLEAgent.BLERequestScan(10000) {
        @Override
        public boolean filter(BLEAgent.BLEDevice device) {
            do {
                // ignore cursorily non-wave devices
                if (!"Wave".equals(device.device.getName())) {
                    break;
                }

                // check to see if we've seen the device yet this activity
                final String mac = device.device.getAddress();
                if ( wavesByMAC.containsKey( mac ) ) {
                    break;
                }

                // query sql
                final WaveInfo info = new WaveInfo( db, mac );
                wavesByMAC.put( mac, info );

                if( info.complete() ) {
                    // don't show devices already associated with user.
                    if( ! UserData.getUserData(c).getCurUID().equals(info.user)) {
                        waves.add(info);
                        arrayAdapter.notifyDataSetChanged();
                    } else {
                        lazyLog.d( "skipping associated device ", info );
                    }
                } else {
                    // query device serial && gatt attributes
                    BLEAgent.handle(new WaveRequest.ReadSerial(device, 10000) {

                        @Override
                        public boolean dispatch(BLEAgent agent) {
                            if(WaveAgent.isWave(this.device)) {
                                //read serial if gatt looks okay
                                return super.dispatch(agent);
                            } else {
                                //not a wave...
                                info.queried = new Date();
                                info.store( db );
                                return true;
                            }
                        }

                        @Override
                        protected void onComplete(boolean success, String serial) {
                            if( success ) {
                                info.serial = serial;
                                info.queried = new Date();
                                info.store( db );
                                waves.add( info );
                                arrayAdapter.notifyDataSetChanged();
                            } else {
                                lazyLog.e( "Failed to query device: " + info.mac );
                            }
                        }
                    });
                }
            } while( false );
            return false;
        }

        @Override
        public void onComplete(BLEAgent.BLEDevice device) {
            lazyLog.i( "Wave discovery complete" );
        }
    };

    public static final LazyLogger lazyLog = new LazyLogger( "Movo Discover Wave",
            MenuActivity.lazyLog );

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
                String currentUser = UserData.getUserData(c).getCurUID();
                lazyLog.i( "Setting user for device ", info, " from ", info.user, " to ", currentUser );
                info.user = currentUser;
                info.store( db );

                // remove as option
                waves.remove( position );
                arrayAdapter.notifyDataSetChanged();
            }
        });

        BLEAgent.handle( scanRequest );
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