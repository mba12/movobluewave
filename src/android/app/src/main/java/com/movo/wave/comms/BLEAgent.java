package com.movo.wave.comms;


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
import android.os.Handler;
import android.util.Pair;

import com.movo.wave.util.LazyLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.HashSet;

//Created by Alexander Haase on 4/1/2015.

/** A singleton for dealing with android BLE limitations.
 *
 * Represents BLE interactions as atomic requests and operation streams. For reliability, BLEAgent
 * executes one request at a time. Inside requests, radio operations are linearized as AsyncOp
 * objects. In the future, it may be possible to instantiate one BLEAgent per device, but this works
 * reliably for now.
 *
 * Good reading on BLE with respect to android:
 * http://toastdroid.com/2014/09/22/android-bluetooth-low-energy-tutorial/
 * http://stackoverflow.com/questions/17910322/android-ble-api-gatt-notification-not-received
 * http://stackoverflow.com/questions/21278993/android-ble-how-to-read-multiple-characteristics
 * http://stackoverflow.com/questions/26342631/problems-with-android-bluetooth-le-notifications
 *
 * @author Alexander Haase
 */
public class BLEAgent {

    /** Logger
     *
     */
    private final static LazyLogger lazyLog = new LazyLogger( "BLEAgent", true );

    /**
     * bytesToHex method
     * Found on the internet
     * http://stackoverflow.com/a/9855338
     */
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int i = 0;
        for (byte aByte : bytes) {
            int v = aByte & 0xFF;
            hexChars[i++] = hexArray[v >>> 4];
            hexChars[i++] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String bytesToHex(byte[] bytes, final int begin, final int length) {
        final int end = length + begin;
        char[] hexChars = new char[length * 2];
        int i = 0;
        for ( int index = begin; index < end; index += 1 ) {
            final byte aByte = bytes[ index ];
            int v = aByte & 0xFF;
            hexChars[i++] = hexArray[v >>> 4];
            hexChars[i++] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int hexArrayIndexOf( final char value ) {
        for( int i = 0; i < hexArray.length; i++ ) {
            if( hexArray[ i ] == value ) {
                return i;
            }
        }
        throw new Error( "Cannot hex-lookup value '" + value + "'" );
    }

    public static byte[]  hexToBytes( final String hexValue ) {
        final char[] chars = hexValue.toCharArray();
        byte[] ret = new byte[ chars.length / 2 ];
        int src = 0;
        for( int dst = 0; dst < ret.length; dst += 1 ) {
            ret[ dst ] = (byte) ( hexArrayIndexOf( chars[ src++ ] ) * 16 +
                    hexArrayIndexOf( chars[ src++ ] ) );
        }
        return ret;
    }

    public static String byteToHex( byte aByte ) {
        int v = aByte & 0xFF;
        return new String( new char[]{ hexArray[v >>> 4], hexArray[v & 0x0F] });
    }


    // singleton instance, only available via handle() to requests
    final static private BLEAgent self = new BLEAgent();

    private static Context context;

    protected static Handler UIHandler;
    private static int refCount = 0;
    private static Semaphore mutex = new Semaphore( 1 );
    private static int lifeCycleCount = 0;

    /** Singleton initializer for framework;
     *
     * @param ctx current application context for binding.
     * @return success
     */
    static public boolean open( Context ctx ) {
        boolean ret = true;

        try {
            mutex.acquire();
            if (context == null) {
                context = ctx;
                UIHandler = new Handler(ctx.getMainLooper());
                final BluetoothManager btManager =
                        (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                self.adapter = btManager.getAdapter();

                if( self.adapter == null || !self.adapter.isEnabled() ) {
                    lazyLog.e( "Bluetooth not enabled!");
                    ret = false;
                } else {
                    lifeCycleCount += 1;
                    lazyLog.d("Initialized: lifecycle ", lifeCycleCount );
                }

            } else if (ctx != context) {
                lazyLog.e( "BLEAgent already open! (and context references differ): ",
                        context, " != ", ctx);
                ret = true;
            }
            if( ret ) {
                refCount += 1;
                lazyLog.i( "BLEAgent ref count ", refCount );
            }
            mutex.release();
        } catch ( InterruptedException e ) {
            lazyLog.e( "Unexpected interrupt in open()" );
            ret = false;
        }
        return ret;
    }

    /**
     * singleton GATT callback for receiving and re-dispatching results.
     * probably dumber than proxying.
     *
     * Note: all responses are re-directed to UI context.
     */
     private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        private boolean checkGatt( final BluetoothGatt gatt ) {
            final boolean ret =  currentOp != null && currentDevice != null && currentDevice.gatt == gatt;

            if( currentOp == null ) {
                lazyLog.w( "Received Gatt operation without active op");
            }

            if( currentRequest == null ) {
                lazyLog.w( "Received Gatt operation without active request");
            }
            if( currentDevice != null )
                lazyLog.a(currentDevice.gatt == gatt, "Gatt objects mismatch");
            else
                lazyLog.w( "Received Gatt operation without active device");
            return ret;
        }

        @Override
        public void onConnectionStateChange( final BluetoothGatt gatt,
                                             final int status,
                                             final int newState) {
            try {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (checkGatt(gatt)) {
                            if (currentOp.onConnectionStateChange(status, newState)) {
                                nextOp();
                            }
                        } else {
                            //try to lookup device
                            lazyLog.i( "Trying to reroute connection state to device... " );
                            final String address = gatt.getDevice().getAddress();
                            final BLEDevice device = BLEDevice.byAddress(address);

                            if (device != null) {
                                lazyLog.i( address, " ",
                                        DebugOp.describeState( device.connectionState ), " => ",
                                        DebugOp.describeState( newState ));
                                device.connectionState = newState;
                                if( device != currentDevice &&
                                        newState != BluetoothGatt.STATE_DISCONNECTED ) {
                                    lazyLog.i( "Disconnecting from errant device: ", address );
                                    //gatt.disconnect();
                                }
                            } else {
                                lazyLog.i( "Failed to look up device for ", address, " ignoring state change" );
                            }

                        }
                    }
                });
            } catch (Exception e ) {
                lazyLog.e( "GATT ERROR:", e );
            }
        }

        @Override
        public void onServicesDiscovered( final BluetoothGatt gatt,
                                          final int status ) {

            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if( checkGatt( gatt ) ) {
                        if (currentOp.onServicesDiscovered(status)) {
                            nextOp();
                        }
                    }
                }
            });
        }

        @Override
        public void onCharacteristicRead( final BluetoothGatt gatt,
                                          final BluetoothGattCharacteristic characteristic,
                                          final int status) {

            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (checkGatt(gatt)) {
                        if (currentOp.onCharacteristicRead(characteristic, status)) {
                            nextOp();
                        }
                    }
                }
            });
        }

        @Override
        public void onCharacteristicWrite( final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic,
                                           final int status) {

            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if( checkGatt( gatt ) ) {
                        if (currentOp.onCharacteristicWrite(characteristic, status)) {
                            nextOp();
                        }
                    }
                }
            });
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic characteristic) {

            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if( checkGatt( gatt ) ) {
                        if (currentOp.onCharacteristicChanged(characteristic)) {
                            nextOp();
                        }
                    }
                }
            });
        }

        @Override
        public void onDescriptorRead( final BluetoothGatt gatt,
                                      final BluetoothGattDescriptor descriptor,
                                      final int status) {
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if( checkGatt( gatt ) ) {
                        if (currentOp.onDescriptorRead(descriptor, status)) {
                            nextOp();
                        }
                    }
                }
            });
        }

        @Override
        public void onDescriptorWrite( final BluetoothGatt gatt,
                                       final BluetoothGattDescriptor descriptor,
                                       final int status) {
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if( checkGatt( gatt ) ) {
                        if (currentOp.onDescriptorWrite(descriptor, status)) {
                            nextOp();
                        }
                    }
                }
            });
        }
    };

    /**
     *  Represents A BLE device and connection state
     */
    public static class BLEDevice {
        public final int lifecycle;
        public final BluetoothDevice device;
        protected final BluetoothGatt gatt;

        protected boolean servicesDiscovered = false;
        protected int connectionState = BluetoothGatt.STATE_DISCONNECTED;

        public Date lastSeen;
        private int refCount = 0;
        private boolean stale = false;

        /** reference counting wrapper for optimally discarding gatt references as soon as possible.
         *
         * @return boolean indication of success
         */
        public synchronized boolean acquire() {
            if( stale ) {
                lazyLog.w( "Attempt to acquire stale device ", this);
            } else if( refCount++ == 0 ) {
                synchronized (deviceMap) {
                    deviceMap.put(device.getAddress(), this);
                }
            }
            return ! stale;
        }

        /** reference counting wrapper for optimally discarding gatt references as soon as possible.
         *
         * @return boolean indication of success
         */
        public synchronized boolean release() {
            final boolean ret = ! stale;
            if( stale ) {
                lazyLog.w( "Attempt to release stale device ", this);
            } else if( --refCount == 0 ) {
                close();
                stale = true;
                synchronized (deviceMap) {
                    deviceMap.remove(device.getAddress());
                    if (self.currentDevice == this) {
                        self.currentDevice = null;
                    }
                }
            }

            return ret;
        }

        /** synchronized getter for stale state. stale-> not accepted for BLERequests.
         *
         * @return if device is stale
         */
        public synchronized boolean isStale() {
            return stale;
        }

        public static BLEDevice byAddress( final String address ) {
            synchronized (deviceMap) {
                return deviceMap.get( address );
            }
        }

        private BLEDevice( final BluetoothDevice device, final Date seen, int lifecycle ) {
            this.lifecycle = lifecycle;
            this.lastSeen = seen;
            this.device = device;
            // important to actually connect: http://stackoverflow.com/questions/25848764/onservicesdiscoveredbluetoothgatt-gatt-int-status-is-never-called
            this.gatt = device.connectGatt( context, true, self.gattCallback );
            lazyLog.a( this.gatt != null, "BLEDevice failed at connectGatt()!");
            this.notifyUUIDs = new HashSet<>();
            this.pendingUUID = null;
        }

        /** Set of (notify enabled) characteristics.
         * Sync on UI thread
         * insert: write descriptor ENABLE_NOTIFICATION_VALUE, setCharacteristicNotification true
         * remove: setCharacteristicNotification false
         */
        private Set<Pair<UUID,UUID>> notifyUUIDs;
        private Pair<UUID,UUID> pendingUUID;

        public BluetoothGattCharacteristic getCharacteristic( final Pair<UUID,UUID> uuidPair ) {
            // lookup service & characteristic (FIXME: null check)
            final BluetoothGattService service =
                    gatt.getService( uuidPair.first );
            if( service != null ) {
                final BluetoothGattCharacteristic characteristic =
                        service.getCharacteristic(uuidPair.second);
                return characteristic;
            } else {
                return null;
            }
        }

        /** Cancel listening to the given service-characteristic uuid pair
         *  Only cancels device-local listening
         * @param notifyUUID service-characteristic pair for which to disable notification.
         */
        private void stopListenUUID( final Pair<UUID, UUID > notifyUUID ) {
            notifyUUIDs.remove( notifyUUID );

            final BluetoothGattCharacteristic notifyCharacteristic = getCharacteristic( notifyUUID );

            lazyLog.a(gatt.setCharacteristicNotification(notifyCharacteristic, false),
                    "Failed to disable notification for service: ", notifyUUID.first,
                    " characteristic: ", notifyUUID.second
            );
        }

        private static final UUID notifyDescriptorUUID =
                UUID.fromString( "00002902-0000-1000-8000-00805f9b34fb" );

        /** Sets up characteristic notification on the device.
         * Also stashes UUID pair for complete
         * @param notifyUUID service-characteristic pair for which to enable notification.
         */
        private void dispatchListenUUID( final Pair<UUID, UUID > notifyUUID ) {
            // Check for pending ops--mostly a debug warning since interrupts are allowed.
            if( pendingUUID != null ) {
                lazyLog.a(false,
                        "Started new listen operation uncleanly. old service: ", pendingUUID.first,
                        " old characteristic: ", pendingUUID.second);
            }

            final BluetoothGattCharacteristic characteristic = getCharacteristic( notifyUUID );

            // check for notification failure
            lazyLog.a(gatt.setCharacteristicNotification(characteristic, true),
                    "Failed to disable notification for service: ", notifyUUID.first,
                    " characteristic: ", notifyUUID.second);

            final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    notifyDescriptorUUID );

            descriptor.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE );

            lazyLog.a(gatt.writeDescriptor(descriptor), " Write notify descriptor for service "
                    , notifyUUID.first, " characteristic ", notifyUUID.second);

            pendingUUID = notifyUUID;
        }

        /** stashes the current UUID pair as active.
         *
         * @param gattStatus callback return value
         */
        private void completeListenUUID( final int gattStatus ) {
            lazyLog.a(pendingUUID != null,
                    "Error, complete listen to UUID with no dispatch!!!"
            );
            if( gattStatus == BluetoothGatt.GATT_SUCCESS ) {
                notifyUUIDs.add(pendingUUID);
            } else {
                lazyLog.e( "Error, non-success gatt status (", gattStatus, ") for service ",
                        pendingUUID.first, " characteristic ", pendingUUID.second );
            }
            pendingUUID = null;
        }

        /**
         * Cleans up and closes the BLE context and any pending state.
         */
        private void close() {
            // see discussion at https://code.google.com/p/android/issues/detail?id=58607
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    lazyLog.i("Close: Disconnecting ", device.getAddress());
                    gatt.disconnect();
                }
            });
            UIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    lazyLog.i("Close: closing ", device.getAddress());
                    gatt.close();
                }
            },100);
        }
    }

    /** Request object for interacting with BLEAgent
     * Executed one-at-a-time by the agent. The Agent switches to the specified device, sets it
     * active, and then delegates request construction to the request. The BLEAgent then delegates
     * received data to the request until the request indicates completion, or timeout occurs.
     *
     * note: all calls *probably* happen on the UI thread.
     *
     * As such, BLEAgent is mostly 'dumb' -- coms logic mostly lies in request.
     */
    abstract static public class BLERequest {
        final public BLEDevice device;
        final int timeout;
        final static String TAG = "BLERequest";
        public boolean stale = false;

        /** Notification characteristics which the API should hook
         *
         * @return Set of service-characteristic UUID pairs on which to receive notifications.
         */
        public Set<Pair<UUID,UUID>> listenUUIDs() {
            return Collections.emptySet();
        }

        /** Specify device an timeout
         *
         * @param device BLEDevice required for this request, or null for no device.
         * @param timeout milliseconds before this request will timeout after it's begun.
         */
        public BLERequest( final BLEDevice device, final int timeout ){
            this.timeout = timeout;
            this.device = device;
            if( device != null ) {
                lazyLog.a(device.acquire(), "Failed to acquire device!! ", device);
            }
        }

        /** Send data using the current BLEAgent handles.
         * Note, device services may not be discovered until the dispatch context.
         * look at opBuildStack and buildOps.
         *
         * @param agent BLEAgent instance currently executing this request.
         * @return indication of it the request has been completed.
         */
        abstract public boolean dispatch(final BLEAgent agent);

        /** receive forwarded response from device, override per type.
         *
         * @param characteristic characteristic associated with the gatt event,
         * @param status status associated with the gatt event.
         * @return indication of if the request has been completed.
         */
        public boolean onReceive( final BluetoothGattCharacteristic characteristic, int status ) {
            lazyLog.e( "Unexpected Characteristic received ",
                    characteristic.getUuid(), " status: ", status, " value: ",
                            bytesToHex( characteristic.getValue() )
            );
            return false;
        }

        public boolean onReceive( final BluetoothGattDescriptor descriptor, int status ) {
            lazyLog.e( "Unexpected Characteristic received ",
                            descriptor.getUuid(), " status: ", status, " value: ",
                            bytesToHex( descriptor.getValue() )
            );
            return false;
        }

        public boolean onReceive( final BLEDevice device ) {
            lazyLog.e( "Unexpected Device received ", device.device.getAddress() );
            return false;
        }

        /** Called on device failure primarily
         *
         */
        public void onFailure() {}

        /** final call at end of processing. release BLEDevice at minimum.
         *
         */
        public synchronized boolean close() {
            if( ! stale && device != null ) {
                device.release();
                stale = true;
            }
            return stale;
        }

        public synchronized boolean isStale() {
            if( stale == false && device != null ) {
                stale = device.isStale();
            }
            return stale;
        }
    }

    private static final Map<String, BLEDevice> deviceMap = new HashMap<>();

    /**
     * Close down singleton and any open devices
     * FIXME: refresh logic
     */
    public static void release() {
        handle(new BLERequest(null, 1000) {
            @Override
            public boolean dispatch(BLEAgent agent) {
                try {
                    mutex.acquire();
                    refCount -= 1;
                    if (refCount > 0) {
                        lazyLog.i("BLEAgent::release(): open elsewhere (refCount): ", refCount);
                    } else if (refCount < 0) {
                        lazyLog.e("BLEAgent::release(): Negative ref count!!!!!!!", refCount);
                    } else {
                        lazyLog.i("BLEAgent::release(): closing BLEAgent");
                        context = null;
                        UIHandler = null;
                        self.adapter = null;
                        for( BLEDevice dev : deviceMap.values()) {
                            lazyLog.e("BLEDevice Still in use! ", dev);
                        }
                        lifeCycleCount += 1;
                        lazyLog.i("BLEAgent::release(): Closed BLEAgent: lifecycle, ", lifeCycleCount );
                    }
                    mutex.release();
                } catch (InterruptedException e) {
                    lazyLog.e("Unexpected interrupt in close()");
                }
                return true;
            }
        });
    }

    private BluetoothAdapter adapter = null;
    private final BluetoothAdapter.LeScanCallback scanCallback =
            new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan( final BluetoothDevice device, int rssi, byte[] scanRecord ) {
            lazyLog.d("Found Device: ", device.getAddress());
            updateDevice(device);
        }
    };

    public void startScan() {
        lazyLog.d( "Start ble scan");
        lazyLog.a(adapter.startLeScan(scanCallback), " Scan start");
    }

    public void stopScan() {
        lazyLog.d( "stop ble scan");
        adapter.stopLeScan(scanCallback);
    }

    //new request queue!
    private final Queue<BLERequest> requestQueue = new ConcurrentLinkedQueue<>();
    private BLERequest currentRequest = null;
    private BLEDevice currentDevice = null;

    /**
     * Interface for doing async things with a BLE device within a request context.
     * Return true to proceed to the nextOp();
     */
    static interface AsyncOp extends Runnable {
        public boolean onConnectionStateChange(int status, int newState);
        public boolean onServicesDiscovered(int status);
        public boolean onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status);
        public boolean onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status);
        public boolean onCharacteristicChanged(BluetoothGattCharacteristic characteristic);
        public boolean onDescriptorRead(BluetoothGattDescriptor descriptor, int status);
        public boolean onDescriptorWrite(BluetoothGattDescriptor descriptor, int status);
    }

    /** Base class offering error logging implementation of all callbacks.
     * TODO: make a RetryOp class that redirects random messages to retrying the current op.
     */
    abstract static class DebugOp implements AsyncOp {
        final static String TAG = "BLEAgent::DebugOp";

        protected static String describeState(int state) {
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
                    break;
                default:
                    msg = "<<Unknown>>";
                    break;
            }
            msg += "(" + state + ")";
            return msg;
        }

        @Override
        public boolean onConnectionStateChange(int status, int newState) {
            lazyLog.e( "Unexpected connection state change: ", describeState( newState ),
                    " status: ", status );
            return false;
        }

        @Override
        public boolean onServicesDiscovered(int status) {
            lazyLog.e( "Unexpected service discovery: ", status);
            return false;
        }

        @Override
        public boolean onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {
            lazyLog.e( "Unexpected characteristic read: ", characteristic.getUuid(), " ("
                   , status, ") ", bytesToHex( characteristic.getValue() ) );
            return false;
        }

        @Override
        public boolean onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {
            lazyLog.e( "Unexpected characteristic write: ", characteristic.getUuid(), " ("
                   , status, ") ", bytesToHex( characteristic.getValue() ) );
            return false;
        }

        @Override
        public boolean onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
            lazyLog.e( "Unexpected characteristic change: ", characteristic.getUuid(), " ",
                    bytesToHex( characteristic.getValue() ) );
            return false;
        }

        @Override
        public boolean onDescriptorRead(BluetoothGattDescriptor descriptor, int status) {
            lazyLog.e( "Unexpected descriptor read: ", descriptor.getUuid(), " ("
                   , status, ") ", bytesToHex( descriptor.getValue() ) );
            return false;
        }

        @Override
        public boolean onDescriptorWrite(BluetoothGattDescriptor descriptor, int status) {
            lazyLog.e( "Unexpected descriptor write: ", descriptor.getUuid(), " ("
                   , status, ") ", bytesToHex( descriptor.getValue() ) );
            return false;
        }
    }

    /**
     * Just name tag override for now....
     */
    private abstract static class SetupOp extends DebugOp {
        final static String TAG = "BLEAgent::SetupOp";
    }

    private final Stack<AsyncOp> opStack = new Stack<>();
    private final Stack<AsyncOp> opBuildStack = new Stack<>();
    private AsyncOp currentOp = null;

    /**
     * convenience for inverting stack ordering
     */
    public void buildOps() {
        try {
            //noinspection InfiniteLoopStatement
            while( true ) {
                opStack.push( opBuildStack.pop() );
            }
        } catch( EmptyStackException e ) {
        }
    }

    public void fifoOp( final AsyncOp op ) {
        opBuildStack.push( op );
    }

    public void stackOp( final AsyncOp op ) {
        opStack.push(op);
    }

    /** Schedules the next method on the op stack for the UI thread.
     * Should be invoked by each OP either in run() or in results of run
     * Note: OpStack is cleared by timeout
     */
    private boolean nextOp() {
        lazyLog.a(opBuildStack.size() == 0, "Op build stack not empty before next op!");
        try {
            currentOp = opStack.pop();
            UIHandler.post(currentOp);
            return true;
        } catch( EmptyStackException e ) {
            currentOp = null;
            return false;
        }
    }

    /** internal method to execute a request.
     * stacks ops to setup this requests
     */
    private void nextRequest() {
        opStack.clear();
        currentOp = null;
        try {
            if( currentRequest != null ) {
                lazyLog.d( "BLEAgent: Request completed: ", currentRequest, " at ", new Date());
                currentRequest.close();
            }

            final BLERequest request;
            do {
                BLERequest tmp = requestQueue.remove();

                if( tmp.isStale() ) {
                    lazyLog.e( "Dropping request with stale device: ", tmp );
                    tmp.onFailure();
                } else {
                    request = tmp;
                    break;
                }
            } while( true );

            currentRequest = request;
            lazyLog.d( "BLEAgent: Preparing request: ", currentRequest );

            stackOp(new SetupOp() {
                @Override
                public void run() {
                    if( currentRequest != request )
                    lazyLog.a(currentRequest == request,
                            "nextRequest(): currentRequest != request");
                    lazyLog.d( "Starting request ", request, " at ", new Date());
                    if (request.dispatch(BLEAgent.this)) {
                        nextRequest();
                    }
                }

                @Override
                public boolean onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
                    if( request.onReceive(characteristic, BluetoothGatt.GATT_SUCCESS )) {
                        nextRequest();
                    }
                    return false;
                }

                @Override
                public boolean onCharacteristicRead(BluetoothGattCharacteristic characteristic, int status) {
                    if( request.onReceive(characteristic, status )) {
                        nextRequest();
                    }
                    return false;
                }

                @Override
                public boolean onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {
                    if( request.onReceive(characteristic, status )) {
                        nextRequest();
                    }
                    return false;
                }

                @Override
                public boolean onDescriptorWrite(BluetoothGattDescriptor descriptor, int status) {
                    if( request.onReceive(descriptor, status )) {
                        nextRequest();
                    }
                    return false;
                }

                @Override
                public boolean onDescriptorRead(BluetoothGattDescriptor descriptor, int status) {
                    if( request.onReceive(descriptor, status )) {
                        nextRequest();
                    }
                    return false;
                }
            });

            buildOps();

            setDevice(request.device);

            UIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentRequest == request) {
                        lazyLog.w("BLEAgent: Request timed out: ", request, " at ", new Date());
                        request.onFailure();
                        nextRequest();
                    }
                }
            }, request.timeout);

            nextOp();
        } catch( NoSuchElementException e ) {
            currentRequest = null;
        }
    }

    /** make the specified device active
     * Pushes AsyncOps to the opstack
     * @param device device to activate.
     */
    private void setDevice( final BLEDevice device ) {

        //disconnect
        if( device != currentDevice && currentDevice != null && device != null ) {
            fifoOp(new SetupOp() {
                @Override
                public void run() {

                    lazyLog.d("Disconnecting from device: ", currentDevice.device.getAddress());

                    // disable notifications
                    for (Pair<UUID, UUID> notifyUUID : currentDevice.notifyUUIDs) {
                        currentDevice.stopListenUUID(notifyUUID);
                    }

                    currentDevice.gatt.disconnect();
                }

                @Override
                public boolean onConnectionStateChange(int status, int newState) {

                    final boolean ret = newState == BluetoothGatt.STATE_DISCONNECTED;
                    currentDevice.connectionState = newState;
                    if (!ret) {
                        lazyLog.w("Not disconnected??!? ", describeState(newState));
                        lazyLog.w("Retrying disconnect...", currentDevice.device.getAddress());
                        currentDevice.gatt.disconnect();
                    } else {
                        lazyLog.d("Disconnected from ", currentDevice.device.getAddress());
                    }
                    return newState == BluetoothGatt.STATE_DISCONNECTED;
                }
            });
        }


        if( device != null ) {
            // connect
            if( device.connectionState != BluetoothGatt.STATE_CONNECTED ) {
                fifoOp(new SetupOp() {
                    @Override
                    public void run() {
                        lazyLog.d( "Connecting to device ", device.device.getAddress());
                        currentDevice = device;
                        lazyLog.a(device.gatt.connect(), "gatt-connect to device: ", device.device.getAddress());
                    }

                    @Override
                    public boolean onConnectionStateChange(int status, int newState) {
                        boolean ret = newState == BluetoothGatt.STATE_CONNECTED;
                        device.connectionState = newState;
                        if (!ret) {
                            lazyLog.i( "Retrying connection. received: ", describeState(newState));
                            currentDevice.gatt.connect();
                        } else {
                            lazyLog.d( "Connected to device ", device.device.getAddress());
                        }
                        return ret;
                    }
                });
            } else {
                fifoOp( new SetupOp() {
                    @Override
                    public void run() {
                        currentDevice = device;
                        nextOp();
                    }
                });
            }

            // discover devices
            if (!device.servicesDiscovered) {
                fifoOp( new SetupOp() {
                    @Override
                    public void run() {
                        lazyLog.d( "Discovering services for device "
                               ,  device.device.getAddress());
                        device.gatt.discoverServices();
                    }

                    @Override
                    public boolean onServicesDiscovered(int status) {
                        final boolean ret = status == BluetoothGatt.GATT_SUCCESS;
                        if( ret ) {
                            lazyLog.d( "Discovered services for device "
                                   , device.device.getAddress());
                            device.servicesDiscovered = true;
                        } else {
                            lazyLog.w( "Failed to discover services for device ",
                                    device.device.getAddress(), ". retrying..." );
                            device.gatt.discoverServices();
                        }
                        return ret;
                    }

                    //TODO: We should probably roll this into a RetryOp class (see DebugOp)
                    @Override
                    public boolean onConnectionStateChange(int status, int newState) {
                        run();
                        return super.onConnectionStateChange(status, newState);
                    }
                });
            }

            // enable notifications (FIXME: abstract)
            final Set<Pair<UUID, UUID>> targetUUIDs = currentRequest.listenUUIDs();
            final Set<Pair<UUID, UUID>> insertUUIDs =
                    new HashSet<>( targetUUIDs );
            final Set<Pair<UUID, UUID>> removeUUIDs;

            if( currentDevice != null ) {
                removeUUIDs = new HashSet<>(currentDevice.notifyUUIDs);
                removeUUIDs.removeAll(targetUUIDs);
                insertUUIDs.removeAll( currentDevice.notifyUUIDs );
            } else {
                removeUUIDs = Collections.emptySet();
            }

            if( removeUUIDs.size() != 0 ) {
                fifoOp(new SetupOp() {
                    @Override
                    public void run() {

                        for(Pair<UUID, UUID> notifyUUID : removeUUIDs) {
                            currentDevice.stopListenUUID( notifyUUID );
                        }
                        nextOp();
                    }
                });
            }

            /*
                Note, we may want to batch these differently:
                Currently our state may end up out of sync(on  request abort), but it does so
                pessimistically (i.e., we'd be listening to the characteristic, just not know we
                are).

                A more reliable strategy would be to have a single op along the pseudo code:

                try {
                    connect()
                    map( listen, uuids )
                } catch {
                    retry()
                }
             */
            for(final Pair<UUID, UUID> notifyUUID : insertUUIDs) {
                fifoOp( new SetupOp() {
                    @Override
                    public void run() {
                        lazyLog.d( "enabling listening to device ",  device.device.getAddress()
                               , " for service ", notifyUUID.first
                               , " characteristic ", notifyUUID.second );

                        currentDevice.dispatchListenUUID(notifyUUID);
                    }

                    @Override
                    public boolean onDescriptorWrite(BluetoothGattDescriptor descriptor,
                                                     int status) {
                        final boolean ret = status == BluetoothGatt.GATT_SUCCESS;
                        currentDevice.completeListenUUID( status );
                        if( ! ret ) {
                            lazyLog.w( "retrying notification for ", notifyUUID);
                            currentDevice.dispatchListenUUID( notifyUUID );
                        } else {
                            lazyLog.d( "enabled listening to device "
                                   ,  device.device.getAddress()
                                   , " for service ", notifyUUID.first
                                   , " characteristic ", notifyUUID.second );
                        }
                        return ret;
                    }

                    @Override
                    public boolean onConnectionStateChange(int status, int newState) {
                        lazyLog.w("Notification observed state: ", newState);
                        lazyLog.a(status == BluetoothGatt.GATT_SUCCESS,
                                "Non-success state status for state ",
                                newState, " (", status, ")" );
                        switch (newState) {
                            case BluetoothGatt.STATE_CONNECTED:
                                run();
                                break;
                            case BluetoothGatt.STATE_DISCONNECTED:
                                currentDevice.completeListenUUID( BluetoothGatt.GATT_FAILURE );
                                currentDevice.gatt.connect();
                                break;
                        }
                        return false;
                    }
                });
            }
        }

        buildOps();
    }

    /** create -or- update internal wrapper for said device
     *  async notifies current request of new device.
     * @param device Bluetooth device to activate.
     */
    private void updateDevice( final BluetoothDevice device ) {
        final Date now = new Date();
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                final String address = device.getAddress();
                BLEDevice dev = BLEDevice.byAddress(address);

                if (dev != null) {
                    if(dev.device != device) {
                        lazyLog.e(dev.device == device, "BluetoothDevice objects don't match ",
                                device, " != ", dev.device);
                        lazyLog.a(dev.lifecycle==lifeCycleCount, " Device is from another age!!, ",
                                dev.lifecycle, " != ", lifeCycleCount, " for ", dev);
                    }
                    dev.lastSeen = now;
                } else {
                    dev = new BLEDevice(device, now, lifeCycleCount);
                }

                dev.acquire();
                lazyLog.a(currentRequest != null, "No active request!!!");
                if (currentRequest != null && currentRequest.onReceive(dev)) {
                    nextRequest();
                }
                dev.release();
            }
        });
    }

    /** Queue a request for the agent to handle.
     * Requests are handled in-order, and are internally in-charge of notification.
     * @param request BLERequest to queue for execution.
     */
    public static void handle( final BLERequest request ) {
        UIHandler.post( new Runnable() {
            @Override
            public void run() {
                self.requestQueue.add(request);
                if( self.currentRequest == null ) {
                    self.nextRequest();
                }
            }
        });
    }

    /** Extends basic request object to scan for devices.
     *
     * Subclasses should implement filter() and onComplete() to find devices.
     */
    abstract static public class BLERequestScan extends BLERequest {
        public BLERequestScan( int timeout ) {
            super( null, timeout );
        }


        /** Implements scan-start logic and looping filter over cached devices.
         *
         * @param agent BLEAgent instance currently executing this request.
         * @return boolean indication of request completion (termination).
         */
        @Override
        public boolean dispatch( BLEAgent agent ) {
            //redirect current BLE map into handler.
            synchronized (deviceMap) {
                for (BLEDevice device : agent.deviceMap.values()) {
                    if (filter(device)) {
                        lazyLog.d("Exiting with pre-existing device: ", device.device.getAddress());
                        onComplete(device);
                        return true;
                    }
                }
            }
            agent.startScan();
            return false;
        }

        /** Redirects device discovery to filter, and handles completion if indicated by filter.
         *
         * @param device discovered BLEDevice object.
         * @return  boolean indication of request completion (termination).
         */
        @Override
        public boolean onReceive(final BLEDevice device) {
            final boolean ret = filter( device );
            if( ret ) {
                //FIXME: reference cheating
                self.stopScan();
                /*
                  TODO: evaluate posting completion on a separate thread to allow next request to
                  execute sooner.
                */
                onComplete(device);
            }
            return ret;
        }

        /** Stops scan on request timeout and calls completion
         *
         */
        @Override
        public void onFailure() {
            lazyLog.d( "BLE Scan abort.");
            onComplete( null );
            //FIXME: reference cheating
            self.stopScan();
        }

        /** Scan termination callback.
         * Override to receive device or null
         *
         * @param device specified BLEDevice or null for failed scan
         */
        abstract public void onComplete( final BLEDevice device );

        /** Return true to abort scan and deliver offered device to onComplete()
         *
         * @param device BLEDevice (from scan or cache)
         * @return end scan ( triggering onComplete() with this device ).
         */
        abstract public boolean filter( final BLEDevice device );
    }

    /** Convenience class to scan for a device by address.
     *
     */
    static abstract class BLERequestScanForAddress extends BLERequestScan {
        final protected String address;

        public BLERequestScanForAddress( final int timeout, final String address ) {
            super( timeout );
            this.address = address;
        }

        @Override
        public boolean filter(BLEDevice device) {
            return address.equals( device.device.getAddress() );
        }
    }
}
