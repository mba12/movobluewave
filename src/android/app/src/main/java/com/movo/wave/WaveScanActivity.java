package com.movo.wave;

import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.movo.wave.comms.BLEAgent;
import com.movo.wave.comms.WaveAgent;
import com.movo.wave.comms.WaveInfo;
import com.movo.wave.comms.WaveRequest;
import com.movo.wave.util.LazyLogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Alex Haase on 3/23/2015.
 */

public class WaveScanActivity extends MenuActivity {

    private HashMap<String,WaveInfo> wavesByMAC = new HashMap<>();
    private SQLiteDatabase db;
    private int pendingRequests = 0;

    private int acquire() {
        return pendingRequests += 1;
    }

    private int release() {
        return pendingRequests -= 1;
    }

    ArrayList<WaveInfo> knownWaves;
    ArrayList<WaveInfo> newWaves;
    ArrayAdapter<WaveInfo> knownWaveAdapter;
    ArrayAdapter<WaveInfo> newWaveAdapter;
    ListView knownWaveList;
    ListView newWaveList;

    enum ScanState {
        WAITING,
        SCANNING,
        COMPLETE ("!", true)
        ;

        final public String terminator;
        final boolean scanEnabled;

        private ScanState() {
            terminator = "\u2026";
            scanEnabled = false;
        }

        private ScanState( String terminator,  boolean scanEnabled ) {
            this.terminator = terminator;
            this.scanEnabled = scanEnabled;
        }

        public ScanState next() {
            if( this.ordinal() + 1 == values().length ) {
                return values()[ 0 ];
            } else {
                return values()[ordinal() + 1];
            }
        }
    }

    Resources resources;
    ScanState scanState = ScanState.COMPLETE;
    ProgressBar scanProgress;
    TextView scanStatus;
    Button scanButton;

    private void nextState() {
        scanState = scanState.next();
        if( scanState.scanEnabled ) {
            scanProgress.setVisibility( View.INVISIBLE );
        } else {
            scanProgress.setVisibility( View.VISIBLE );
        }

        scanStatus.setText( resources.getTextArray(R.array.scan_state_enum)[ scanState.ordinal() ] +
            scanState.terminator);

        scanButton.setEnabled( scanState.scanEnabled );
    }

    private final BLEAgent.BLERequestScan scanRequest = new BLEAgent.BLERequestScan(10000) {

        @Override
        public boolean dispatch(BLEAgent agent) {
            acquire();
            nextState();
            return super.dispatch(agent);
        }

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
                    addInfo( info );
                    lazyLog.d( "Loading sql-cached wave device", info );
                } else {
                    acquire();
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
                                addInfo( info );
                            } else {
                                lazyLog.e( "Failed to query device: " + info.mac );
                            }
                            if( release() == 0 ) {
                                nextState();
                            }
                        }
                    });
                }
            } while( false );
            return false;
        }

        @Override
        public void onComplete(BLEAgent.BLEDevice device) {
            if( release() == 0 ) {
                nextState();
            }
            lazyLog.i( "Wave discovery complete" );
        }
    };

    public static final LazyLogger lazyLog = new LazyLogger( "WaveScanActivity",
            MenuActivity.lazyLog );

    public boolean startScan() {
        if( scanState != ScanState.COMPLETE ) {
            lazyLog.e( "Call to startScan() while scan in progress!");
            return false;
        }

        nextState();
        BLEAgent.handle( scanRequest );
        return true;
    }

    /** Use to add a *new* waveinfo instance.
     *
     * @param info
     */
    private void addInfo( WaveInfo info ) {
        if( UserData.getUserData(c).getCurUID().equals(info.user) ) {
            //TODO: Sort by date.
            knownWaves.add(info);
            knownWaveAdapter.notifyDataSetChanged();
        } else {
            newWaves.add(info);
            newWaveAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMenu(R.layout.activity_wave_sync);
        knownWaveList = (ListView) findViewById(R.id.knownWaveList);
        newWaveList = (ListView) findViewById( R.id.newWaveList);

        resources = getResources();
        scanProgress = (ProgressBar) findViewById( R.id.waveScanProgress);
        scanStatus = (TextView) findViewById(R.id.waveScanStatus);
        scanButton = (Button) findViewById(R.id.waveScanButton);

        scanButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });

        DatabaseHelper mDbHelper = new DatabaseHelper(c);

        db = mDbHelper.getWritableDatabase();

        knownWaves = new ArrayList<>();
        newWaves = new ArrayList<>();

        // Create The Adapter with passing ArrayList as 3rd parameter
        knownWaveAdapter = new ArrayAdapter<>(this,R.layout.drawer_list_item, knownWaves);

        // Set The Adapter
        knownWaveList.setAdapter(knownWaveAdapter);

        knownWaveAdapter.setNotifyOnChange(true);

        knownWaveList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // argument position gives the index of item which is clicked
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {

                final WaveInfo info = knownWaves.get(position);
                // start sync activity
                Intent intent = new Intent(c, SyncDataActivity.class);
                intent.putExtra("MAC", info.mac);
                startActivity(intent);
            }
        });

        newWaveAdapter = new ArrayAdapter<>(this,R.layout.drawer_list_item, newWaves);

        newWaveList.setAdapter(newWaveAdapter);

        newWaveAdapter.setNotifyOnChange(true);

        newWaveList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            // argument position gives the index of item which is clicked
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                // change association of wave to current user
                final WaveInfo info = newWaves.get(position);
                String currentUser = UserData.getUserData(c).getCurUID();
                lazyLog.i("Setting user for device ", info, " from ", info.user, " to ", currentUser);
                info.user = currentUser;
                info.store(db);

                // start sync activity
                Intent intent = new Intent(c, SyncDataActivity.class);
                intent.putExtra("MAC", info.mac);
                startActivity(intent);

                // Move entry from new to known
                newWaves.remove( position );
                newWaveAdapter.notifyDataSetChanged();
                addInfo( info );
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( scanState == ScanState.COMPLETE ) {
            startScan();
        }
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