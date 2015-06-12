package com.movo.wave.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.movo.wave.Database;
import com.movo.wave.DatabaseHelper;

/**
 * Created by Alex Haase on 6/12/2015.
 */
public class DatabaseHandle {

    public static final LazyLogger lazyLog = new LazyLogger("DatabaseHandle");

    private int refCount = 0;
    public SQLiteDatabase db;

    public synchronized boolean acquire( ) {
        final boolean ret = db != null;

        if( ret ) {
            refCount += 1;
        } else {
            lazyLog.e( "REF on stale database handle!!!");
        }
        return ret;
    }

    public synchronized boolean release() {
        final boolean ret = db != null;
        if( ret && --refCount == 0 ) {
            db.close();
            db = null;
        } else if( ! ret ) {
            lazyLog.e( "UNREF on stale database handle!!!");
        }
        return ret;
    }

    public DatabaseHandle( Context c ) {
        DatabaseHelper dbHelper = new DatabaseHelper(c);
        db = dbHelper.getWritableDatabase();
    }
}
