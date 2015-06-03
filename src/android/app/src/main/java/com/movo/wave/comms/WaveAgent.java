package com.movo.wave.comms;

//Created by Alexander Haase on 4/10/2015.

import com.movo.wave.util.LazyLogger;
import com.movo.wave.util.UTC;

import java.util.Calendar;
import java.util.Collection;
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

        abstract public void notify( WaveDevice device );

        /** Called at scan completion
         */
        abstract public void onComplete();
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
    public static class DataSync {

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

            /** Indicate if state should be skipped, if skippable
             * 
             * @param sync sync object.
             * @param state sync state.
             * @return boolean indicator of if to skip the state
             */
            public boolean skip( DataSync sync, SyncState state );

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
        public WaveInfo info;
        final public int timeout = 10000;
        public Date deviceDate = null;
        public Date localDate = null;
        private int dataTotal = 0;
        private int dataSuccess = 0;
        private int dataFailure = 0;
        final public Callback callback;
        final private List<WaveRequest.WaveDataPoint> data = new LinkedList<>();
        private SyncState state = SyncState.DISCOVERY;
        private float requestProgress = 0;

        /** Public state getter
         *
         * @return current state
         */
        public SyncState getState() {
            return state;
        }


        /**
         * Discovery: 1 (maybe)
         * Data: 7 requests per week (1 days per request at 30 min/point)
         * Get and set date: 2
         * Serial and version: 2
         */
        private static float PROGRESS_STEP = 1.0f / ( 7 + 5 );

        private void progress() {
            callback.notify( this, requestProgress += PROGRESS_STEP );
        }

        /** Enumeration of sync state
         *
         */
        public static enum SyncState {
            DISCOVERY(false),
            VERSION,
            GET_DATE,
            REQUEST_DATA,
            SET_DATE,
            COMPLETE(false) {
                /** Don't advance beyond complete!
                 *
                 * @return ERROR enum
                 */
                @Override
                public SyncState next() {
                    return ERROR;
                }
            },
            ERROR(false) {
                /** Once in ERROR, always in ERROR
                 *
                 * @return ERROR enum
                 */
                @Override
                public SyncState next() {
                    return this;
                }
            },
            ABORT(false) {
                /** Stop issuing requests, just go to completion state.
                 *
                 * @return COMPLETE
                 */
                @Override
                public SyncState next() {
                    return COMPLETE;
                }
            };

            public final boolean skippable;
            
            private SyncState() {
                this.skippable = true;
            }
            private SyncState( boolean skippable ) {
                this.skippable = skippable;
            }
            
            public SyncState next() {
                return values()[ordinal() + 1];
            }
        }

        public boolean abort() {
            if( inNotify && state != SyncState.COMPLETE && state != SyncState.ERROR ) {
                state = SyncState.ABORT;
                return true;
            } else {
                return false;
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

            BLEAgent.handle(new BLEAgent.BLERequestScanForAddress(timeout, address) {
                @Override
                public void onComplete(BLEAgent.BLEDevice device) {
                    ret.device = device;
                    ret.nextState(device != null);
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
                public void notify(WaveDevice device) {
                    if( device.serial.equals( serial ) ) {
                        ret.device = device.ble;
                        ret.nextState( true );
                    }
                }

                @Override
                public void onComplete() {
                    if( ret.device == null ) {
                        ret.nextState( false );
                    }
                }
            });

            return ret;
        }

        /** Constructor for finding device by WaveInfo
         *
         * Since double string overload is a no-no.
         *
         * NOTE: may be very slow......
         *
         * @param timeout discovery timeout in seconds.
         * @param info WaveInfo object for target device.
         * @param callback notification callback.
         * @return DataSync object for operation.
         */
        public static DataSync byInfo( final int timeout,
                                       final WaveInfo info,
                                       final Callback callback) {
            DataSync ret = null;
            if( info.mac != null ) {
                ret = byAddress( timeout, info.mac, callback );
            } else if( info.serial != null ) {
                ret = bySerial( timeout, info.serial, callback );
            } else {
                lazyLog.a( info.mac != null || info.serial != null,
                        " At least one of mac and serial must be not-null!");
            }

            if( ret != null ) {
                ret.info = info;
            }
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
            nextState(true);
        }

        private boolean inNotify = false;

        /** central state machine logic.
         *
         * @param success indication of current state success.
         */
        private void nextState( boolean success ) {
            progress();

            inNotify = true;
            callback.notify(this, state, success);
            inNotify = false;

            lazyLog.v(this.toString(), "\tState was: ", state, " (", success, ")");

            if( success ) {

                while( true ) {
                    state = state.next();
                    if (state.skippable && callback.skip(this, state)) {
                        lazyLog.v(this.toString(), "\tSkipping state: ", state);
                        continue;
                    } else {
                        break;
                    }
                }

                lazyLog.v( this.toString(), "\tState now: ", state );

                switch (state) {
                    case VERSION:
                        device.acquire(); //<-- VERY IMPORTANT!!!!

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
                                if( success ) {
                                    deviceDate = date;
                                    localDate = new Date();
                                    DataSync.lazyLog.i("Device Time: ", UTC.isoFormat(deviceDate));
                                    DataSync.lazyLog.i("Local Time: ", UTC.isoFormat(localDate));
                                }
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
                        signalComplete( data );
                        break;

                    case ERROR:
                        lazyLog.e( "Error ", this);
                    case ABORT:
                        //Note: We may want to post delayed on the UI thread to avoid nesting stacks.
                        lazyLog.i( "Aborting ", this );
                        BLEAgent.UIHandler.post( new Runnable() {
                            @Override
                            public void run() {
                                nextState(false);
                            }
                        });
                        break;

                    default:
                        lazyLog.e( "Unexpected state: ", state);
                        break;
                }
            } else {
                signalComplete( null );
            }
        }

        private void signalComplete( List<WaveRequest.WaveDataPoint> data ) {
            callback.complete( this, data );
            if(device!=null) {
                device.release(); //<-- VERY IMPORTANT!!!!
            }
        }

        /** Convenience wrapper for dispatching data requests
         * TODO: refactor to one-at-a-time to release radio sooner on failure.
         */
        private void dispatchData() {
            final Calendar cal = UTC.newCal();
            cal.set( Calendar.HOUR_OF_DAY, 0 );
            cal.set( Calendar.MINUTE, 0 );
            cal.set( Calendar.SECOND, 0 );
            cal.set( Calendar.MILLISECOND, 0 );

            for( int day = 0; day < 7; day += 1 ) {
                dataTotal += 1;
                BLEAgent.handle(new WaveRequest.ReadData(device, timeout, cal ) {
                    @Override
                    protected void onComplete(boolean success,
                                              WaveRequest.WaveDataPoint[] data) {
                        receiveData( data, cal );
                    }
                });

                cal.add( Calendar.DATE, -1 );
            }
        }

        /** Wrapper/barrier for receiving and inspecting requests
         *
         * @param response array of WaveDataPoints or null for failure.
         */
        private void receiveData( final WaveRequest.WaveDataPoint[] response, final Calendar cal ) {
            progress();
            if( response != null) {
                dataSuccess += 1;
                boolean dump = false;

                for (WaveRequest.WaveDataPoint point : response) {


                    if( point.mode == WaveRequest.WaveDataPoint.Mode.RESERVED ) {
                        //skip reserved values.

                        //lazyLog.v("Dropping point(mode): ", point );
                        continue;
                    } else if( point.date.compareTo( deviceDate ) > 0 ) {
                        //skip future data.
                        lazyLog.w("Dropping point(future date): ", point);
                        continue;
                    }
                    data.add(point);
                }
                if( dump ) {
                    lazyLog.i( "DUMP START: ", UTC.isoFormat(cal) );
                    for (WaveRequest.WaveDataPoint point : data) {
                        lazyLog.i( "DUMP: ", point );
                    }
                    lazyLog.i( "DUMP END: ", UTC.isoFormat(cal) );
                }
            } else {
                dataFailure += 1;
                lazyLog.w("FAILED data: ",UTC.isoFormat(cal) );
            }

            if( dataSuccess + dataFailure == dataTotal ) {
                nextState( true );
            }
        }
    }
}
