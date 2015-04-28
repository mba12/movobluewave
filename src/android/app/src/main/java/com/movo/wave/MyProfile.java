package com.movo.wave;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by P on 4/27/2015.
 */
public class MyProfile extends ActionBarActivity {
    Context c;
    String TAG = "Movo MyProfile";

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private static ProgressBar syncProgressBar;
    private static TextView syncText;
    private ActionBarDrawerToggle mDrawerToggle;
    String[] menuOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);
        c = this.getApplicationContext();


        //**********************Set Up slider menu******************//
        menuOptions = new String[7];
        menuOptions[0] = "My Life Calendar";
        menuOptions[1] = "My Profile";
        menuOptions[2] = "Upload Data";
        menuOptions[3] = "Users";
        menuOptions[4] = "FAQ";
        menuOptions[5] = "Contact Us";
        menuOptions[6] = "Logout";

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList.setBackgroundResource(R.drawable.splash);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(MyProfile.this,
                R.layout.drawer_list_item, menuOptions));
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
        //*********************************************************//

    }


    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {


        mDrawerList.setItemChecked(position, false);
        mDrawerLayout.closeDrawer(mDrawerList);
        switch (position)
        {
            case 0:
//                create();
                Log.d(TAG, "My Life Calendar Pressed");
               myLife();
               break;
            case 1:
                Log.d(TAG, "My Profile Pressed");
                myLife();
                break;
            case 2:
                Log.d(TAG, "Upload pressed");
//                UserData myData = UserData.getUserData(c);
//                Log.d(TAG, "Cur user data: "+myData.getCurUID());

//                upload();

                break;
            case 3:
                Log.d(TAG, "Users pressed");
                users();

                break;
            case 4:
                Log.d(TAG, "FAQ pressed");
                logout();

                break;

            case 5:
                Log.d(TAG, "Contact pressed");
//                match();

                break;

            case 6:
                Log.d(TAG, "Logout pressed");
                logout();


                break;
        }
    }
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
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
//

    public void myLife(){
        Intent intent = new Intent(getApplicationContext(),
                Home.class);
        startActivity(intent);

    }
    public void myProfile(){
        Intent intent = new Intent(getApplicationContext(),
                MyProfile.class);
        startActivity(intent);

    }

    public void login(){
        Intent intent = new Intent(getApplicationContext(),
                FirstLogin.class);
        startActivity(intent);
    }
    public void users(){
        Intent intent = new Intent(getApplicationContext(),
                UserActivity.class);
        startActivity(intent);
    }
    public void logout(){
        UserData mUD = UserData.getUserData(c);
        boolean status = mUD.logoutCurrentUser();
//        if(!status){
            Intent intent = new Intent(getApplicationContext(),
                    Home.class);
            startActivity(intent);
//        }else{
//            setContentView(R.layout.activity_home);
//        }

    }


}
