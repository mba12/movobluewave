package com.movo.wave;

//Created by Alexander Haase on 4/10/2015.

import android.speech.tts.SynthesisCallback;
import android.util.Log;

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

    final static String TAG = "WaveAgent";

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

                } else if( ! callback.seen.contains( device ) ) {
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
                                //FIXME: stub for debug
                                serial = "UNKNOWN";
                                Log.w( TAG, "scanForWaveDevices(): couldn't scan device "
                                        + device.device.getAddress());
                            }

                            final WaveDevice wave = new WaveDevice( device, "UNKNOWN", serial);
                            deviceMap.put(device, wave);
                            callback.notify(wave);

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

        final static String TAG = "WaveAgent::DataSync";

        /** Callback for state notifications
         *
         */
        public static interface Callback {
            /** Notify state change
             *
             * @param sync sync object.
             * @param state sync state.
             * @param status sync status.
             */
            public void notify( DataSync sync, SyncState state, boolean status );

            /** Completion callback
             *
             * @param sync sync object.
             * @param data data set or null.
             */
            public void complete( DataSync sync, List<WaveRequest.WaveDataPoint> data );
        }

        public BLEAgent.BLEDevice device = null;
        final public int timeout = 5000;
        public Date deviceDate;
        private int dataSuccess = 0;
        private int dataFailure = 0;
        final public Callback callback;
        final private List<WaveRequest.WaveDataPoint> data = new LinkedList<>();
        private SyncState state = SyncState.DISCOVERY;

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
            callback.notify( this, state, success );

            Log.v( TAG, this.toString() + "\tState was: " + state );
            state = state.next();
            Log.v( TAG, this.toString() + "\tState now: " + state );

            if( success ) {
                switch (state) {
                    case VERSION:
                        /*BLEAgent.handle(new WaveRequest.ReadVersion(device, 1000) {
                            @Override
                            protected void onComplete(boolean success, final String version) {
                                nextState( success );
                            }
                        });
                        break;*/
                        nextState( true );
                        break;
                    case GET_DATE:
                        BLEAgent.handle(new WaveRequest.GetDate(device, timeout) {
                            @Override
                            protected void onComplete(boolean success, Date date) {
                                deviceDate = date;
                                nextState( success );
                            }
                        });
                        break;
                    case REQUEST_DATA:
                        dispatchData();
                        break;
                    case SET_DATE:
                        BLEAgent.handle( new WaveRequest.SetDate( device, 1000 ) {
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
                        Log.e( TAG, "Unexpected state: " + state);
                        break;
                }
            } else {
                callback.complete( this, null );
            }
        }

        /** Convenience wrapper for dispatching data requests
         *
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
            if( response != null) {
                dataSuccess += 1;
                //int reservedCount = 0;

                for (WaveRequest.WaveDataPoint point : response) {
                    //Log.d( TAG, point.toString() );
                    if( point.mode != WaveRequest.WaveDataPoint.Mode.RESERVED
                            && point.value != 0) {
                        Log.i( TAG, point.toString() );
                    }
                    data.add(point);
                }
                                /*if( reservedCount != data.length && reservedCount != 0 ) {
                                    Log.i( TAG, "DUMP START: " + day + " " + hour
                                            + " (" + reservedCount + "/" + data.length + ")" );
                                    for (WaveRequest.WaveDataPoint point : data) {
                                        Log.i( TAG, point.toString() );
                                    }
                                    Log.i( TAG, "DUMP END: " + day + " " + hour
                                            + " (" + reservedCount + "/" + data.length + ")" );
                                }*/
            } else {
                dataFailure += 1;
                Log.w(TAG, "FAILED data: " + day + " " + hour );
            }

            if( dataSuccess + dataFailure == 7 * 24 / 3 ) {
                nextState( true );
            }
        }
    }
}
