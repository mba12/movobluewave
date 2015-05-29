package com.movo.wave;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by P on 4/14/2015.
 */
public final class Database {


    public Database(){}

    //table contents
    public static abstract class StepEntry implements BaseColumns {
        public static final String STEPS_TABLE_NAME = "steps";
        public static final String SYNC_ID = "syncid";
        public static final String START = "starttime";
        public static final String END = "endtime";
        public static final String USER = "user";
        public static final String STEPS = "count";
        public static final String IS_PUSHED = "ispushed";
        public static final String DEVICEID = "deviceid";
        public static final String WORKOUT_TYPE = "workouttype";
        public static final String GUID = "guid";

    }


    public static abstract class SyncEntry implements BaseColumns {
        public static final String SYNC_TABLE_NAME = "sync";
        public static final String ID = "syncid";
        public static final String SYNC_START = "starttime";
        public static final String SYNC_END = "endtime";
        public static final String USER = "user";
        public static final String STATUS = "status";
        public static final String GUID = "guid";

    }

    public static abstract class KnownWaves implements BaseColumns {
        public static final String WAVE_TABLE_NAME = "waves";
        public static final String MAC = "mac";
        public static final String QUERIED = "queried";
        public static final String SERIAL = "serial";
        public static final String USER = "user";
    }

    public static abstract class PhotoStore implements BaseColumns {
        public static final String PHOTO_TABLE_NAME = "photos";
        public static final String DATE = "date";
        public static final String USER = "user";
        public static final String PHOTOBLOB = "blob";
        public static final String MD5 = "md5";

    }

}
