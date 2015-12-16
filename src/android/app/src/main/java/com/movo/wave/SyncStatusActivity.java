package com.movo.wave;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.movo.wave.R;
import com.movo.wave.util.DatabaseHandle;
import com.movo.wave.util.FirebaseSync;
import com.movo.wave.util.LazyLogger;

public class SyncStatusActivity extends MenuActivity {

    final static LazyLogger lazyLog = new LazyLogger( "SyncStatusActivity", true );

    DatabaseHandle dbHandle;

    TextView userStepsText;
    TextView otherStepsText;
    Button syncButton;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMenu(R.layout.activity_sync_status);
        dbHandle = new DatabaseHandle(c);
        dbHandle.acquire();

        // Construct views
        userStepsText = (TextView) findViewById( R.id.stepSyncStatus );
        otherStepsText = (TextView) findViewById( R.id.stepSyncOtherStatus );
        syncButton = (Button) findViewById( R.id.stepSyncButton );

        // Add listener
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // disable things to make it look like we're doing work!
                syncButton.setEnabled(false);
                userStepsText.setText(R.string.step_sync_pending);
                otherStepsText.setText(R.string.step_sync_pending);

                final String uid = UserData.getUserData(c).getCurUID();

                // TODO: Push to background thread.
                FirebaseSync.insertStepsIntoFirebase( c, uid );
                updateSteps( uid);
                // re-enable button
                syncButton.setEnabled( true );
            }
        });

        // Set initial value
        updateSteps(UserData.getUserData(c).getCurUID());
    }

    @Override
    protected void onDestroy() {
        dbHandle.release();
        super.onDestroy();
    }

    final static String otherUserStepsQuery = "SELECT SUM(" + Database.StepEntry.STEPS  +") FROM " +
            Database.StepEntry.STEPS_TABLE_NAME + " WHERE " + Database.StepEntry.USER + "!=? AND " +
            Database.StepEntry.IS_PUSHED + "=0;";

    final static String currentUserStepQuery = "SELECT SUM(" + Database.StepEntry.STEPS  +") FROM " +
        Database.StepEntry.STEPS_TABLE_NAME + " WHERE " + Database.StepEntry.USER + "=? AND " +
        Database.StepEntry.IS_PUSHED + "=0;";

    void displayQuery( final String query, final String[] args, final TextView out ) {
        Cursor cursor = dbHandle.db.rawQuery(query, args );
        if( cursor.moveToFirst() ) {
            String steps = cursor.getString(0);
            if( steps == null ) {
                steps = "0";
            }
            out.setText(steps);
        } else {
            out.setText(R.string.step_sync_pending);
        }
    }

    void updateSteps( final String user) {
        final String[] args = new String[] { user };
        displayQuery( currentUserStepQuery, args, userStepsText );
        displayQuery( otherUserStepsQuery, args, otherStepsText );
    }

}
