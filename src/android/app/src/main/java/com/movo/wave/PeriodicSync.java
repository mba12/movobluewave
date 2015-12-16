package com.movo.wave;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.widget.Toast;

import com.movo.wave.util.FirebaseSync;
import com.movo.wave.util.LazyLogger;

import java.util.UUID;

/**
 * Created by elan on 12/15/15.
 */
public class PeriodicSync extends BroadcastReceiver
{

    public static final String TAG = "PeriodicSync";

    public static final LazyLogger lazyLog = new LazyLogger(TAG,true );

    public void onReceive(Context context, Intent intent)
    {
//        Toast.makeText(context, "Periodic Sync Upload! !!!!!!!!!!", Toast.LENGTH_LONG).show(); // For example

        // if we have network connection
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if(isConnected){

            UserData userData = UserData.getUserData(context);
            if(userData.getCurrentUsername() != null) {
                lazyLog.d("had username: "+userData.getCurrentUsername());
                FirebaseSync.insertStepsIntoFirebase(context, userData.getCurrentUser());
            }else{
                lazyLog.d("no username");

            }
        }else {
            lazyLog.d("no connection");
        }
    }




    public static void SetAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, PeriodicSync.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), 1000 * 10 * 1, pi); // Millisec * Second * Minute
    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, PeriodicSync.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
