package com.movo.wave;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.movo.wave.util.LazyLogger;

import java.util.LinkedList;
import java.util.List;

//Created by Alexander Haase on 4/28/2015.


/** Super class for all activities with main program menu. centralizes menu logic.
 *
 * use initMenu( bundle ) in the subclass onCreate() to setup menu handling.
 */
public abstract class MenuActivity extends ActionBarActivity {
    final static LazyLogger lazyLog = new LazyLogger( "MenuActivity");

    /** Menu option enumeration for all activities. Add new menu items here, and subclass select()
     * as needed.
     */
    public static enum Option {
        LifeCycle   ("My Life Calendar", Home.class ),
        MyProfile   ("My Profile", com.movo.wave.MyProfile.class ),
        DiscoverWave( "Upload Data", WaveScanActivity.class),

        User        ("Users", UserActivity.class ),
        FAQ         ("FAQ", null) {
            @Override
            public Intent select(Context context) {
                final Intent uriIntent = new Intent( Intent.ACTION_VIEW,
                        Uri.parse("http://www.getmovo.com/appfaq"));
                return uriIntent;
            }
        },
        Contact     ("Contact", null)  {
            @Override
            public Intent select(Context context) {
                final Intent emailIntent = new Intent( Intent.ACTION_SEND );
                emailIntent.setType("plain/text");
                emailIntent.putExtra( Intent.EXTRA_EMAIL,
                        context.getResources().getStringArray(R.array.contact_email_recipients) );
                emailIntent.putExtra( Intent.EXTRA_SUBJECT,
                        context.getString(R.string.contact_email_subject) +
                                UserData.getUserData(context).getCurrentUsername() );
                return emailIntent;
            }
        },
        Logout      ("Logout", null) {
            @Override
            public Intent select(Context context) {
//                UserData mUD = UserData.getUserData(context);
                return new Intent( context,  UserData.getUserData(context).logoutCurrentUser() ? Home.class : FirstLaunch.class );
            }
        },
        ;

        final public String text;
        final public Class<?> activity;

        private Option( String text, Class<?> activity ) {
            this.text = text;
            this.activity = activity;
        }

        /** Called on menu select, returns an intent for the current activity to start or
         * null for no action.
         *
         * @param context of caller activity
         * @return Intent to start or null for no change.
         */
        public Intent select( Context context ) {
            Intent ret = null;
            if( activity != null ) {
                ret = new Intent( context, activity );
            } else {
                lazyLog.e( "No activity class for menu option ", name() );
            }
            return ret;
        }

        /** Array of human names for menu options. should probably wrap this into initMenu via
         * R.array.<some name>
         */
        public static final String[] names;

        /*
        Build static array of names, not needed when replaced with localized version above.
         */
        static {
            names = new String[ Option.values().length ];
            for( Option option : Option.values() ) {
                names[ option.ordinal() ] = option.text;
            }
        }
    }

    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected Context c;

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            mDrawerList.setItemChecked(position, false);
            mDrawerLayout.closeDrawer(mDrawerList);

            final Option option = Option.values()[position];

            if( option.activity != MenuActivity.this.getClass()) {
                final Intent intent = option.select(c);
                if (intent != null) {
                    startActivity(intent);
                }
            } else {
                lazyLog.i( "Already in menu option: ", option );
            }
        }
    }

    /** Call form onCreate in subclass to setup menu. assumes the layout contains the relevant
     * content. Should set layout before calling.
     *
     * @param layout_id
     */
    protected void initMenu( int layout_id ) {
        c = getApplicationContext();
        setContentView(layout_id);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout); //:TODO <---------------this relies on drawerlayout to be the same name in each class.
        mDrawerList.setBackgroundResource(R.drawable.splash);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, Option.names ));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        Toolbar mToolbar = (Toolbar)findViewById(R.id.toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,  mDrawerLayout, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_home, menu);

        return true;
    }

    /** Make intent to take photo from any app or camera
     *
     * @return intent to pass to startActivityForResult()
     */
    public static Intent photoPickerIntent() {
        //http://stackoverflow.com/questions/2708128/single-intent-to-let-user-take-picture-or-pick-image-from-gallery-in-android
        Intent takePhotoIntent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(photoPickerIntent,"Select Photo With");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                new Intent[]{takePhotoIntent});

        return chooserIntent;
    }

    protected boolean bleEnabled() {
        /* check BLE state */
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    protected final int REQUEST_ENABLE_BT = 1;

    protected void requestBLEEnabled() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }


    /** Helpers for maintaining delegates
     *
     */
    protected List<UserData.UpdateDelegate> delegates = new LinkedList<>();

    /** Disable all tracked delegate
     *
     */
    protected void invalidateDelegates() {
        for(UserData.UpdateDelegate delegate : delegates)
            delegate.invalidate();
        delegates.clear();
    }

    /** Track delegate so we can invalidate them in a batch
     *
     * @param delegate to track
     * @return original delegate
     */
    protected UserData.UpdateDelegate trackDelegate( final UserData.UpdateDelegate delegate ) {
        delegates.add( delegate );
        return delegate;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        invalidateDelegates();
    }
}
