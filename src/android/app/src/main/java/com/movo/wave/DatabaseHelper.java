package com.movo.wave;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by P on 4/15/2015.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    //**********************************DBOPS**************************************//
    public String TAG = "DBHelper";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES_STEPS =
            "CREATE TABLE " + Database.StepEntry.STEPS_TABLE_NAME + " (" +
                    Database.StepEntry._ID + " INTEGER PRIMARY KEY," +
                    Database.StepEntry.SYNC_ID + TEXT_TYPE + COMMA_SEP +
                    Database.StepEntry.START + TEXT_TYPE + COMMA_SEP +
                    Database.StepEntry.END + TEXT_TYPE + COMMA_SEP +
                    Database.StepEntry.STEPS + TEXT_TYPE + COMMA_SEP +
                    Database.StepEntry.IS_PUSHED + TEXT_TYPE +
                    " )";
    private static final String SQL_CREATE_ENTRIES_SYNC =
            "CREATE TABLE " + Database.SyncEntry.SYNC_TABLE_NAME + " (" +
                    Database.SyncEntry._ID + " INTEGER PRIMARY KEY," +
                    Database.SyncEntry.ID + TEXT_TYPE + COMMA_SEP +
                    Database.SyncEntry.SYNC_START + TEXT_TYPE + COMMA_SEP +
                    Database.SyncEntry.SYNC_END + TEXT_TYPE + COMMA_SEP +
                    Database.SyncEntry.USER + TEXT_TYPE + COMMA_SEP +
                    Database.SyncEntry.STATUS + TEXT_TYPE + COMMA_SEP +
                    Database.SyncEntry.GUID + TEXT_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Database.StepEntry.STEPS_TABLE_NAME;


    //**********************************DBOPS**************************************//

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Steps.db";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES_STEPS);
        Log.d(TAG, "Creating steps table");
        db.execSQL(SQL_CREATE_ENTRIES_SYNC);
        Log.d(TAG, "Creating sync table");
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

