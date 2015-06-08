package com.movo.wave.comms;

//Created by Alexander Haase on 6/7/2015.

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.provider.ContactsContract;

import com.movo.wave.Database;
import com.movo.wave.util.LazyLogger;
import com.movo.wave.util.UTC;

import java.util.Collection;
import java.util.Date;

/**  Class mapping waves to users and names by SERIAL.
 *
 * Note: Database handles SERIAL + USER as unique!!!! ALL CODE should act accordingly.
 *
 * @author Alexander Haase
 */
public class WaveName {

    final public static LazyLogger lazyLog = new LazyLogger( "WaveName" );

    public final WaveInfo info;
    public final String user;
    public Date when;
    private String name;

    /** Default constructor for fully specifying a wave device's name for a user
     *
     * @param info parent wave info attribute
     * @param user unique user string
     * @param name
     */
    public WaveName( final WaveInfo info, final String user, final String name, final Date when ) {
        this.info = info;
        this.user = user;
        this.name = name;
        this.when = when;
    }

    /** Constructor for no-name association
     *
     * @param info parent wave info attribute
     * @param user unique user string
     */
    public WaveName( final WaveInfo info, final String user ) {
        this( info, user, null, new Date() );
    }

    /** Public getter for name.
     *
     * @return user's device name or serial.
     */
    public String getName() {
        if( name != null ) {
            return name;
        } else {
            return info.serial;
        }
    }

    public final static String[] queryColumns = new String[] {
            Database.WaveUserAssociation.SERIAL,
            Database.WaveUserAssociation.USER,
            Database.WaveUserAssociation.NAME,
            Database.WaveUserAssociation.WHEN
    };

    public final static String whereSerialClause = Database.WaveUserAssociation.SERIAL + " = ? ";

    /** database serializer
     *
     * @param db database handle for storage.
     * @return row index of resultant record.
     */
    public long store( final SQLiteDatabase db ) {
        ContentValues values = new ContentValues();
        values.put( Database.WaveUserAssociation.SERIAL, info.serial );
        values.put( Database.WaveUserAssociation.USER, user );
        values.put( Database.WaveUserAssociation.NAME, name );
        values.put( Database.WaveUserAssociation.WHEN, UTC.isoFormat(when));

        final long ret = db.replace( Database.WaveUserAssociation.WAVE_USER_ASSOCIATION_TABLE_NAME, null, values );

        return ret;
    }

    /** Helper function to extract columns by name
     *
     * @param cursor query cursor object.
     * @param column column name to extract.
     * @return string value of results.
     */
    public static String getColumn( Cursor cursor, String column ) {
        int index = cursor.getColumnIndex( column );
        return cursor.getString( index );
    }

    /** Database oriented constructor to facilitate queries
     *
     * @param info parent info object
     * @param cursor cursor to unpack
     */
    protected WaveName( WaveInfo info, Cursor cursor ) {
        this.info = info;

        lazyLog.a( info.serial.equals(getColumn(cursor, Database.WaveUserAssociation.SERIAL)));
        this.user = getColumn(cursor, Database.WaveUserAssociation.USER);
        this.name = getColumn(cursor, Database.WaveUserAssociation.NAME);
        this.when = null; //! FIXME: Parse modification time!
    }

    public static long byInfo( final SQLiteDatabase db, final WaveInfo info, final Collection<WaveName> out ) {
        final String[] criteria = new String[] { info.serial };
        Cursor result = db.query( Database.WaveUserAssociation.WAVE_USER_ASSOCIATION_TABLE_NAME,
                queryColumns,
                whereSerialClause,
                criteria,
                null,
                null,
                null);

        long ret = 0;

        while( result.moveToNext() ) {
            out.add( new WaveName( info, result ) );
            ret += 1;
        }
        return ret;
    }
}