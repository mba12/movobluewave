package com.movo.wave;

//Created by Alexander Haase on 4/10/2015.

import android.util.Log;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/** High-level Wave interaction class
 *
 * @author Alexander Haase
 *
 */
public class WaveAgent {

    final static String TAG = "WaveAgent";

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

        abstract void notify( BLEAgent.BLEDevice device );

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
                    if( isWave( device ) ) {
                        callback.notify(device);
                    }
                } else if( ! callback.seen.contains( device ) ) {
                    callback.acquire();
                    BLEAgent.handle( new BLEAgent.BLERequest(device, timeout) {
                        @Override
                        public boolean dispatch(BLEAgent agent) {
                            if( isWave( this.device ) ) {
                                callback.notify( device );
                                callback.release();
                            }
                            return true;
                        }

                        @Override
                        public void onFailure() {
                            Log.w( TAG, "scanForWaveDevices(): couldn't scan device "
                                    + device.device.getAddress());
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




    public static <V> Set<V> asSet( final V value ) {
        final Set<V> ret = new HashSet<>();
        ret.add( value );
        return ret;
    }

    /*
    protected static class DataSync {
        public interface WaveSyncCallback {
            public void notify( SyncDevice sync, SyncDevice.SyncState state );

            public void complete( WaveRequest.WaveDataPoint[] data );
        }
        public BLEAgent.BLEDevice device;
        public Date deviceDate;
        public WaveSyncCallback callback;

        enum SyncState {
            SCANNING,
            VERSION,
            VERSION_CHECK,
        }

        DataSync( BLEAgent.BLEDevice device ) {
            this.device = device;
            onDevice();
        }

        private void onDevice() {
            final BLEAgent.BLERequest dateRequest = new WaveRequest.GetDate() {
                @Override
                protected void onComplete(boolean success, Date date) {
                    deviceDate = date;
                    if( success ) {
                        callback.notify( this, Wa);
                    }
                }
            })
        }
    }*/
}
