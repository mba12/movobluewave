package com.movo.wave;

import android.content.Context;
import android.content.Intent;
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

/**
 * Created by alex on 4/28/2015.
 */
public abstract class MenuActivity extends ActionBarActivity {
    final static LazyLogger lazyLog = new LazyLogger( "MenuActivity");

    public static enum Option {
        LifeCycle   ("My Life Calendar", Home.class ),
        MyProfile   ("My Profile", com.movo.wave.MyProfile.class ),
        UploadData  ("Upload Data", Home.class){
            @Override
            public Intent select(Context context) {
                UserData mUD = UserData.getUserData(context);
                Intent intent = new Intent( context,Home.class);
                intent.putExtra("Upload",true);
                return intent;
            }
        },
        SyncData  ("Sync Wave", SyncDataActivity.class),
        User        ("Users", UserActivity.class ),
        FAQ         ("FAQ", null),
        Contact     ("Contact", null),
        Logout      ("Logout", null) {
            @Override
            public Intent select(Context context) {
                UserData mUD = UserData.getUserData(context);
                return new Intent( context, mUD.logoutCurrentUser() ? Home.class : FirstLaunch.class );
            }
        },
        DiscoverWave    ( "My Waves", DiscoverWaveActivity.class),
        ;

        final public String text;
        final public Class<?> activity;

        private Option( String text, Class<?> activity ) {
            this.text = text;
            this.activity = activity;
        }

        public Intent select( Context context ) {
            Intent ret = null;
            if( activity != null ) {
                ret = new Intent( context, activity );
            } else {
                lazyLog.e( "No activity class for menu option ", name() );
            }
            return ret;
        }

        public static final String[] names;

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

            if( position >= Option.values().length ) {
                lazyLog.e( "Menu ordinal out of range: " + position );
            } else {
                final Intent intent = Option.values()[position].select(c);
                if (intent != null) {
                    startActivity(intent);
                    finish();
                }
            }
        }
    }

    protected void initMenu( int layout_id ) {
        c = getApplicationContext();
        setContentView(layout_id);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
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
}
