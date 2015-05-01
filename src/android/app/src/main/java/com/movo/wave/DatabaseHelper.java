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
    private static final String INTEGER_TYPE = " INTEGER";
//    private static final String STEP_TYPE = " INTEGER";
    private static final String BLOB_TYPE = " BLOB";

    private static final String COMMA_SEP = ",";

    //Create table with unique step key being the Date of the step count taken
    private static final String SQL_CREATE_ENTRIES_STEPS =
            "CREATE TABLE " + Database.StepEntry.STEPS_TABLE_NAME + " (" +
                    Database.StepEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    Database.StepEntry.SYNC_ID + BLOB_TYPE + COMMA_SEP +
                    Database.StepEntry.START + INTEGER_TYPE + COMMA_SEP +
                    Database.StepEntry.END + INTEGER_TYPE + COMMA_SEP +
                    Database.StepEntry.STEPS + INTEGER_TYPE + COMMA_SEP +
                    Database.StepEntry.USER + TEXT_TYPE + COMMA_SEP +
                    Database.StepEntry.IS_PUSHED + INTEGER_TYPE + COMMA_SEP +
                    Database.StepEntry.DEVICEID + BLOB_TYPE + COMMA_SEP +
                    Database.StepEntry.WORKOUT_TYPE + BLOB_TYPE + COMMA_SEP +
                    Database.StepEntry.GUID + BLOB_TYPE +COMMA_SEP +
                    " CONSTRAINT uniqueTime UNIQUE ( "+Database.StepEntry.START+","+Database.StepEntry.DEVICEID +" ) ON CONFLICT REPLACE" +
                    " )";
    //Create table with unique key being the sync GUID
    private static final String SQL_CREATE_ENTRIES_SYNC =
            "CREATE TABLE " + Database.SyncEntry.SYNC_TABLE_NAME + " (" +
                    Database.SyncEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    //Database.SyncEntry.ID + TEXT_TYPE + COMMA_SEP +
                    Database.SyncEntry.SYNC_START + INTEGER_TYPE + COMMA_SEP +
                    Database.SyncEntry.SYNC_END + INTEGER_TYPE + COMMA_SEP +
                    Database.SyncEntry.USER + TEXT_TYPE + COMMA_SEP +
                    Database.SyncEntry.STATUS + INTEGER_TYPE + COMMA_SEP +
                    Database.SyncEntry.GUID + BLOB_TYPE +COMMA_SEP +
                    Database.StepEntry.DEVICEID + BLOB_TYPE + COMMA_SEP +
                    " UNIQUE ( "+Database.SyncEntry.GUID+" ) ON CONFLICT REPLACE" +
                    " )";

    private static final String SQL_CREATE_KNOWN_WAVES =
            "CREATE TABLE " + Database.KnownWaves.WAVE_TABLE_NAME + " (" +
                    Database.KnownWaves.MAC + TEXT_TYPE + " PRIMARY KEY NOT NULL ON CONFLICT REPLACE " + COMMA_SEP +
                    Database.KnownWaves.QUERIED + INTEGER_TYPE  + COMMA_SEP +
                    Database.KnownWaves.SERIAL + TEXT_TYPE + COMMA_SEP +
                    Database.KnownWaves.USER + TEXT_TYPE + ")";

    private static final String SQL_CREATE_PHOTOS_STORAGE =
            "CREATE TABLE " + Database.PhotoStore.PHOTO_TABLE_NAME + " (" +
                    Database.PhotoStore._ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    Database.PhotoStore.DATE + INTEGER_TYPE  + COMMA_SEP +
                    Database.PhotoStore.USER + TEXT_TYPE + COMMA_SEP +
                    Database.PhotoStore.PHOTOBLOB + BLOB_TYPE + COMMA_SEP +
                    " CONSTRAINT uniqueTime UNIQUE ( "+Database.PhotoStore.DATE+","+Database.PhotoStore.USER +" ) ON CONFLICT REPLACE" +
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

        Log.d(TAG, "Creating steps table: " +SQL_CREATE_ENTRIES_STEPS);
        db.execSQL(SQL_CREATE_ENTRIES_STEPS);

        Log.d(TAG, "Creating sync table: " + SQL_CREATE_ENTRIES_SYNC);
        db.execSQL(SQL_CREATE_ENTRIES_SYNC);

        Log.d(TAG, "Creating wave table: "+ SQL_CREATE_KNOWN_WAVES);
        db.execSQL(SQL_CREATE_KNOWN_WAVES);

        Log.d(TAG, "Creating wave table: "+ SQL_CREATE_PHOTOS_STORAGE);
        db.execSQL(SQL_CREATE_PHOTOS_STORAGE);

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

