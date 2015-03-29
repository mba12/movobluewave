package com.movo.wave;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.text.style.BulletSpan;
import android.util.Log;
import android.os.Handler;
import java.util.HashMap;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Alexander Haase on 3/24/15.
 */
public class WaveManager {
    private static final String TAG = "MovoWaveDeviceManager";
    public class BluetoothDisabledException extends Exception {}

    private final BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private final Handler mHandler;
    private HashMap<BluetoothDevice,Wave> mDevices;
    private final ScanCallback mCallback;
    private final Context mContext;
    private final Handler mUIHandler;

    /**
     * bytesToHex method
     * Found on the internet
     * http://stackoverflow.com/a/9855338
     */
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static int hexArrayIndexOf( final char value ) {
        for( int i = 0; i < hexArray.length; i++ ) {
            if( hexArray[ i ] == value ) {
                return i;
            }
        }
        throw new Error( "Cannot hex-lookup value '" + value + "'" );
    }

    private static byte[]  hexToBytes( final String hexValue ) {
        final char[] chars = hexValue.toCharArray();
        byte[] ret = new byte[ chars.length / 2 ];
        int src = 0;
        for( int dst = 0; dst < ret.length; dst += 1 ) {
            ret[ dst ] = (byte) ( hexArrayIndexOf( chars[ src++ ] ) * 16 +
                    hexArrayIndexOf( chars[ src++ ] ) );
        }
        return ret;
    }

    /** Makes sure we're bootstrapped appropriately
     * Failure here signals bluetooth inoperable
     * @return boolean indication of success
     */
    public WaveManager( final Context context ) {
        mScanning = false;
        mHandler = new Handler();
        mDevices = new HashMap<BluetoothDevice,Wave>();
        mCallback = new ScanCallback( this );
        mContext = context;
        mUIHandler = new Handler( mContext.getMainLooper() );

        final BluetoothManager btManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = btManager.getAdapter();

        if( mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled() ) {
            Log.e(TAG, "Bluetooth not enabled!");
            //throw new BluetoothDisabledException();
        } else {
            Log.d( TAG, "Initialized" );
        }

    }


    static final UUID notifyCharacteristicUUID =
            UUID.fromString( "0000ffe4-0000-1000-8000-00805f9b34fb" );
    static final UUID notifyServiceUUID =
            UUID.fromString( "0000ffe0-0000-1000-8000-00805f9b34fb" );
    static final UUID notifyDescriptorUUID =
            UUID.fromString( "00002902-0000-1000-8000-00805f9b34fb" );
    static final UUID writeCharacteristicUUID =
            UUID.fromString( "0000ffe9-0000-1000-8000-00805f9b34fb" );
    static final UUID writeServiceUUID =
            UUID.fromString( "0000ffe5-0000-1000-8000-00805f9b34fb" );

    /** Class for inspecting BLE devices via gatt
     * Defines a specific service and characteristic we're looking for, and
     * encapsulates testing and extracting those resources.
     */
    public static class WaveGattRequirement {
        public final UUID serviceUUID;
        public final UUID characteristicUUID; //TODO: collection?
        public final String name;

        public WaveGattRequirement( final String reqName,
                            final UUID reqServiceUUID,
                            final UUID reqCharacteristicUUID ) {
            name = reqName;
            serviceUUID = reqServiceUUID;
            characteristicUUID = reqCharacteristicUUID;
        }

        public boolean in( final BluetoothGatt gatt ) {
            return extract( gatt ) != null;
        }

        public BluetoothGattCharacteristic extract( final BluetoothGatt gatt ) {
            BluetoothGattService gattService = gatt.getService( serviceUUID );
            if( gattService != null ) {
                return gattService.getCharacteristic( characteristicUUID );
            } else {
                return null;
            }
        }
    }

    /** Notify characteristic requirement
     * Defines the UUIDs we need to send IO requests.
     */
    static WaveGattRequirement notifyRequirement = new WaveGattRequirement(
            "notify",
            notifyServiceUUID,
            notifyCharacteristicUUID
    );

    /** Write characteristic requirement
     *
     */
    static WaveGattRequirement writeRequirement = new WaveGattRequirement(
            "write",
            writeServiceUUID,
            writeCharacteristicUUID
    );

    /** Wave device wrapper, exposes high level device interface
     * Defines the UUIDs we need to re receive IO requests.
     */
    public class Wave extends BluetoothGattCallback {

        /** Signature of the device, useful for re-discovery
         *
         */
        public class Signature {

        }

        private final BluetoothDevice mDevice;
        private Date lastSeen;
        private int mConnectionState;
        private final BluetoothGatt mGatt;
        private BluetoothGattCharacteristic writeCharacteristic;
        private BluetoothGattCharacteristic notifyCharacteristic;

        public Wave( final Context context, final BluetoothDevice device )  {
            mDevice = device;
            lastSeen = new Date();
            Log.d( TAG, "Device Name: " + device.getName() );
            mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
            mGatt = mDevice.connectGatt( context, false, this );
            //Always Bluetooth LE: Log.d( TAG, "Device Type: " + device.getType() );
            //Log.d( TAG, "Device UUID: " + device.getUuids() );
        }

        /** Establish connection to gatt service
         *
         * @param context
         */
        public void connect( final Context context ) {
            mGatt.connect();
        }

        /** Pretty print state description
         *
         * @param state state to enumerate
         * @return string describing state
         */
        private String describeState( int state ) {
            String msg;
            switch ( state ) {
                case BluetoothProfile.STATE_CONNECTED:
                    msg = "CONNECTED";
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    msg = "DISCONNECTED";
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    msg = "CONNECTING";
                default:
                    msg = "<<Unknown>>";
                    break;
            }
            msg += "(" + state + ")";
            return msg;
        }

        /** Trace function for device debugging
         *
         * @param msg
         */
        private void trace( final String what, final String msg ) {
            Log.d( TAG, mDevice.getName() + " " + what + ": " + msg );
        }

        /* Gatt callback interface, maybe we should implement this elsewhere... */

        /** Currently just log state change
         *
         * @param gatt
         * @param status
         * @param newState
         */
        @Override
        public void onConnectionStateChange( final BluetoothGatt gatt, int status, int newState) {
            trace("State", describeState(mConnectionState) + " -> " + describeState(newState));
            mConnectionState = newState;
            if (mConnectionState == BluetoothProfile.STATE_CONNECTED) {
                mGatt.discoverServices();
            }
        }

        /** Dumps all services, characteristics, etc. to log
         * Assumes services already discovered.
         * @param gatt
         */
        private void dumpServices( final BluetoothGatt gatt ) {
            for(BluetoothGattService service : gatt.getServices()) {
                final String serviceUuid = service.getUuid().toString();
                trace( "Service", "\tServiceUUID: " + serviceUuid );

                for( BluetoothGattCharacteristic characteristic : service.getCharacteristics() ) {
                    final String charUuid = characteristic.getUuid().toString();
                    trace( "Service", "\t\tCharacteristicUUID: "  + charUuid );

                    for(BluetoothGattDescriptor descriptor : characteristic.getDescriptors() ) {
                        trace("Service", "\t\t\tDescriptor: " + descriptor.getUuid());
                    }
                }
            }
        }

        private boolean subscribe( final BluetoothGatt gatt ) {
            notifyCharacteristic = notifyRequirement.extract( gatt );
            writeCharacteristic = writeRequirement.extract( gatt );
            boolean ret = notifyCharacteristic != null && writeCharacteristic != null;
            if( ret ) {
                gatt.setCharacteristicNotification(notifyCharacteristic, true);
                BluetoothGattDescriptor notifyDescriptor =
                        notifyCharacteristic.getDescriptor( notifyDescriptorUUID );
                notifyDescriptor.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE );
                gatt.writeDescriptor( notifyDescriptor );

            }
            return ret;
        }

        /** Ensure the device is a wave
         * Called after we probe the device with gatt.discoverServices().
         * @param gatt
         * @param status
         */
        @Override
        public void onServicesDiscovered( final BluetoothGatt gatt, int status ) {
            trace("Service", "Discovered (status " + status + ")");
            dumpServices( gatt );
            boolean isWave = subscribe( gatt );
            trace("Service", "Is a wave? " + isWave );
            if( isWave ) {
                writeCharacteristic.setValue( hexToBytes( "890000" ) );
                gatt.writeCharacteristic( writeCharacteristic );
            }
        }

        @Override
        public void onCharacteristicChanged( final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            trace("Characteristic", "Changed");
            byte[] value = characteristic.getValue();
            trace("Characteristic", bytesToHex( value ) );
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            trace( "Characteristic", "Write" );
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            trace( "Characteristic", "Read" );
        }
    }

    private void updateDevice( final BluetoothDevice device ) {
        mUIHandler.post( new Runnable() {
            @Override
            public void run() {
                Log.d( TAG, "swapping to UI thread" );
                WaveManager.this.updateDeviceImpl( device );
            }
        });
    }

    private void updateDeviceImpl( final BluetoothDevice device )  {
        Wave instance = mDevices.get( device );
        if ( instance != null ) {
            Log.d( TAG, "Updating device!");
            instance.lastSeen = new Date();
        } else {
            Log.d( TAG, "Adding device!" );
            instance = new Wave( mContext, device );
            mDevices.put( device, instance );
        }
    }

    /** Implements interface, delegating device reception to MovoManager
     *
     */
    private class ScanCallback implements BluetoothAdapter.LeScanCallback {
        private final WaveManager mWaveManager;
        public ScanCallback( final WaveManager deviceManager ) {
            mWaveManager = deviceManager;
        }
        @Override
        public void onLeScan( final BluetoothDevice device, int rssi, byte[] scanRecord ) {
            Log.d( TAG, "Found Device! " + device.getName() );
            mWaveManager.updateDevice( device );
        }
    }

    public void scan( Wave.Signature signature ) {

        final int timeout = 10000;

        mHandler.postDelayed( new Runnable() {
            @Override
            public void run() {
                Log.d( TAG, "Stop Device Scan!" );
                mBluetoothAdapter.stopLeScan( mCallback );
            }
        }, timeout);

        Log.d( TAG, "Start Device Scan!" );

        mBluetoothAdapter.startLeScan( mCallback );
    }
}
