package com.movo.wave;

//Created by Alexander Haase on 4/10/2015.

import android.speech.tts.SynthesisCallback;
import android.util.Log;

import com.movo.wave.util.LazyLogger;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** High-level Wave interaction class
 *
 * @author Alexander Haase
 *
 */
public class WaveAgent {

    final private static LazyLogger lazyLog = new LazyLogger( "WaveAgent" );

    public static class WaveDevice {
        final public BLEAgent.BLEDevice ble;
        final public String version;
        final public String serial;

        private WaveDevice( BLEAgent.BLEDevice device, String version, String serial ) {
            this.ble = device;
            this.version = version;
            this.serial = serial;
        }
    }

    private final static HashMap<BLEAgent.BLEDevice, WaveDevice> deviceMap = new HashMap<>();

    /** Interface for receiving wave device scan results
     *
     * Note: We don't synchronize internally because everything happens on the main thread.
     */
    abstract static public class WaveScanCallback {
        final private Set<BLEAgent.BLEDevice> seen = new HashSet<>();
        private int pendingCount = 0;

        private void acquire() {
            pendingCount += 1;
        }

        private void release() {
            pendingCount -= 1;
            if( pendingCount == 0 ){
                onComplete();
            }
        }

        abstract void notify( WaveDevice device );

        /** Called at scan completion
         */
        abstract void onComplete();
    }

    /** Test for if a BLEDevice is a wave device. Requires service discovery
     *
     * Right now just looks for notify and write UUIDs.
     *
     * @param device communications target.
     * @return boolean indication of wave-ness.
     */
    public static boolean isWave( BLEAgent.BLEDevice device ) {
        return device.getCharacteristic( WaveRequest.BLERequestWaveOp.writeUUIDs ) != null &&
                device.getCharacteristic( WaveRequest.BLERequestWaveOp.notifyUUIDs ) != null;
    }

    /** Scans for wave devices, discovering services as necessary.
     *
     * @param timeout per-request timeout, can make things quite large.
     * @param callback result target for synchronization and device registry.
     */
    public static void scanForWaveDevices( final int timeout, final WaveScanCallback callback ) {

        callback.acquire();

        BLEAgent.handle( new BLEAgent.BLERequestScan(timeout) {

            @Override
            public boolean filter(BLEAgent.BLEDevice device) {

                if( device.servicesDiscovered ) {
                    final WaveDevice wave = deviceMap.get( device );
                    if( wave != null ) {
                        callback.notify( wave );
                    }

                } else if( ! callback.seen.contains( device )
                        && "Wave".equals(device.device.getName()) ) {

                    /*
                     AH 2015-04-19: Connect to prevent devices from getting board before we can
                     inspect them.
                     */
                    device.gatt.connect();
                    callback.acquire();
                    BLEAgent.handle( new WaveRequest.ReadSerial(device, timeout) {
                        @Override
                        public boolean dispatch(BLEAgent agent) {
                            if( isWave( this.device ) ) {
                                return super.dispatch( agent );
                            } else {
                                return true;
                            }
                        }

                        @Override
                        protected void onComplete(boolean success, String serial) {

                            if( ! success ) {
                                WaveAgent.lazyLog.w( "scanForWaveDevices(): couldn't scan device "
                                       , device.device.getAddress());
                            } else {
                                final WaveDevice wave = new WaveDevice(device, "UNKNOWN", serial);
                                WaveAgent.lazyLog.d("Pairing device '", device.device.getAddress(),
                                        "' to serial '", serial, "'");
                                deviceMap.put(device, wave);
                                callback.notify(wave);
                            }

                            callback.release();
                        }
                    });
                }
                callback.seen.add(device);
                return false;
            }

            @Override
            public void onComplete(BLEAgent.BLEDevice device) {
                callback.release();
            }
        });
    }

    /** Data sync representation object
     *
     */
    protected static class DataSync {

        //final private static LazyLogger lazyLog = new LazyLogger("WaveAgent::DataSync");
        final private static LazyLogger lazyLog = new LazyLogger("WaveAgent::DataSync",WaveAgent.lazyLog);

        /** Callback for state notifications
         * Can be used for multiple sync sessions. Each sync session will present it's object as
         * part of the notification callback.
         */
        public static interface Callback {
            /** Notify state change
             *
             * @param sync sync object.
             * @param state sync state.
             * @param status sync status.
             */
            public void notify( DataSync sync, SyncState state, boolean status );

            /** Notify progress
             *
             * @param sync sync object.
             * @param progress percent as decimal.
             */
            public void notify( DataSync sync, float progress );

            /** Completion callback. No new notifications after this callback.
             *
             * @param sync sync object.
             * @param data data set or null.
             */
            public void complete( DataSync sync, List<WaveRequest.WaveDataPoint> data );
        }

        public BLEAgent.BLEDevice device = null;
        final public int timeout = 10000;
        public Date deviceDate = null;
        public Date localDate = null;
        private int dataSuccess = 0;
        private int dataFailure = 0;
        final public Callback callback;
        final private List<WaveRequest.WaveDataPoint> data = new LinkedList<>();
        private SyncState state = SyncState.DISCOVERY;
        private float requestProgress = 0;


        /**
         * Discovery: 1 (maybe)
         * Data: 8 requests/day * 7 days
         * Get and set date: 2
         * Serial and version: 2
         */
        private static float PROGRESS_STEP = 1.0f / (24 * 7 / 3 + 5 );

        private void progress() {
            callback.notify( this, requestProgress += PROGRESS_STEP );
        }

        /** Enumeration of sync state
         *
         */
        public static enum SyncState {
            DISCOVERY,
            VERSION,
            GET_DATE,
            REQUEST_DATA,
            SET_DATE,
            COMPLETE {
                @Override
                public SyncState next() {
                    return null;
                }
            };

            public SyncState next() {
                return values()[ordinal() + 1];
            }
        }

        /** Private constructor to facilitate device discovery...
         *
         * @param callback notification callback.
         */
        private DataSync( final Callback callback ) {
            this.callback = callback;
        }

        /** Constructor for finding device by address
         *
         * Since double string overload is a no-no
         *
         * @param timeout discovery timeout in seconds.
         * @param address BLE address of device.
         * @param callback notification callback.
         * @return DataSync object for operation.
         */
        public static DataSync byAddress( final int timeout,
                                          final String address,
                                          final Callback callback ) {
            final DataSync ret = new DataSync( callback );

            BLEAgent.handle( new BLEAgent.BLERequestScanForAddress( timeout, address ) {
                @Override
                public void onComplete(BLEAgent.BLEDevice device) {
                    ret.device = device;
                    ret.nextState( device != null );
                }
            });

            return ret;
        }

        /** Constructor for finding device by serial
         *
         * Since double string overload is a no-no.
         *
         * NOTE: may be very slow......
         *
         * @param timeout discovery timeout in seconds.
         * @param serial wave serial of device.
         * @param callback notification callback.
         * @return DataSync object for operation.
         */
        public static DataSync bySerial(final int timeout,
                                        final String serial,
                                        final Callback callback ) {
            final DataSync ret = new DataSync( callback );

            WaveAgent.scanForWaveDevices( timeout, new WaveScanCallback() {
                @Override
                void notify(WaveDevice device) {
                    if( device.serial.equals( serial ) ) {
                        ret.device = device.ble;
                        ret.nextState( true );
                    }
                }

                @Override
                void onComplete() {
                    if( ret.device == null ) {
                        ret.nextState( false );
                    }
                }
            });

            return ret;
        }

        /** Sync constructor for known device.
         *
         * @param device communications target.
         * @param callback notification callback.
         */
        public DataSync( final BLEAgent.BLEDevice device, final Callback callback ) {
            this.device = device;
            this.callback = callback;
            nextState( true );
        }

        /** central state machine logic.
         *
         * @param success indication of current state success.
         */
        private void nextState( boolean success ) {
            progress();
            callback.notify( this, state, success );

            lazyLog.v( this.toString(), "\tState was: ", state, " (", success, ")" );

            if( success ) {

                state = state.next();
                lazyLog.v( this.toString(), "\tState now: ", state );

                switch (state) {
                    case VERSION:
                        lazyLog.d( "DeviceName: '", device.device.getName(),
                                "' DeviceAddress: ", device.device.getAddress() );
                        BLEAgent.handle(new WaveRequest.ReadVersion(device, timeout) {
                            @Override
                            protected void onComplete(boolean success, final String version) {
                                if( version != null && ! version.equals("4E4F2E31") ) {
                                    DataSync.lazyLog.e("Unrecognized device version: '", version
                                           , "' address: ", device.device.getAddress());
                                }
                                nextState( success );
                            }
                        });
                        break;
                    case GET_DATE:
                        BLEAgent.handle(new WaveRequest.GetDate(device, timeout) {
                            @Override
                            protected void onComplete(boolean success, Date date) {
                                deviceDate = date;
                                localDate = new Date();
                                DataSync.lazyLog.i( "Device Time: ", WaveRequest.UTC.isoFormat( deviceDate ) );
                                DataSync.lazyLog.i( "Local Time: ", WaveRequest.UTC.isoFormat( localDate ) );
                                nextState(success);
                            }
                        });
                        break;
                    case REQUEST_DATA:
                        dispatchData();
                        break;
                    case SET_DATE:
                        BLEAgent.handle( new WaveRequest.SetDate( device, timeout ) {
                            @Override
                            protected void onComplete(boolean success, byte[] response) {
                                nextState( success );
                            }
                        });
                        break;
                    case COMPLETE:
                        callback.complete( this, data );
                        break;
                    default:
                        lazyLog.e( "Unexpected state: ", state);
                        break;
                }
            } else {
                callback.complete( this, null );
            }
        }

        /** Convenience wrapper for dispatching data requests
         * TODO: refactor to one-at-a-time to release radio sooner on failure.
         */
        private void dispatchData() {
            for( int day = 0; day < 7; day++ ) {
                for( int hour = 0; hour < 24; hour += 3 ) {
                    BLEAgent.handle(new WaveRequest.DataByDay(device, timeout, day, hour) {
                        @Override
                        protected void onComplete(boolean success,
                                                  WaveRequest.WaveDataPoint[] data) {
                            receiveData( data, day, hour );
                        }
                    });
                }
            }
        }

        /** Wrapper/barrier for receiving and inspecting requests
         *
         * @param response array of WaveDataPoints or null for failure.
         * @param day of week for request.
         * @param hour of day for request.
         */
        private void receiveData( final WaveRequest.WaveDataPoint[] response,
                                  final int day,
                                  final int hour ) {
            progress();
            if( response != null) {
                dataSuccess += 1;
                boolean dump = false;

                for (WaveRequest.WaveDataPoint point : response) {


                    if( point.mode == WaveRequest.WaveDataPoint.Mode.RESERVED ) {
                        //skip reserved values.

                        lazyLog.v("Dropping point(mode): ", point );
                        continue;
                    } else if( point.date.compareTo( deviceDate ) > 0 ) {
                        //skip future data.
                        lazyLog.v("Dropping point(date): ", point );
                        continue;
                    }
                    data.add(point);
                }
                if( dump ) {
                    lazyLog.i( "DUMP START: ", day, " ", hour );
                    for (WaveRequest.WaveDataPoint point : data) {
                        lazyLog.i( "DUMP: ", point );
                    }
                    lazyLog.i( "DUMP END: ", day, " ", hour );
                }
            } else {
                dataFailure += 1;
                lazyLog.w("FAILED data: ", day, " ", hour );
            }

            if( dataSuccess + dataFailure == 7 * 24 / 3 ) {
                nextState( true );
            }
        }
    }
}
