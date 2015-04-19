package com.movo.wave;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by EnderSSD on 4/19/2015.
 */
public class FirebaseCalls {
    private static com.firebase.client.Firebase mRef;
    private static Context mC;

    public FirebaseCalls(Context c){

    }

    public static void onCreate(Context c) {
      mRef = UserData.getUserData(c).getCurrentUserRef();
        mC = c;
    }


    public void uploadSync(String guid){
        DatabaseHelper mDbHelper = new DatabaseHelper(mC);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        Cursor cur = db.query(
                Database.SyncEntry.SYNC_TABLE_NAME,  // The table to query
                new String[] { Database.SyncEntry.GUID, Database.SyncEntry.SYNC_START },                               // The columns to return
                Database.SyncEntry.GUID,                                // The columns for the WHERE clause
                new String[] { guid },                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        cur.moveToFirst();
        long itemId = cur.getLong(
                cur.getColumnIndexOrThrow(Database.SyncEntry.GUID)

        );
        Log.d("TAG", "Found sync id "+guid+": "+cur.getColumnIndexOrThrow(Database.SyncEntry.GUID));
    }

}
