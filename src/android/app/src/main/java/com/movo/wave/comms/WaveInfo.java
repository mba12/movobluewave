package com.movo.wave.comms;

/**
 * Created by alex on 4/29/2015.
 */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.movo.wave.Database;
import com.movo.wave.util.LazyLogger;

import java.util.Collection;
import java.util.Date;

/** Class to describe wave devices to the app (long-term storage)
 *
 */
public class WaveInfo {

    static final LazyLogger lazyLog = new LazyLogger( "WaveInfo");

    public final String mac;
    public Date queried = null;
    public String user = null;
    public String serial = null;
    //public Date lastSeen = null;

    /** Create a new unquired device.
     *
     * @param mac address of device.
     */
    WaveInfo( String mac ) {
        lazyLog.a( mac != null, "Error, cannot have a WaveInfo instance with null mac");
        this.mac = mac;
    }

    public long store( SQLiteDatabase db ) {
        ContentValues values = new ContentValues();
        values.put( Database.KnownWaves.MAC, mac );
        values.put(Database.KnownWaves.QUERIED, queried.getTime());
        values.put( Database.KnownWaves.SERIAL, serial );
        values.put( Database.KnownWaves.USER, user );

        return db.replace( Database.KnownWaves.WAVE_TABLE_NAME, null, values );
    }

    public final static String[] queryColumns = new String[] {
            Database.KnownWaves.MAC,
            Database.KnownWaves.QUERIED,
            Database.KnownWaves.SERIAL,
            Database.KnownWaves.USER
    };

    public final static String whereMACClause = Database.KnownWaves.MAC + " = '";
    public final static String whereUserClause = Database.KnownWaves.USER + " = '";

    /** Get-or-create method for database access
     *
     * Note: does not produce globally unique instance.
     *
     * @param db database helper.
     * @param mac address of device.
     */
    public WaveInfo( SQLiteDatabase db, String mac ) {
        this( mac );
        Cursor cursor = db.query( Database.KnownWaves.WAVE_TABLE_NAME,
                queryColumns,
                whereMACClause + mac + "'",
                null,
                null,
                null,
                null);
        if( cursor.moveToNext() ) {
            readCursor(cursor);
        }
    }

    private void readCursor( Cursor cursor ) {
        queried = cursor.isNull( 1 ) ? null : new Date( cursor.getLong( 1 ) );
        serial = cursor.getString( 2 );
        user = cursor.getString( 3 );
    }

    /** Create a WaveInfo instance from a query row.
     *
     * @param cursor query result cursor pointing at current row.
     */
    public WaveInfo( Cursor cursor ) {
        this( cursor.getString( 0 ) );
        readCursor(cursor);
    }

    /** Insert models for all WaveInfo instances matching the current user.
     *
     * @param db database helper.
     * @param destination output collection
     * @param user for which to query waves
     * @return number of WaveInfo instances inserted.
     */
    static long byUser( SQLiteDatabase db, Collection<WaveInfo> destination, String user ) {
        long ret = 0;
        Cursor cursor = db.query( Database.KnownWaves.WAVE_TABLE_NAME,
                queryColumns,
                whereUserClause + user + "'",
                null,
                null,
                null,
                null);

        while( cursor.moveToNext() ) {
            destination.add( new WaveInfo( cursor ) );
            ret += 1;
        }
        return ret;
    }

    public boolean complete() {
        return queried != null;
    }

    public String toString() {
        return this.serial;
    }
}