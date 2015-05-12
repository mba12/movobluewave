package com.movo.wave.comms;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Pair;

import com.movo.wave.util.LazyLogger;
import com.movo.wave.util.UTC;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

//Created by Alexander Haase on 4/8/2015.
/** Implements Wave-specific BLERequests and device detection.
 *
 * Builds upon BLEAgent API to interact reliably with BLE Devices. Requests are representative of
 * wave protocol spec. Discovering devices is handled by scanForWaveDevices until firmware updates
 * allow for more direct inspection.
 *
 * @author Alexander Haase
 */
public class WaveRequest {

    /** logger
     *
     */
    final static private LazyLogger lazyLog = new LazyLogger( "WaveRequest", true );


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
        GET_TIME            ( (byte)0x89, (byte)0x29, (byte)0x09 ),
        READ_VERSION        ( (byte)0x90, (byte)0x30, (byte)0x0A ),
        READ_SERIAL         ( (byte)0x91, (byte)0x31, (byte)0x0B ),
        ;


        public final byte CODE;     //* Send op code
        public final byte SUCCESS;  //* Response success op code
        public final byte FAILURE;  //* Response failure op code

        private WaveOp( byte code, byte success, byte failure ) {
            this.CODE = code;
            this.SUCCESS = success;
            this.FAILURE = failure;
        }
    }



    /** Abstract base class for interacting with wave device using WaveOp and byte[] buffers.
     *
     * Concisely, the class implements the pattern: Write an op, receive notify.
     *
     * Recommended strategy to prevent code bloat: subclass for each op, and add appropriate
     * abstract onComplete(...) methods for return data types, using byte[] onComplete as a parser.
     */
    abstract public static class BLERequestWaveOp extends BLEAgent.BLERequest {

        static final LazyLogger lazyLog = new LazyLogger( "BLERequestWaveOp", WaveRequest.lazyLog );

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
        static public final Pair<UUID, UUID> notifyUUIDs;

        static {
            final HashSet<Pair<UUID,UUID>> tmpUUIDs = new HashSet<>();
            tmpUUIDs.add( new Pair<>( notifyServiceUUID, notifyCharacteristicUUID ) );
            listenUUIDs = Collections.unmodifiableSet( tmpUUIDs );
            writeUUIDs = new Pair<>( writeServiceUUID, writeCharacteristicUUID );
            notifyUUIDs = new Pair<>( notifyServiceUUID, notifyCharacteristicUUID );
        }

        // Meat and potatoes of this class: setup an array with op, expect response with op.
        final protected WaveOp op;
        final protected byte[] message;

        //support for multi-part responses
        final private byte[] response = new byte[ 256 ];
        private int responseSize = 0;
        private int messageSize;

        /** Check for response completion, concatenate multipart-response.
         *
         * @param responsePart part (or possibly whole) of response body as byte array.
         * @return indication of completion--response byte array completed for parsing.
         */
        private boolean buildResponse( final byte[] responsePart ) {
            if( responseSize == 0 ) {
                final int code = MarshalByte.OP.parse( responsePart );
                if( code != op.FAILURE && code != op.SUCCESS ) {
                    lazyLog.w("Skipping bad preamble");
                    return false;
                }
                messageSize = MarshalByte.SIZE.parse( responsePart ) + 3;
            }
            System.arraycopy( responsePart, 0, response, responseSize, responsePart.length );
            responseSize += responsePart.length;
            return responseSize >= messageSize;
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
            lazyLog.a(bodySize <= 200, "Message length ", bodySize, " too long!!!!");
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
            lazyLog.a(message.length >= 3, "Message too small! ", message.length);
            if( message.length == 3 ) {
                return 0x00;
            } else {
                byte ret = message[ 2 ];
                final int limit = 2 + MarshalByte.SIZE.parse(message);
                lazyLog.a(limit < message.length, " Message length exceeds buffer!");
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
            lazyLog.a(message.length >= 3, "Message too short! ", message.length);
            final int index = 2 + MarshalByte.SIZE.parse(message);
            lazyLog.a(message.length >= index, "index larger than message ", index
                   , " ", message.length);
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

            lazyLog.d( "Sending message: ", BLEAgent.bytesToHex( message ));
            lazyLog.a(device.gatt.writeCharacteristic(characteristic),
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
                lazyLog.a(sent.equals(message), "Sent message doesn't match constructed! Sent: "
                       , BLEAgent.bytesToHex(sent), " Constructed: "
                       , BLEAgent.bytesToHex(message));

                if( ret ) {
                    lazyLog.e(  "FAILED: write to wave device: ", device.device.getAddress() );
                    onComplete(false, null);
                }
                return ret;

                // on notify
            } else if( characteristic.getUuid().equals( notifyCharacteristicUUID )) {
                lazyLog.a(status == BluetoothGatt.GATT_SUCCESS, "complete notify from wave");
                final byte[] value = characteristic.getValue();

                lazyLog.d( "Wave response: ", BLEAgent.bytesToHex(value));

                if( ! buildResponse( value ) ) {
                    lazyLog.d(  "Partial message detected. ", responseSize,
                            " of ", MarshalByte.SIZE.parse( response ), " bytes.");
                    return false;
                }

                final byte checksum = getChecksum( response );
                final byte check = calcChecksum( response );

                boolean success = check == checksum;

                lazyLog.a(success, "Checksum mismatch. received "
                       , BLEAgent.byteToHex(checksum), " expected "
                       , BLEAgent.byteToHex(check));

                final byte responseCode = (byte) MarshalByte.OP.parse( response );

                if( responseCode == op.FAILURE ) {
                    lazyLog.a(false, "Received failure response code from wave.");
                    success = false;
                } else if( responseCode == op.SUCCESS ) {
                    lazyLog.d(  " wave indicates success.");
                } else {
                    lazyLog.a(false, "Mismatch ops, received "
                           , BLEAgent.byteToHex(responseCode), " expected "
                           , BLEAgent.byteToHex(op.SUCCESS), " or "
                           , BLEAgent.byteToHex(op.FAILURE));
                    success = false;
                }

                final byte[] message;

                if( success ) {
                    message = new byte[ messageSize ];
                    System.arraycopy( response, 0, message, 0, messageSize);
                } else {
                    message = null;
                }

                onComplete( success, message );

                return true;

                // default case: unexpected input!
            } else {
                return super.onReceive( characteristic, status );
            }
        }

        /** Map timeout behavior to onComplete()
         *
         */
        @Override
        public void onFailure() {
            onComplete( false, null );
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
        protected abstract void onComplete( boolean success, byte[] response );
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
    public static enum MarshalByte {

        //General message stuff
        OP                  ( 0, -128, 127, 0 ),
        SIZE                ( 1, 0, 200, 0 ),

        // date related
        YEAR                ( 2, 0, 99, 2000 ),
        MONTH               ( 3, 1, 12, -1 ),
        DATE                ( 4, 1, 31, 0 ),
        HOUR                ( 5, 0, 23, 0 ),
        MINUTE              ( 6, 0, 59, 0 ),
        SECOND              ( 7, 0, 59, 0 ),
        DAY                 ( 8, 1, 7, 0 ) {
            /** Specialize parsing for Monday=1, Sunday=7
             *
             * @param buffer raw message byte array.
             * @param offset index offset within buffer.
             * @return index s.t. Sunday=0, Saturday=6
             */
            @Override
            public int parse(byte[] buffer, int offset) {
                // Values 0-6 already match, just mod
                return super.parse(buffer, offset) % 7;
            }

            /** Specialize parsing for Monday=1, Sunday=7
             *  Converts value from Sunday=0, Saturday=6
             * @param buffer raw message byte array.
             * @param value integer to be marshaled to byte value.
             * @param offset index offset within buffer.
             */
            @Override
            public void put(byte[] buffer, int value, int offset) {
                // Values 0-6 already match, replace 0 with 7.
                super.put(buffer, value == 0 ? 7 : value, offset);
            }
        },

        //personal info related
        GENDER              ( 35, 0, 1, 0 ),
        HEIGHT_CM           ( 36, 0, 250, 0 ),
        WEIGHT_KG           ( 37, 0, 200, 0 ),
        STRIDE_CM           ( 38, 0, 150, 0 ),
        RUNNING_STRIDE_CM   ( 39, 0, 200, 0 ),
        SLEEP_BEGIN_HOUR    ( 40, 0, 23, 0 ),
        SLEEP_BEGIN_MINUTE  ( 41, 0, 59, 0),
        SLEEP_END_HOUR      ( 42, 0, 23, 0 ),
        SLEEP_END_MINUTE    ( 43, 0, 59, 0),
        BT_AUTO_DISCONNECT  ( 46, 0, 1, 0 ),
        BT_AUTO_TIMEOUT     ( 47, 0, 120, 0 ),

        //device data related
        DEVICE_LID          ( 2, 0x01, 0x09, 0 );
        ;

        final private static LazyLogger lazyLog = new LazyLogger( "WaveAgent.MarshalByte",
                WaveRequest.lazyLog );

        private final int index;    //* index: raw index in message byte array.
        private final int min;      //* min: lower (inclusive) bounds for byte value.
        private final int max;      //* max: upper (inclusive) bounds for byte value.
        private final int offset;   //* offset: int offset to apply to byte value.

        private MarshalByte(final int index, final int min, final int max, final int offset) {
            if( ( min < 0 && max > 127 ) || min < -128 || max > 255 || min > max ) {
                WaveRequest.lazyLog.e("Error, illegal range for casting: [", min, ",", max, "]");
            }
            this.index = index;
            this.min = min;
            this.max = max;
            this.offset = offset;
        }

        private void checkRange( int value ) {
            if( value < min || value > max ) {
                lazyLog.e("WaveAgent::MessageByte::", this.name(), " Value exceeds bounds("
                       , min, ",", max, "): ", value);
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
                lazyLog.e("WaveAgent::MessageByte", "Value exceeds bounds("
                       , min, ",", max, "): ", value);
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
    abstract static public class GetDate extends BLERequestWaveOp {

        /** Creates a new GET_TIME request for the wave device in question.
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         */
        public GetDate( final BLEAgent.BLEDevice device, final int timeout ) {
            super( WaveOp.GET_TIME, 0, device, timeout );
        }

        @Override
        protected void onComplete(boolean success, byte[] response) {
            Date ret = null;
            if( success && response != null) {
                final Calendar cal = com.movo.wave.util.UTC.newCal();
                cal.set( Calendar.YEAR, MarshalByte.YEAR.parse(response) );
                cal.set( Calendar.MONTH, MarshalByte.MONTH.parse(response) );
                cal.set( Calendar.DATE, MarshalByte.DATE.parse(response) );
                cal.set( Calendar.HOUR_OF_DAY, MarshalByte.HOUR.parse(response) );
                cal.set( Calendar.MINUTE, MarshalByte.MINUTE.parse(response) );
                cal.set( Calendar.SECOND, MarshalByte.SECOND.parse(response) );
                ret = cal.getTime();
            }
            onComplete(success, ret);
        }

        /** Completion callback for operation
         *
         * @param success boolean indication of state.
         * @param date response date or null
         */
        abstract protected void onComplete( boolean success, Date date );
    }

    /** Set time on device using local clock. Device time will be set in UTC.
     *
     */
    abstract static public class SetDate extends BLERequestWaveOp {

        /** Construct new SET_TIME request for the device
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         */
        public SetDate(final BLEAgent.BLEDevice device, final int timeout ) {
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
            final Calendar cal = com.movo.wave.util.UTC.newCal();
            final Date now = new Date();
            lazyLog.d(  "Setting time as ", com.movo.wave.util.UTC.isoFormat( now ), " (", now, ")" );
            cal.setTime( now );
            MarshalByte.YEAR.put( message, cal.get( Calendar.YEAR ) );
            MarshalByte.MONTH.put( message, cal.get( Calendar.MONTH ) );
            MarshalByte.DATE.put( message, cal.get( Calendar.DATE ) );
            MarshalByte.HOUR.put( message, cal.get( Calendar.HOUR_OF_DAY ) );
            MarshalByte.MINUTE.put( message, cal.get( Calendar.MINUTE ) );
            MarshalByte.SECOND.put( message, cal.get( Calendar.SECOND ) );
            MarshalByte.DAY.put( message, cal.get( Calendar.DAY_OF_WEEK ) );
            return super.dispatch(agent);
        }

        //TODO: parse set date response if we care
    }


    /** Set personal info request.
     *
     */
    static abstract public class SetPersonalInfo extends BLERequestWaveOp {
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
         * @param sleepBegin hour and minute Date object.
         * @param sleepEnd hour and minute Date object.
         */
        public SetPersonalInfo( final BLEAgent.BLEDevice device,
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
    static public class WaveDataPoint implements Comparable<WaveDataPoint> {
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
                    //lazyLog.w( "WaveAgent.WaveDataPoint", "Data point with RESERVED value" );
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
            int tmp = message[ offset ] & 0x3F;
            tmp <<= 8;
            tmp += 0xFF & (int) message[ offset + 1 ];
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
            final WaveDataPoint[] ret = new WaveDataPoint[qty];
            for( int index = 0 ; index < qty;  index += 1 ) {
                final Date date = new Date( start.getTime() - index * 1000 * 60 * 2 );
                date.setSeconds( 0 );
                ret[ index ] = new WaveDataPoint( message, index * 2 + offset, date );
            }

            return ret;
        }

        /** Bulk parser for extracting points from a message.
         *
         * @param message raw message byte array.
         * @param offset index to first byte data point.
         * @param qty number of data points to parse (unchecked).
         * @param year of first data point.
         * @param month of first data point.
         * @param date of first data point.
         * @return array of parsed WaveDataPoint objects.
         */
        public static WaveDataPoint[] parseResponse( final byte[] message,
                                                     final int offset,
                                                     final int qty,
                                                     final int year,
                                                     final int month,
                                                     final int date ) {
            final WaveDataPoint[] ret = new WaveDataPoint[qty];
            final Calendar cal = com.movo.wave.util.UTC.newCal();
            cal.set( Calendar.YEAR, year );
            cal.set( Calendar.MONTH, month );
            cal.set( Calendar.DATE, date );
            cal.set( Calendar.HOUR_OF_DAY, 0 );
            cal.set( Calendar.MINUTE, 0 );
            cal.set( Calendar.SECOND, 0 );
            cal.set( Calendar.MILLISECOND, 0 );

            for( int index = 0 ; index < qty;  index += 1 ) {
                ret[ index ] = new WaveDataPoint( message, index * 2 + offset, cal.getTime() );
                cal.add( Calendar.MINUTE, 30 );
            }

            return ret;
        }

        /** not sure this is correct
         *
         * @return "mode value date" string.
         */
        @Override
        public String toString() {
            return mode.toString() + " " + value + " " + com.movo.wave.util.UTC.isoFormat( date );
        }

        /** Data comparison for sort by date.
         * @param other Data point for comparison
         * @return compareTo integer
         */
        public int compareTo( final WaveDataPoint other ) {
            return this.date.compareTo(other.date);
        }
    }

    /** Request for data by day of the week
     *
     */
    abstract static public class ReadData extends BLERequestWaveOp {

        final private static LazyLogger lazyLog = new LazyLogger( "DataByDate",
                BLERequestWaveOp.lazyLog );

        protected final Calendar cal = UTC.newCal();

        /** Check calendar values and set message request bytes
         *
         */
        private void initMessage() {
            lazyLog.a( cal.get( Calendar.HOUR_OF_DAY ) == 0, "Dropping non-zero HOUR in data request" );
            lazyLog.a( cal.get( Calendar.MINUTE ) == 0, "Dropping non-zero MINUTE in data request" );
            lazyLog.a( cal.get( Calendar.SECOND ) == 0, "Dropping non-zero SECOND in data request" );
            lazyLog.a( cal.get( Calendar.MILLISECOND ) == 0, "Dropping non-zero MILLISECOND in data request" );

            MarshalByte.YEAR.put( message, cal.get( Calendar.YEAR ) );
            MarshalByte.MONTH.put( message, cal.get( Calendar.MONTH ) );
            MarshalByte.DATE.put( message, cal.get( Calendar.DATE ) );
        }

        /** Create a new request at the given Date
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         * @param date time for request (hour, minutes, and seconds should be zero in UTC).
         */
        public ReadData(final BLEAgent.BLEDevice device,
                        final int timeout,
                        final Date date){
            this( device, timeout, date.getTime() );
        }

        /** Create a new request at the given Calendar
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         * @param date time for request (hour, minutes, and seconds should be zero in UTC).
         */
        public ReadData(final BLEAgent.BLEDevice device,
                        final int timeout,
                        final Calendar date){
            this( device, timeout, date.getTimeInMillis() );
        }

        /** Create a new request at the given timestamp
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         * @param timestamp time for request (hour, minutes, and seconds should be zero in UTC).
         */
        public ReadData(final BLEAgent.BLEDevice device,
                        final int timeout,
                        final long timestamp){
            super( WaveOp.READ_DATA_WEEK, 3, device, timeout );
            cal.setTimeInMillis( timestamp );

            initMessage();
        }


        /** Create a request for the given year, month, date
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         * @param year to request.
         * @param month to request.
         * @param date to request.
         */
        public ReadData(final BLEAgent.BLEDevice device,
                        final int timeout,
                        final int year,
                        final int month,
                        final int date){
            super( WaveOp.READ_DATA_WEEK, 3, device, timeout );

            cal.set( Calendar.YEAR, year );
            cal.set( Calendar.MONTH, month );
            cal.set( Calendar.DATE, date );
            cal.set( Calendar.HOUR_OF_DAY, 0 );
            cal.set( Calendar.MINUTE, 0 );
            cal.set( Calendar.SECOND, 0 );
            cal.set( Calendar.MILLISECOND, 0 );

            initMessage();
        }

        @Override
        protected void onComplete(boolean success, byte[] response) {
            WaveDataPoint[] ret = null;

            success &= (response != null);

            do {
                if( ! success ) {
                    lazyLog.d(  "Failed to get data " + UTC.isoFormat( cal ) );
                    break;
                }
                lazyLog.a( MarshalByte.SIZE.parse( response ) == 99,
                        "Data length doesn't match spec: 99!=",
                        MarshalByte.SIZE.parse( response ) );

                final int qty = (MarshalByte.SIZE.parse(response) - 3)/2;

                lazyLog.d(  "Data by date for ", UTC.isoFormat( cal ), " returned ", qty,
                        " data points." );

                success &= ( qty >= 0 );

                if( ! success ) {
                    lazyLog.d("No data points for ", cal);
                    break;
                }

                final int year = MarshalByte.YEAR.parse( response );
                final int month = MarshalByte.MONTH.parse( response );
                final int date = MarshalByte.DATE.parse( response );

                lazyLog.d(  "Response date stamp: ", year, "-", month, "-", date,
                        0, ":00:00" );
                lazyLog.a(year == cal.get( Calendar.YEAR ), "Year mismatch "
                       , year, " ", cal.get( Calendar.YEAR ) );
                lazyLog.a(month == cal.get( Calendar.MONTH ), "Month mismatch "
                       , month, " ", cal.get( Calendar.MONTH ));
                lazyLog.a(date == cal.get( Calendar.DATE ), "Date mismatch "
                       , date, " ", cal.get( Calendar.DATE ));

                ret = WaveDataPoint.parseResponse(response, 5, qty, year, month, date);

            } while( false );

            onComplete( success, ret );
        }

        /** completion callback for data points
         *
         * @param success boolean indication of state.
         * @param data array of WaveDataPoint objects or null;
         */
        protected abstract void onComplete( boolean success, WaveDataPoint[] data );
    }

    /** Request for setting device name. seems broken.
     *
     * FIXME: Check character set encoding.
     */
    abstract static public class SetName extends BLERequestWaveOp {
        static final int bodySize = 15;

        /** Create a SET_NAME request.
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         * @param name device name to set, maximum 15 characters.
         */
        public SetName( final BLEAgent.BLEDevice device,
                                   final int timeout,
                                   final String name ){
            super( WaveOp.SET_NAME, bodySize, device, timeout );
            final char[] chars = name.toCharArray();
            final int copyLimit = Math.min(bodySize, chars.length);
            lazyLog.a(copyLimit == chars.length, "Requested name longer than 15: ", name);

            for( int index = 0; index < copyLimit; index++ ) {
                message[ index + 2 ] = (byte) chars[ index ];
            }
            for( int index = copyLimit; index < bodySize; index++ ) {
                message [ index + 2 ] = (byte) '#';
            }
        }
    }

    /** Request clear device data
     *
     */
    abstract static public class ClearData extends BLERequestWaveOp {

        /** Create new request to clear data on a device
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         */
        public ClearData( final BLEAgent.BLEDevice device, final int timeout ) {
            super( WaveOp.CLEAR_DATA, 0, device, timeout );
        }
    }

    /** Request reset device
     *
     */
    abstract static public class ResetDevice extends BLERequestWaveOp {

        /** Create new request to reset device to factory defaults
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         */
        public ResetDevice( final BLEAgent.BLEDevice device, final int timeout ) {
            super( WaveOp.RESET_TO_DEFAULTS, 0, device, timeout );
        }
    }

    /** Create a read device data requests
     *
     */
    abstract static public class ReadDeviceData extends BLERequestWaveOp {

        /** Enumeration of LID range and hex values.
         *
         */
        static public enum LocalIdentifier {
            MONDAY      (0x01),
            TUESDAY     (0x02),
            WEDNESDAY   (0x03),
            THURSDAY    (0x04),
            FRIDAY      (0x05),
            SATURDAY    (0x06),
            SUNDAY      (0x07),
            TOTAL       (0x09),
            BATTERY     (0x08)
            ;
            public final int value;
            private  LocalIdentifier( final int value ) {
                this.value = value;
            }
        }

        protected final LocalIdentifier lid;

        /** Create a new read device data request
         *
         * @param device communications target.
         * @param timeout relative time after operation begins before request is abandoned.
         * @param lid Local identifier to query.
         */
        public ReadDeviceData( final BLEAgent.BLEDevice device,
                                          final int timeout,
                                          final LocalIdentifier lid) {
            super( WaveOp.READ_DEVICE, 1, device, timeout );
            this.lid = lid;
            MarshalByte.DEVICE_LID.put( message, lid.value );
        }

        @Override
        protected void onComplete(boolean success, byte[] response) {
            int steps = 0;

            if( lid == LocalIdentifier.BATTERY) {

                lazyLog.e( "Parsing of battery data not implemented");
                success = false;
            } else {
                if (success) {
                    steps = (int) response[2] & 0xFF;
                    steps <<= 8;
                    steps += (int) response[3] & 0xFF;
                    steps <<= 8;
                    steps += (int) response[4] & 0xFF;
                }
            }
            onComplete( success, steps );
        }

        /** Callback after parsing.
         *
         * Note: this.lid holds the requested lid.
         *
         * @param success boolean indication of state.
         * @param steps number of steps.
         */
        protected abstract void onComplete( boolean success, int steps );
    }

    /** Create read device serial requests
     *
     */
    abstract public static class ReadSerial extends BLERequestWaveOp {

        /** Create a new request to read a device's serial number.
         *
         * @param device    communications target.
         * @param timeout   relative time after operation begins before request is abandoned.
         */
        public ReadSerial( final BLEAgent.BLEDevice device, final int timeout ) {
            super(WaveOp.READ_SERIAL, 0, device, timeout);
        }

        /** Parse response into hex string.
         *
         * @param success boolean indication of state.
         * @param response raw response byte array or null.
         */
        @Override
        protected void onComplete(boolean success, byte[] response) {
            final String serial;
            if( success ) {
                serial = BLEAgent.bytesToHex( response, 2, MarshalByte.SIZE.parse( response ) );
            } else {
                serial = null;
            }
            onComplete( success, serial );
        }

        /** Callback for serial number.
         *
         * @param success boolean indication of state.
         * @param serial hex string serial number or null.
         */
        abstract protected void onComplete(boolean success, String serial);
    }

    /** Create read device version requests
     *
     */
    abstract public static class ReadVersion extends BLERequestWaveOp {

        /** Create a new request to read a device's version number.
         *
         * @param device    communications target.
         * @param timeout   relative time after operation begins before request is abandoned.
         */
        public ReadVersion( final BLEAgent.BLEDevice device, final int timeout ) {
            super(WaveOp.READ_VERSION, 0, device, timeout);
        }

        /** Parse response into hex string.
         *
         * @param success boolean indication of state.
         * @param response raw response byte array or null.
         */
        @Override
        protected void onComplete(boolean success, byte[] response) {
            final String version;
            if( success ) {
                version = BLEAgent.bytesToHex( response, 2, MarshalByte.SIZE.parse( response ) );
            } else {
                version = null;
            }
            onComplete( success, version );
        }

        /** Callback for version number.
         *
         * @param success boolean indication of state.
         * @param version hex string version number or null.
         */
        abstract protected void onComplete(boolean success, String version);
    }
}
