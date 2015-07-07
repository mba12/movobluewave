package com.movo.wave;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.movo.wave.comms.BLEAgent;
import com.movo.wave.comms.WaveAgent;
import com.movo.wave.comms.WaveInfo;
import com.movo.wave.comms.WaveRequest;
import com.movo.wave.util.LazyLogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static android.widget.AdapterView.OnItemLongClickListener;

/**
 * Created by Alex Haase on 3/23/2015.
 */

public class WaveScanActivity extends MenuActivity {

    /** Rendering helper for dumping wave info with CurrentUser context
     *
     */
    class WaveAdapter {
        final WaveInfo info;

        WaveAdapter( WaveInfo info ) {
            this.info = info;
        }

        boolean knownToUser( String user ) {
            return info.getName( user ) != null;
        }

        boolean knownToUser() {
            return knownToUser(UserData.getUserData(c).getCurUID());
        }

        /** Displayed value
         *
         * @return serial or user's name for device.
         */
        @Override
        public String toString() {
            String value = info.getName( UserData.getUserData(c).getCurUID() );
            if( value == null )
                value = info.serial;
            return value;
        }
    }

    private HashMap<String,WaveInfo> wavesByMAC = new HashMap<>();
    private SQLiteDatabase db;
    private int pendingRequests = 0;

    private int acquire() {
        return pendingRequests += 1;
    }

    private int release() {
        return pendingRequests -= 1;
    }

    ArrayList<WaveAdapter> knownWaves;
    ArrayList<WaveAdapter> newWaves;
    ArrayAdapter<WaveAdapter> knownWaveAdapter;
    ArrayAdapter<WaveAdapter> newWaveAdapter;
    ListView knownWaveList;
    ListView newWaveList;
    String inputNameText = "";

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
                    lazyLog.v( "IGNORING non-wave ", device.device.getAddress(), device.device.getName() );
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
                    lazyLog.d( "Loading sql-cached wave device ", info );
                } else {
                    acquire();
                    // query device serial && gatt attributes
                    BLEAgent.handle(new WaveRequest.ReadSerial(device, 30000) {

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

                                addInfo(info);
                            } else {
                                lazyLog.e( "Failed to query device: " + info.mac );
                            }
                            checkComplete();
                        }
                    });
                }
            } while( false );
            return false;
        }

        /** See if all pending scan actions are done
         *
         */
        private void checkComplete() {
            if( release() == 0 ) {
                BLEAgent.release();
                lazyLog.i("Wave discovery complete");
                nextState();
            }
        }

        @Override
        public void onComplete(BLEAgent.BLEDevice device) {
            lazyLog.i( "BLE scan complete" );
            checkComplete();
        }
    };

    public static final LazyLogger lazyLog = new LazyLogger( "WaveScanActivity",
            MenuActivity.lazyLog );


    public boolean startScan() {

        if ( ! bleEnabled() ) {
            lazyLog.i("BLE not enabled, spawning activity");
            requestBLEEnabled();
            return false;
        }


        if( scanState != ScanState.COMPLETE ) {
            lazyLog.e( "Call to startScan() while scan in progress!");
            return false;
        }

        knownWaves.clear();
        knownWaveAdapter.notifyDataSetChanged();
        newWaves.clear();
        newWaveAdapter.notifyDataSetChanged();
        wavesByMAC.clear();

        nextState();
        final boolean status = BLEAgent.open(c);
        lazyLog.i("Opened BLE agent with status ", status);
        BLEAgent.handle( scanRequest );
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == REQUEST_ENABLE_BT ) {
            lazyLog.i( "BLE enable multi-return with status ", resultCode );
        } else {
            lazyLog.i("BLE still disabled, not starting scan: request ", requestCode,
                    " result ", resultCode);
        }
    }

    /** Use to add a *new* waveinfo instance.
     *
     * @param info
     */
    private void addInfo( WaveInfo info ) {
        addInfo(new WaveAdapter(info));
    }

    /** Use to add a *new* waveinfo instance.
     *
     * @param adapter
     */
    private void addInfo( WaveAdapter adapter ) {
        knownWaveList.clearAnimation();
        newWaveList.clearAnimation();
        if( adapter.knownToUser() ) {
            //TODO: Sort by date.
            knownWaves.add(adapter);
            knownWaveAdapter.notifyDataSetChanged();
        } else {
            newWaves.add(adapter);
            newWaveAdapter.notifyDataSetChanged();
        }

        //create blinking animation for known/unknown waves. If known wave is present, blink it. If only unknown waves, blink them instead.
        final Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        animation.setDuration(1000); // duration - whole second
        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
        if(knownWaves.size()>0){

            knownWaveList.startAnimation(animation);
        }else if(newWaves.size()>0){

            newWaveList.startAnimation(animation);
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
        knownWaveAdapter = new ArrayAdapter<>(this,R.layout.drawer_list_item_scan, knownWaves);

        // Set The Adapter
        knownWaveList.setAdapter(knownWaveAdapter);

        knownWaveAdapter.setNotifyOnChange(true);

        knownWaveList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // argument position gives the index of item which is clicked
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {

                String currentUser = UserData.getUserData(c).getCurUID();
                final WaveAdapter adapter = knownWaves.get(position);
                // start sync activity
                Intent intent = new Intent(c, SyncDataActivity.class);
                intent.putExtra(SyncDataActivity.EXTRA_WAVE_MAC, adapter.info.mac);
                intent.putExtra(SyncDataActivity.EXTRA_WAVE_USER_ID, currentUser);
                startActivity(intent);
            }
        });
        knownWaveList.setOnItemLongClickListener(new OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int position, long id) {

                final WaveAdapter adapter = knownWaves.get(position);

                final String currentUser = UserData.getUserData(c).getCurUID();
                Intent intent = new Intent(c, SyncDataActivity.class);
                intent.putExtra(SyncDataActivity.EXTRA_WAVE_MAC, adapter.info.mac);
                intent.putExtra(SyncDataActivity.EXTRA_WAVE_USER_ID, currentUser);
                adapter.info.getName(currentUser);

                AlertDialog.Builder builder = new AlertDialog.Builder(WaveScanActivity.this);
                builder.setTitle("Rename Wave?");

                final EditText input = new EditText(WaveScanActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
                String waveName = prefs.getString(adapter.info.mac, "");
                Log.v("Wave name is", waveName);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        inputNameText = input.getText().toString();

                        if(!inputNameText.equals("")) {
                            String currentUser = UserData.getUserData(c).getCurUID();
                            adapter.info.setName(currentUser, inputNameText);
                            adapter.info.store(db);
                            prefs.edit().putBoolean(currentUser+adapter.info.mac+"syncPrompt",true);
                        }else{
                            Toast.makeText(WaveScanActivity.this, "Name cannot be blank!", Toast.LENGTH_SHORT).show();
                        }
                        Log.v("Wave name set is ", inputNameText);


                    }
                });


                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();



                return true;
            }
        });

        newWaveAdapter = new ArrayAdapter<>(this,R.layout.drawer_list_item_scan, newWaves);

        newWaveList.setAdapter(newWaveAdapter);

        newWaveAdapter.setNotifyOnChange(true);

        newWaveList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            // argument position gives the index of item which is clicked
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                // change association of wave to current user
                final WaveAdapter adapter = newWaves.get(position);
                String currentUser = UserData.getUserData(c).getCurUID();
                lazyLog.i("Setting user for device ", adapter, " to ", currentUser);
                adapter.info.setName( currentUser, null );
                adapter.info.store(db);

                // start sync activity
                Intent intent = new Intent(c, SyncDataActivity.class);
                intent.putExtra(SyncDataActivity.EXTRA_WAVE_MAC, adapter.info.mac);
                intent.putExtra(SyncDataActivity.EXTRA_WAVE_USER_ID, currentUser);
                startActivity(intent);

                // Move entry from new to known
                newWaves.remove( position );
                newWaveAdapter.notifyDataSetChanged();
                addInfo( adapter );
            }
        });



    }

    @Override
    protected void onResume() {
        super.onResume();
        if( scanState == ScanState.COMPLETE && bleEnabled() ) {
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