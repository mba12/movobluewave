package com.movo.wave.comms;

/**
 * Created by alex on 4/29/2015.
 */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.movo.wave.Database;
import com.movo.wave.UserData;
import com.movo.wave.util.LazyLogger;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/** Class to describe wave devices to the app (long-term storage)
 *
 */
public class WaveInfo {

    static final LazyLogger lazyLog = new LazyLogger( "WaveInfo");

    public final String mac;
    public Date queried = null;
    public String serial = null;
    private Set<WaveName> names = new HashSet<>();

    /** Create a new unqueried device.
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
        values.put(Database.KnownWaves.SERIAL, serial);

        final long ret = db.replace( Database.KnownWaves.WAVE_TABLE_NAME, null, values );

        for( WaveName name : names ) {
            name.store( db );
        }

        return ret;
    }

    public final static String[] queryColumns = new String[] {
            Database.KnownWaves.MAC,
            Database.KnownWaves.QUERIED,
            Database.KnownWaves.SERIAL
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

        if( serial != null ) {
            WaveName.byInfo(db, this, names);
        }
    }

    private void readCursor( Cursor cursor ) {
        queried = cursor.isNull( 1 ) ? null : new Date( cursor.getLong( 1 ) );
        serial = cursor.getString( 2 );
    }

    /** Create a WaveInfo instance from a query row.
     *
     * @param cursor query result cursor pointing at current row.
     */
    public WaveInfo( Cursor cursor ) {
        this( cursor.getString( 0 ) );
        readCursor(cursor);
    }

    public boolean complete() {
        return queried != null;
    }

    /** Lookup a user name for the device, or null if none set.
     * @param user for lookup
     * @return user's name for device.
    public String getName( final String user ) {
        for( WaveName waveName : names ) {
            if( waveName.user.equals(user) ) {
                return waveName.getName();
            }
        }
        return null;
    }

    /** Set user name for this device.
     *
     * @param user for lookup
     * @param name user's name for device.
     */
    public void setName( final String user, final String name ) {
        for( WaveName waveName : names ) {
            if( waveName.user.equals(user) ) {
                waveName.setName( name );
                return;
            }
        }
        names.add( new WaveName(this, user, name));
    }
}