package com.movo.wave;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import android.util.Pair;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Alexander Haase on 4/8/2015.
 */
public class WaveAgent {

    /** Enum describing wave operation byte codes
     * Op codes entered from spec.
     */
    public enum WaveOp {
        SET_NAME            ( (byte)0x81, (byte)0x21, (byte)0x01 ),
        SET_PASSWORD        ( (byte)0x85, (byte)0x25, (byte)0x05 ),
        SET_PERSONAL_INFO   ( (byte)0x83, (byte)0x23, (byte)0x03 ),
        READ_DATA_WEEK      ( (byte)0xC4, (byte)0x24, (byte)0x04 ),
        READ_DATA_DATE      ( (byte)0xC7, (byte)0x31, (byte)0x0B ),
        READ_DEVICE         ( (byte)0xC6, (byte)0x26, (byte)0x06 ),
        READ_DEVICE_TOTAL   ( (byte)0xC8, (byte)0x30, (byte)0x0A ),
        RESET_TO_DEFAULTS   ( (byte)0x87, (byte)0x27, (byte)0x07 ),
        CLEAR_DATA          ( (byte)0x88, (byte)0x28, (byte)0x09 ),
        SET_TIME            ( (byte)0xC2, (byte)0x22, (byte)0x02 ),
        GET_TIME            ( (byte)0x89, (byte)0x29, (byte)0x09 );


        public final byte CODE;     //* Send op code
        public final byte SUCCESS;  //* Response success op code
        public final byte FAILURE;  //* Response failure op code

        private WaveOp( byte code, byte success, byte failure ) {
            this.CODE = code;
            this.SUCCESS = success;
            this.FAILURE = failure;
        }
    }


    /** Writes an op, receives notify.
     * Recommended strategy to prevent code bloat: subclass for each op, and add appropriate
     * abstract onComplete(...) methods for return data types, using byte[] onComplete as a parser.
     */
    abstract public static class BLERequestWaveOp extends BLEAgent.BLERequest {

        static final String TAG = "BLERequestWaveOp";

        static final UUID notifyCharacteristicUUID =
                UUID.fromString( "0000ffe4-0000-1000-8000-00805f9b34fb" );
        static final UUID notifyServiceUUID =
                UUID.fromString( "0000ffe0-0000-1000-8000-00805f9b34fb" );
        static final UUID writeCharacteristicUUID =
                UUID.fromString( "0000ffe9-0000-1000-8000-00805f9b34fb" );
        static final UUID writeServiceUUID =
                UUID.fromString( "0000ffe5-0000-1000-8000-00805f9b34fb" );

        static public Set<Pair<UUID,UUID>> listenUUIDs = new HashSet<>();
        static public final Pair<UUID, UUID> writeUUIDs;

        static {
            final HashSet<Pair<UUID,UUID>> tmpUUIDs = new HashSet<>();
            tmpUUIDs.add( new Pair<>( notifyServiceUUID, notifyCharacteristicUUID ) );
            listenUUIDs = Collections.unmodifiableSet( tmpUUIDs );
            writeUUIDs = new Pair<>( writeServiceUUID, writeCharacteristicUUID );
        }

        static protected void logFailure( final boolean success, final String msg ) {
            if( ! success ) {
                Log.e( TAG, "Failure: " + msg );
            }
        }

        // Meat and potatoes of this class: setup an array with op, expect response with op.
        final protected WaveOp op;
        final protected byte[] message;

        final private byte[] response = new byte[ 256 ];
        private int responseSize = 0;

        private boolean buildResponse( final byte[] responsePart ) {
            System.arraycopy( responsePart, 0, response, responseSize, responsePart.length );
            responseSize += responsePart.length;
            return responseSize >= MarshalByte.SIZE.parse( response );
        }

        /** Public constructor for banging out a message by hand. Recommend subclass instead.
         *
         * @param op WaveOp operation enum.
         * @param body byte array payload to send.
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         */
        public BLERequestWaveOp( final WaveOp op,
                                 final byte[] body,
                                 BLEAgent.BLEDevice device,
                                 int timeout ){
            this(op, body.length, device, timeout);
            System.arraycopy(body, 0, message, 2, body.length);
        }

        /** Subclass convenience constructor.
         *
         * Allows constructing an empty body of set size. Message checksum constructed during
         * dispatch(). Note: For time-sensitive message contents, construct body during dispatch().
         *
         * @param op WaveOp operation enum
         * @param bodySize length of body section of payload.
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         */
        protected BLERequestWaveOp( final WaveOp op,
                                 int bodySize,
                                 BLEAgent.BLEDevice device,
                                 int timeout ){
            super( device, timeout );
            // Spec says limit is 200 bytes
            logFailure( bodySize <= 200, "Message length " + bodySize + " too long!!!!");
            this.op = op;
            message = new byte[ bodySize + 3 ];
            MarshalByte.OP.put( message, op.CODE );
            MarshalByte.SIZE.put( message, bodySize );
        }

        /** Calculates checksum given a raw message per wave device spec.
         *
         * @param message raw byte array message
         * @return checksum byte
         */
        public static byte calcChecksum( final byte[] message ) {
            logFailure( message.length >= 3, "Message too small! " + message.length);
            if( message.length == 3 ) {
                return 0x00;
            } else {
                byte ret = message[ 2 ];
                final int limit = 2 + MarshalByte.SIZE.parse(message);
                logFailure( limit < message.length, " Message length exceeds buffer!" );
                for( int index = 3; index < limit; index++ ) {
                    ret ^= message[ index ];
                }
                return ret;
            }
        }

        /** retrieves checksum given a raw message per wave device spec.
         *
         * NOTE: response bodies can be longer than the message--DO NOT NAIVELY ASSUME THEY MATCH.
         *
         * @param message raw byte array message
         * @return checksum byte
         */
        public static byte getChecksum( final byte[] message ) {
            logFailure( message.length >= 3, "Message too short! " + message.length );
            final int index = 2 + MarshalByte.SIZE.parse(message);
            logFailure( message.length >= index, "index larger than message " + index
                    + " " + message.length );
            return message[ index ];
        }

        /** Calculate checksum, send message via characteristic.
         *
         * @param agent BLEAgent for request
         * @return always false--waiting for response!
         */
        @Override
        public boolean dispatch(BLEAgent agent) {
            message[ message.length -1 ] = calcChecksum(message);

            final BluetoothGattCharacteristic characteristic = device.getCharacteristic( writeUUIDs );
            characteristic.setValue(message);

            Log.d( TAG, "Sending message: " + BLEAgent.bytesToHex( message ));
            logFailure(device.gatt.writeCharacteristic(characteristic),
                    "start write to wave");
            return false;
        }

        /** De-multiplex write and notify characteristics, warn if other characteristic observed.
         *
         * Switch on characteristic UUID:
         *   - on write failure abort
         *   - on notify, checksum and verify op status, forward to onComplete( with value ).
         *
         * @param characteristic characteristic associated with the gatt event,
         * @param status status associated with the gatt event.
         * @return true on write failure, any notify, or unrecognized UUID; false on write success.
         */
        @Override
        public boolean onReceive(BluetoothGattCharacteristic characteristic, int status) {

            // on write
            if( characteristic.getUuid().equals( writeCharacteristicUUID )) {
                final boolean ret = BluetoothGatt.GATT_SUCCESS != status;
                final byte[] sent = characteristic.getValue();
                logFailure( sent.equals( message ), "Sent message doesn't match constructed! Sent: "
                        + BLEAgent.bytesToHex(sent) + " Constructed: "
                        + BLEAgent.bytesToHex(message));

                if( ret ) {
                    Log.e( TAG, "FAILED: write to wave device: " + device.device.getAddress() );
                    onCompletion( false, null );
                }
                return ret;

                // on notify
            } else if( characteristic.getUuid().equals( notifyCharacteristicUUID )) {
                logFailure(status == BluetoothGatt.GATT_SUCCESS, "complete notify from wave");
                final byte[] value = characteristic.getValue();
                Log.d(TAG, "Wave response: " + BLEAgent.bytesToHex(value));

                if( ! buildResponse( value ) ) {
                    Log.d( TAG, "Partial message detected. " + responseSize +
                            " of " + MarshalByte.SIZE.parse( response ) + " bytes.");
                    return false;
                }

                final byte checksum = getChecksum( response );
                final byte check = calcChecksum( response );

                boolean success = check == checksum;

                logFailure(success, "Checksum mismatch. received "
                        + BLEAgent.byteToHex(checksum) + " expected "
                        + BLEAgent.byteToHex(check));

                if( ! success ) {
                    Log.d( TAG, "Checksum offset = " +( check - checksum ));
                }

                final byte responseCode = (byte) MarshalByte.OP.parse( response );

                if( responseCode == op.FAILURE ) {
                    logFailure( false, "Received failure response code from wave.");
                    success = false;
                } else if( responseCode == op.SUCCESS ) {
                    Log.d( TAG, " wave indicates success.");
                } else {
                    logFailure(false, "Mismatch ops, received "
                            + BLEAgent.byteToHex( responseCode ) + " expected "
                            + BLEAgent.byteToHex( op.SUCCESS) + " or "
                            + BLEAgent.byteToHex( op.FAILURE ));
                    success = false;
                }

                onCompletion( success, response );

                return true;

                // default case: unexpected input!
            } else {
                return super.onReceive( characteristic, status );
            }
        }

        @Override
        public Set<Pair<UUID, UUID>> listenUUIDs() {
            return listenUUIDs;
        }

        /** Override to catch request complete
         *
         * @param success boolean indication of state.
         * @param response raw response byte array or null.
         */
        abstract void onCompletion( boolean success, byte[] response );
    }

    static public byte[] waveName( final String name ) {
        final byte[] ret = new byte[15];
        final char[] chars = name.toCharArray();
        final int copyLimit = Math.min(ret.length, chars.length);
        for( int index = 0; index < copyLimit; index++ ) {
            ret[ index ] = (byte) chars[ index ];
        }
        for( int index = copyLimit; index < ret.length; index++ ) {
            ret[ index ] = (byte) '#';
        }
        return ret;
    }

    /** Enum for marshalling byte fields for byte array message exchanges with wave devices.
     *
     * Auto-detects sign conversion based on range.
     *
     * index: raw index in message byte array.
     * min: lower (inclusive) bounds for byte value.
     * max: upper (inclusive) bounds for byte value.
     * offset: int offset to apply to byte value (additive on parse, subtractive on put).
     */
    enum MarshalByte {

        //General message stuff
        OP                  (0, -128, 127, 0),
        SIZE                (1, 0, 200, 0),

        // date related
        YEAR                (2, 0, 99, 100),
        MONTH               (3, 1, 12, -1 ),
        DATE                ( 4, 1, 31, 0 ),
        HOUR                ( 5, 0, 23, 0 ),
        MINUTE              ( 6, 0, 59, 0 ),
        SECOND              ( 7, 0, 59, 0 ),
        DAY                 ( 8, 1, 7, -1 ),

        //personal info related
        GENDER              ( 35, 0, 1, 0 ),
        HEIGHT_CM           ( 36, 0, 250, 0),
        WEIGHT_KG           ( 37, 0, 200, 0),
        STRIDE_CM           ( 38, 0, 150, 0),
        RUNNING_STRIDE_CM   ( 39, 0, 200, 0),
        SLEEP_BEGIN_HOUR    ( 40, 0, 23, 0 ),
        SLEEP_BEGIN_MINUTE  ( 41, 0, 59, 0),
        SLEEP_END_HOUR      ( 42, 0, 23, 0 ),
        SLEEP_END_MINUTE    ( 43, 0, 59, 0),
        BT_AUTO_DISCONNECT  ( 46, 0, 1, 0 ),
        BT_AUTO_TIMEOUT     ( 47, 0, 120, 0),

        //data related
        DATA_REQUEST_DAY    ( 2, 1, 7, -1),
        DATA_REQUEST_HOUR   ( 3, 0, 23, 0 ),
        DATA_REQUEST_QTY    ( 4, 1, 3, 0 ),

        DATA_RESPONSE_YEAR  ( 2, 0, 99, 100 ),
        DATA_RESPONSE_MONTH ( 3, 1, 12, -1 ),
        DATA_RESPONSE_DATE  ( 4, 1, 31, 0 ),
        ;

        final static String TAG = "WaveAgent.MarshalByte";

        private final int index;    //* index: raw index in message byte array.
        private final int min;      //* min: lower (inclusive) bounds for byte value.
        private final int max;      //* max: upper (inclusive) bounds for byte value.
        private final int offset;   //* offset: int offset to apply to byte value.

        private MarshalByte(final int index, final int min, final int max, final int offset) {
            if( ( min < 0 && max > 127 ) || min < -128 || max > 255 || min > max ) {
                Log.e( TAG, "Error, illegal range for casting: [" + min + "," + max + "]" );
            }
            this.index = index;
            this.min = min;
            this.max = max;
            this.offset = offset;
        }

        private void checkRange( int value ) {
            if( value < min || value > max ) {
                Log.e("WaveAgent::MessageByte", "Value exceeds bounds("
                        + min + "," + max + "): " + value);
            }
        }

        /** Retrieve integer variant of byte value from message buffer
         *
         * @param buffer raw message byte array.
         * @param offset index offset within buffer.
         * @return marshaled integer value.
         */
        public int parse( byte[ ] buffer, int offset ) {
            final int value;
            if( max < 128 ) {
                value = buffer[index + offset];
            } else {
                value = (int) buffer[index + offset] & 0xFF;
            }
            checkRange( value );
            return value + this.offset;
        }

        /** Retrieve integer variant of byte value from message buffer
         *
         * @param buffer raw message byte array.
         * @return marshaled integer value.
         */
        public int parse( byte[] buffer ) {
            return parse( buffer, 0 );
        }

        /** Place byte variant of integer value into message buffer
         *
         * @param buffer raw message byte array.
         * @param value integer to be marshaled to byte value.
         * @param offset index offset within buffer.
         */
        public void put( byte[] buffer, int value, int offset ) {
            value -= this.offset;
            if( value < min || value > max ) {
                Log.e("WaveAgent::MessageByte", "Value exceeds bounds("
                        + min + "," + max + "): " + value);
            }
            buffer[ index + offset ] = (byte)value;
        }

        /** Place byte variant of integer value into message buffer
         *
         * @param buffer raw message byte array.
         * @param value integer to be marshaled to byte value.
         */
        public void put( byte[] buffer, int value ) {
            put( buffer, value, 0 );
        }
    }

    /** Get date from device
     *
     */
    abstract static public class WaveRequestGetDate extends BLERequestWaveOp {

        /** Creates a new GET_TIME request for the wave device in question.
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         */
        public WaveRequestGetDate( final BLEAgent.BLEDevice device, final int timeout ) {
            super( WaveOp.GET_TIME, 0, device, timeout );
        }

        @Override
        void onCompletion(boolean success, byte[] response) {
            Date ret = null;
            if( success && response != null) {
                ret = new Date(
                        MarshalByte.YEAR.parse(response),
                        MarshalByte.MONTH.parse(response),
                        MarshalByte.DATE.parse(response),
                        MarshalByte.HOUR.parse(response),
                        MarshalByte.MINUTE.parse(response),
                        MarshalByte.SECOND.parse(response) );
            }
            onCompletion( success, ret );
        }

        /** Completion callback for operation
         *
         * @param success boolean indication of state.
         * @param date response date or null
         */
        abstract void onCompletion( boolean success, Date date );
    }

    /** Set time on device using local clock. Device time will be set in UTC.
     *
     */
    abstract static public class WaveRequestSetDate extends BLERequestWaveOp {

        /** Construct new SET_TIME request for the device
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         */
        public WaveRequestSetDate(final BLEAgent.BLEDevice device, final int timeout ) {
            super( WaveOp.SET_TIME, 7, device, timeout );
        }

        /** JIT message constructor
         *
         * Construct set time request right before BLEAgent sends the packet.
         *
         * @param agent BLEAgent for request
         * @return delegated to superclass
         */
        @Override
        public boolean dispatch(BLEAgent agent) {
            final Date date = new Date();
            MarshalByte.YEAR.put( message, date.getYear() );
            MarshalByte.MONTH.put( message, date.getMonth() );
            MarshalByte.DATE.put( message, date.getDate() );
            MarshalByte.HOUR.put( message, date.getHours() );
            MarshalByte.MINUTE.put( message, date.getMinutes() );
            MarshalByte.SECOND.put( message, date.getSeconds() );
            MarshalByte.DAY.put( message, date.getDay() );
            return super.dispatch(agent);
        }

        //TODO: parse set date response if we care
    }


    /** Set personal info request.
     *
     */
    static abstract public class WaveRequestSetPersonalInfo extends BLERequestWaveOp {
        static final int MALE = 0;
        static final int FEMALE = 1;

        /** Create Date object for hour/minute representation of sleep time begin or end.
         *
         * @param hour to set in result [0,24).
         * @param minute to set in result [0,60).
         * @return Date object with correct hour and date.
         */
        public static Date sleepTime( int hour, int minute ) {
            final Date ret = new Date();
            ret.setHours( hour );
            ret.setMinutes(minute);
            return ret;
        }

        /** Constructor for SET_PERSONAL_INFO request.
         *
         * Currently ignores and disables alarms, and sets BT auto timeout to 10 minutes.
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         * @param gender either MALE or FEMALE (see static members).
         * @param height_cm height measurement(cm).
         * @param weight_kg weight measurement(cm).
         * @param stride_cm stride measurement(cm).
         * @param running_stride_cm running stride measurement(cm).
         * @param sleepBegin hour&minute Date object.
         * @param sleepEnd hour&minute Date object.
         */
        public WaveRequestSetPersonalInfo( final BLEAgent.BLEDevice device,
                                           final int timeout,
                                           final int gender,
                                           final int height_cm,
                                           final int weight_kg,
                                           final int stride_cm,
                                           final int running_stride_cm,
                                           final Date sleepBegin,
                                           final Date sleepEnd ) {

            super( WaveOp.SET_PERSONAL_INFO, 47, device, timeout );

            //disable alarms --zero everything
            for( int index = 2; index < message.length ; index++ ) {
                message[ index ] = 0;
            }
            MarshalByte.GENDER.put( message, gender );
            MarshalByte.HEIGHT_CM.put( message, height_cm );
            MarshalByte.WEIGHT_KG.put( message, weight_kg );
            MarshalByte.STRIDE_CM.put( message, stride_cm );
            MarshalByte.RUNNING_STRIDE_CM.put( message, running_stride_cm );
            MarshalByte.SLEEP_BEGIN_HOUR.put(message, sleepBegin.getHours());
            MarshalByte.SLEEP_BEGIN_MINUTE.put(message, sleepBegin.getMinutes());
            MarshalByte.SLEEP_END_HOUR.put(message, sleepEnd.getHours());
            MarshalByte.SLEEP_END_MINUTE.put(message, sleepEnd.getMinutes());
            MarshalByte.BT_AUTO_DISCONNECT.put( message, 1 );
            MarshalByte.BT_AUTO_TIMEOUT.put( message, 10 );
        }
    }

    /** Data point representation. Encapsulates mode, value, and parsing.
     *
     */
    static class WaveDataPoint {
        enum Mode {
            DAILY,
            SPORTS,
            SLEEP,
            RESERVED
        }

        final public Mode mode;
        final public int value;
        final public Date date;

        /** Construct data point from enum and value. Probably not useful.
         *
         * @param mode mode according to spec.
         * @param value 14 bit integer value (unchecked).
         * @param date Date which data point represents.
         */
        public WaveDataPoint( final Mode mode, final int value, final Date date ) {
            this.mode = mode;
            this.value = value;
            this.date = date;
        }

        /** Parse mode from MSB byte (see spec)
         *
         * @param value MSB byte to parse.
         * @return mode according to spec.
         */
        static public Mode getMode( byte value ) {
            switch( value >>> 6 ) {
                case (0x00):
                    return Mode.DAILY;
                case (0x01):
                    return Mode.SLEEP;
                case (0x02):
                    return Mode.SPORTS;
                default:
                    Log.w( "WaveAgent.WaveDataPoint", "Data point with RESERVED value" );
                    return Mode.RESERVED;
            }
        }

        /** Construct data point from byte array message
         *
         * @param message raw message byte array.
         * @param offset index within byte array to MSB byte (unchecked).
         * @param date Date which data point represents.
         */
        public WaveDataPoint( final byte[] message, int offset, final Date date ) {
            this.mode = getMode( message[ offset ]);
            // shave off
            int tmp = message[ offset ] << 2;
            tmp <<= 6;
            tmp += offset;
            this.value = tmp;
            this.date = date;
        }

        /** Bulk parser for extracting points from a message.
         *
         * @param message raw message byte array.
         * @param offset index to first byte data point.
         * @param qty number of data points to parse (unchecked).
         * @param start time of first data point.
         * @return array of parsed WaveDataPoint objects.
         */
        public static WaveDataPoint[] parseResponse( final byte[] message,
                                              final int offset,
                                              final int qty,
                                              final Date start ) {
            int minutes = 0;
            final WaveDataPoint[] ret = new WaveDataPoint[qty];
            for( int index = 0 ; index < qty;  index += 1 ) {
                final Date date = new Date( start.getTime() + index * 1000 * 60 * 2 );
                ret[ index ] = new WaveDataPoint( message, index * 2 + offset, date );
            }

            return ret;
        }

        /** not sure this is correct
         *
         * @return "mode value date" string.
         */
        @Override
        public String toString() {
            return mode.toString() + " " + value + " " + date;
        }
    }

    /** Request for data by day of the week
     *
     */
    abstract static class WaveRequestDataByDay extends BLERequestWaveOp {

        final static String TAG = "WaveRequestDataByDate";

        protected final Date date;

        public WaveRequestDataByDay( final BLEAgent.BLEDevice device,
                                     final int timeout,
                                     final Date date ){
            super( WaveOp.READ_DATA_WEEK, 3, device, timeout );
            this.date = date;
            MarshalByte.DATA_REQUEST_DAY.put( message, date.getDay());
            MarshalByte.DATA_REQUEST_HOUR.put( message, date.getHours());
            MarshalByte.DATA_REQUEST_QTY.put( message, 3 );
            //TODO: Check date within the last 7 days.
        }

        @Override
        void onCompletion(boolean success, byte[] response) {
            WaveDataPoint[] ret = null;

            if( success ) {
                final int year = MarshalByte.DATA_RESPONSE_YEAR.parse( response );
                final int month = MarshalByte.DATA_RESPONSE_MONTH.parse( response );
                final int date = MarshalByte.DATA_RESPONSE_DATE.parse( response );

                Log.d( TAG, "Response date stamp: " + (1900 + year) + " " + month + " " + date );

                logFailure( year == this.date.getYear(), "Year mismatch "
                        + year + " " + this.date.getYear());
                logFailure( month == this.date.getMonth(), "Month mismatch "
                        + month + " " + this.date.getMonth());
                logFailure( date == this.date.getDate(), "Date mismatch "
                        + date + " " + this.date.getDate());

                final int qty = (MarshalByte.SIZE.parse(response) - 3)/2;

                Log.d( TAG, "Data by date for " + this.date + " returned " + qty + "data points." );

                ret = WaveDataPoint.parseResponse( response, 5, qty, this.date );

                for( final WaveDataPoint point : ret ) {
                    Log.d( TAG, "\t" + point );
                }
            }

            onCompletion( success, ret );
        }

        abstract void onCompletion( boolean success, WaveDataPoint[] data );
    }
}
