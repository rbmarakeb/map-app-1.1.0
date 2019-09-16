package com.map.android.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.map.android.DroidPlannerApp;
import com.map.android.R;
import com.map.android.activities.helpers.SuperUI;
import com.map.android.client.Drone;
import com.map.android.fragments.SettingsFragment;
import com.map.android.fragments.control.BaseFlightControlFragment;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.view.SlidingDrawer;

import java.util.List;
/**
 * This abstract activity provides its children access to a navigation drawer
 * interface.
 */
public abstract class DrawerNavigationUI extends SuperUI implements SlidingDrawer.OnDrawerOpenListener, SlidingDrawer.OnDrawerCloseListener, NavigationView.OnNavigationItemSelectedListener {

    /**
     * Activates the navigation drawer when the home button is clicked.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * Navigation drawer used to access the different sections of the app.
     */
    private static DrawerLayout mDrawerLayout;

    private SlidingDrawer actionDrawer;

    /**
     * Container for the activity content.
     */
    private FrameLayout contentLayout;

    /**
     * Clicking on an entry in the open navigation drawer updates this intent.
     * When the navigation drawer closes, the intent is used to navigate to the desired location.
     */
    private Intent mNavigationIntent;

    /**
     * Navigation drawer view
     */
    private NavigationView navigationView;

    private boolean mIsSetting;
    static private boolean is_Previous_Sail_Plan=false;
    protected DroidPlannerApp dpApp;
    private Drone drone;
    protected static final String KEY_UPDOWN = "com.map.android.KEY_UPDOWN";
    protected static final String UPDOWN_VALUE = "com.map.android.UPDOWN_VALUE";
    static Context context;
    List<LatLong> previousWaypointsPositions;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the drawer layout container.
        mDrawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_drawer_navigation_ui, null);
        contentLayout = (FrameLayout) mDrawerLayout.findViewById(R.id.content_layout);
        mIsSetting = false;
        dpApp = (DroidPlannerApp) getApplication();
        drone = dpApp.getDrone();
        context=this;
        previousWaypointsPositions=null;
        if (context.getClass().getSimpleName().equals("EditorActivity")) {
            previousWaypointsPositions = EditorActivity.getCurrentWaypointsPositions();
        }

        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                switch (drawerView.getId()) {
                    case R.id.navigation_drawer:
                        if (mNavigationIntent != null) {
                            if(mIsSetting) {
                                mIsSetting = false;
                                AlertDialog.Builder builder = new AlertDialog.Builder(DrawerNavigationUI.this);
                                builder.setTitle("Password");

// Set up the input
                                final EditText input = new EditText(DrawerNavigationUI.this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                builder.setView(input);

// Set up the buttons
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //String pwd = input.getText().toString();
                                        //if(getResources().getString(R.string.settings_passcode).equals(pwd)) {
                                        dialog.dismiss();
                                        startActivity(mNavigationIntent);
                                        mNavigationIntent = null;
                                        dialog.dismiss();
                                        //} else {
                                        //    Toast.makeText(DrawerNavigationUI.this, "Password incorrect.", Toast.LENGTH_SHORT);
                                        //  }
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();

                                    }
                                });
                                builder.show();
                            } else {
                                startActivity(mNavigationIntent);
                                mNavigationIntent = null;
                            }
                        }
                        break;
                }
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        actionDrawer = (SlidingDrawer) mDrawerLayout.findViewById(R.id.action_drawer_container);
        actionDrawer.setOnDrawerCloseListener(this);
        actionDrawer.setOnDrawerOpenListener(this);
    }

    protected View getActionDrawer() {
        return actionDrawer;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BaseFlightControlFragment.FOLLOW_SETTINGS_UPDATE:
                LocalBroadcastManager.getInstance(getApplicationContext())
                        .sendBroadcast(new Intent(SettingsFragment.ACTION_LOCATION_SETTINGS_UPDATED)
                                .putExtra(SettingsFragment.EXTRA_RESULT_CODE, resultCode));
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    /**
     * Intercepts the call to 'setContentView', and wrap the passed layout
     * within a DrawerLayout object. This way, the children of this class don't
     * have to do anything to benefit from the navigation drawer.
     *
     * @param layoutResID layout resource for the activity view
     */
    @Override
    public void setContentView(int layoutResID) {
        final View contentView = getLayoutInflater().inflate(layoutResID, mDrawerLayout, false);
        contentLayout.addView(contentView);
        setContentView(mDrawerLayout);

        navigationView = (NavigationView) findViewById(R.id.navigation_drawer_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void initToolbar(Toolbar toolbar) {
        super.initToolbar(toolbar);

        toolbar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                final float topMargin = getActionDrawerTopMargin();
                final int fullTopMargin = (int) (topMargin + (bottom - top));

                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) actionDrawer.getLayoutParams();
                if (lp.topMargin != fullTopMargin) {
                    lp.topMargin = fullTopMargin;
                    actionDrawer.requestLayout();
                }

                onToolbarLayoutChange(left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
            }
        });
    }

    /**
     * Manage Navigation drawer menu items
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {

        int id = menuItem.getItemId();

        switch (id) {
            case R.id.navigation_flight_data:

                if(is_Previous_Sail_Plan) {
                    is_Previous_Sail_Plan = false;
                    final boolean isDroneConnected = drone.isConnected();
                    if (isDroneConnected) {
                        if( isWayPointPathUpdated(previousWaypointsPositions) )
                        showConfirmRouteDialog( 0);
                        else {
                            mNavigationIntent = new Intent(this, FlightActivity.class);
                        }
                    }
                    else {
                        mNavigationIntent = new Intent(this, FlightActivity.class);
                    }

                }
                else
                {
                    mNavigationIntent = new Intent(this, FlightActivity.class);
                }
                break;

            case R.id.navigation_editor:
                is_Previous_Sail_Plan=true;
                mNavigationIntent = new Intent(this, EditorActivity.class);

                break;

//            case R.id.navigation_locator:
//                mNavigationIntent = new Intent(this, LocatorActivity.class);
//                break;

            case R.id.navigation_remote_helm:
                if(is_Previous_Sail_Plan) {
                    is_Previous_Sail_Plan = false;
                    final boolean isDroneConnected = drone.isConnected();
                    if (isDroneConnected) {
                        if( isWayPointPathUpdated(previousWaypointsPositions) )
                        showConfirmRouteDialog(1);
                        else {
                            mNavigationIntent = new Intent(this, RemoteHelmActivity.class);
                        }

                    }
                    else {
                        mNavigationIntent = new Intent(this, RemoteHelmActivity.class);
                    }

                }
                else
                {
                    mNavigationIntent = new Intent(this, RemoteHelmActivity.class);
                }
                break;

//            case R.id.navigation_params:
//                mNavigationIntent = new Intent(this, ConfigurationActivity.class)
//                        .putExtra(ConfigurationActivity.EXTRA_CONFIG_SCREEN_ID, id);
//                break;
//
//            case R.id.navigation_checklist:
//                mNavigationIntent = new Intent(this, ConfigurationActivity.class)
//                        .putExtra(ConfigurationActivity.EXTRA_CONFIG_SCREEN_ID, id);
//                break;
//
//            case R.id.navigation_calibration:
//                mNavigationIntent = new Intent(this, ConfigurationActivity.class)
//                        .putExtra(ConfigurationActivity.EXTRA_CONFIG_SCREEN_ID, id);
//                break;

            case R.id.navigation_settings:
                //if you need to show password prompt
                //   mIsSetting = true;
                if(is_Previous_Sail_Plan) {
                    is_Previous_Sail_Plan = false;
                    final boolean isDroneConnected = drone.isConnected();
                    if (isDroneConnected) {
                        if( isWayPointPathUpdated(previousWaypointsPositions) )
                        showConfirmRouteDialog(2);
                        else {
                            mNavigationIntent = new Intent(this, SettingsActivity.class);
                        }
                    }
                    else {
                        mNavigationIntent = new Intent(this, SettingsActivity.class);
                    }

                }
                else
                {
                    mNavigationIntent = new Intent(this, SettingsActivity.class);
                }
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void onToolbarLayoutChange(int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

    }

    protected float getActionDrawerTopMargin() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();
        updateNavigationDrawer();
    }

    private void updateNavigationDrawer() {
        final int navDrawerEntryId = getNavigationDrawerMenuItemId();
        switch (navDrawerEntryId) {
            default:
                navigationView.setCheckedItem(navDrawerEntryId);
                break;
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (mDrawerToggle != null) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            mDrawerToggle.syncState();
        }
    }

    public boolean isActionDrawerOpened() {
        return actionDrawer.isOpened();
    }

    protected int getActionDrawerId() {
        return R.id.action_drawer_content;
    }

    /**
     * Called when the action drawer is opened.
     * Should be override by children as needed.
     */
    @Override
    public void onDrawerOpened() {

    }

    /**
     * Called when the action drawer is closed.
     * Should be override by children as needed.
     */
    @Override
    public void onDrawerClosed() {

    }

    public void openActionDrawer() {
        actionDrawer.animateOpen();
        actionDrawer.lock();
    }

    public void closeActionDrawer() {
        actionDrawer.animateClose();
        actionDrawer.lock();
    }

    protected abstract int getNavigationDrawerMenuItemId();

    private void showConfirmRouteDialog(final int ac)
    {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_route);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));


        Button upload = (Button) dialog.findViewById(R.id.upload_btn);
        Button dont_upload = (Button) dialog.findViewById(R.id.dont_upload_btn);
        Button cancel = (Button) dialog.findViewById(R.id.cancel_btn);
        // if button is clicked, close the custom dialog
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                writeSharedPreference(0,UPDOWN_VALUE,KEY_UPDOWN);
                Intent mNavigationIntent= new Intent(getApplicationContext(), FlightActivity.class);
                if (ac==0)
                    mNavigationIntent = new Intent(getApplicationContext(), FlightActivity.class);
                else if (ac==1)
                    mNavigationIntent = new Intent(getApplicationContext(), RemoteHelmActivity.class);
                else if (ac==2)
                    mNavigationIntent = new Intent(getApplicationContext(), SettingsActivity.class);

                startActivity(mNavigationIntent);
            }
        });
        dont_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                writeSharedPreference(1,UPDOWN_VALUE,KEY_UPDOWN);
                Intent mNavigationIntent= new Intent(getApplicationContext(), FlightActivity.class);
                if (ac==0)
                    mNavigationIntent = new Intent(getApplicationContext(), FlightActivity.class);
                else if (ac==1)
                    mNavigationIntent = new Intent(getApplicationContext(), RemoteHelmActivity.class);
                else if (ac==2)
                    mNavigationIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(mNavigationIntent);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                is_Previous_Sail_Plan=true;
                navigationView.getMenu().getItem(1).setChecked(true);
            }
        });
        dialog.show();

    }


    public int  readSharedPreference(String key,String s )
    {
        SharedPreferences sharedPref =getBaseContext().getSharedPreferences(key,MODE_PRIVATE);
        //-1 is default_value if no vaule
        int  savedSetting = sharedPref.getInt(s,-1);

        return savedSetting;
    }
    public  void  writeSharedPreference(int savedSetting,String key,String s )
    {
        SharedPreferences sharedPref =getBaseContext().getSharedPreferences(key,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(s, savedSetting);
        editor.commit();
    }

    public static void lockDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }
    // To unlock the drawer:

    public static void unlockDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }
    public boolean isWayPointPathUpdated(List<LatLong> previousWaypointsPositions){
        try {

            if (context.getClass().getSimpleName().equals("EditorActivity"))
            {
                List<LatLong>  currentWaypointsPositions=EditorActivity.getCurrentWaypointsPositions();
                if (currentWaypointsPositions == null && previousWaypointsPositions == null )
                    return false;

               else if (currentWaypointsPositions == null )
                    return false;

                else if (currentWaypointsPositions.size() == 0 && previousWaypointsPositions == null )
                    return false;

                else if (currentWaypointsPositions.size() != 0 && previousWaypointsPositions == null )
                    return true;
                else
                    {
                        int i11=currentWaypointsPositions.size();
                        int i22=previousWaypointsPositions.size();

                    if( i11 != i22 )
                        return true;
                    //same size
                    else
                    {
                        Boolean updated=false;
                        for (int i =0 ; i < previousWaypointsPositions.size() ; i++)
                            //compare first item of MarkerInfos of each MissionItemProxy's Position
                        {
                            LatLong latLongprevious =previousWaypointsPositions.get(i);
                            LatLong latLongcurrent=currentWaypointsPositions.get(i);
//                            int c1=Double.compare(latLongprevious.getLatitude(), latLongcurrent.getLatitude());
//                            int c2=Double.compare(latLongprevious.getLongitude(), latLongcurrent.getLongitude());
                             if(Double.compare(latLongprevious.getLatitude(), latLongcurrent.getLatitude()) != 0
                                     || Double.compare(latLongprevious.getLongitude(), latLongcurrent.getLongitude()) != 0  )
                                {
                                    updated=true;
                                    break;
                                }
                        }
                        return updated;

                            }
                    }
            }
        }
        catch (Exception e)
        {
            return true;
        }
        return true;
    }
}
