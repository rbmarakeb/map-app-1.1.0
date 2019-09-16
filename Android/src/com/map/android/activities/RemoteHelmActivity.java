package com.map.android.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;

import com.map.android.R;
import com.map.android.client.Drone;
import com.map.android.client.apis.MissionApi;
import com.map.android.dialogs.DialogMaterialFragment;
import com.map.android.fragments.FlightDataFragment;
import com.map.android.fragments.RemoteHelmDataFragment;
import com.map.android.fragments.WidgetsListFragment;
import com.map.android.fragments.actionbar.ActionBarTelemFragment;
import com.map.android.proxy.mission.MissionProxy;
import com.map.android.utils.Utils;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class RemoteHelmActivity extends DrawerNavigationUI implements SlidingUpPanelLayout.PanelSlideListener {

    private static final String EXTRA_IS_ACTION_DRAWER_OPENED = "extra_is_action_drawer_opened";
    private static final boolean DEFAULT_IS_ACTION_DRAWER_OPENED = true;
  static  private RemoteHelmDataFragment flightData;

    @Override
    public void onDrawerClosed() {
        super.onDrawerClosed();

        if (flightData != null)
            flightData.onDrawerClosed();
    }

    @Override
    public void onDrawerOpened() {
        super.onDrawerOpened();

        if (flightData != null)
            flightData.onDrawerOpened();
    }
    //this function used to access to RemoteHelmDataFragment Instance
    public static RemoteHelmDataFragment getInstance(){
        return flightData;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_helm);

        final FragmentManager fm = getSupportFragmentManager();

        //Add the flight data fragment
        flightData = (RemoteHelmDataFragment) fm.findFragmentById(R.id.flight_data_container);
        if(flightData == null){
            Bundle args = new Bundle();
            args.putBoolean(RemoteHelmDataFragment.EXTRA_SHOW_ACTION_DRAWER_TOGGLE, true);

            flightData = new RemoteHelmDataFragment();
            flightData.setArguments(args);
            fm.beginTransaction().add(R.id.flight_data_container, flightData).commit();
        }

        // Add the telemetry fragment
        final int actionDrawerId = getActionDrawerId();
        WidgetsListFragment widgetsListFragment = (WidgetsListFragment) fm.findFragmentById(actionDrawerId);
        if (widgetsListFragment == null) {
            widgetsListFragment = new WidgetsListFragment();
            fm.beginTransaction()
                    .add(actionDrawerId, widgetsListFragment)
                    .commit();
        }

        boolean isActionDrawerOpened = DEFAULT_IS_ACTION_DRAWER_OPENED;
        if (savedInstanceState != null) {
            isActionDrawerOpened = savedInstanceState.getBoolean(EXTRA_IS_ACTION_DRAWER_OPENED, isActionDrawerOpened);
        }

        if (isActionDrawerOpened)
            openActionDrawer();
        //Upload route to Drone or Download receive route from AutoPilot
        try {
            final Drone drone = dpApp.getDrone();
            int updaown=readSharedPreference(UPDOWN_VALUE,KEY_UPDOWN);
            //if updaown==0 =>show Uploading  dialog
            if(updaown == 0)
            {
                final boolean isDroneConnected = drone.isConnected();
                if (isDroneConnected) {
                    final MissionProxy missionProxy = dpApp.getMissionProxy();
                    missionProxy.sendMissionToAPM(drone);
                }
                writeSharedPreference(-1 , UPDOWN_VALUE,KEY_UPDOWN);
            }
            //if updaown==1 =>show Downloading  dialog
            else  if(updaown == 1)
            {
                final boolean isDroneConnected = drone.isConnected();
                if (isDroneConnected) {
                    MissionApi.getApi(drone).loadWaypoints();
                }
                writeSharedPreference(-1 , UPDOWN_VALUE,KEY_UPDOWN);
            }

        }catch (Exception e)
        {

        }
    }

    @Override
    protected void onToolbarLayoutChange(int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom){
        if(flightData != null)
            flightData.updateActionbarShadow(bottom);
    }

    @Override
    protected void addToolbarFragment() {
        final int toolbarId = getToolbarId();
        final FragmentManager fm = getSupportFragmentManager();
        Fragment actionBarTelem = fm.findFragmentById(toolbarId);
        if (actionBarTelem == null) {
            actionBarTelem = new ActionBarTelemFragment();
            fm.beginTransaction().add(toolbarId, actionBarTelem).commit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_IS_ACTION_DRAWER_OPENED, isActionDrawerOpened());
    }

    @Override
    public void onStart(){
        super.onStart();

//        final Context context = getApplicationContext();
//        //Show the changelog if this is the first time the app is launched since update/install
//        if(Utils.getAppVersionCode(context) > mAppPrefs.getSavedAppVersionCode()) {
//            DialogMaterialFragment changelog = new DialogMaterialFragment();
//            changelog.show(getSupportFragmentManager(), "Changelog Dialog");
//
//            mAppPrefs.updateSavedAppVersionCode(context);
//        }
    }

    @Override
    protected int getToolbarId() {
        return R.id.actionbar_toolbar;
    }

    @Override
    protected int getNavigationDrawerMenuItemId() {
        return R.id.navigation_remote_helm;
    }

    @Override
    protected boolean enableMissionMenus() {
        return true;
    }

    @Override
    public void onPanelSlide(View view, float v) {
        final int bottomMargin = (int) getResources().getDimension(R.dimen.action_drawer_margin_bottom);

        //Update the bottom margin for the action drawer
        final View flightActionBar = ((ViewGroup)view).getChildAt(0);
        final int[] viewLocs = new int[2];
        flightActionBar.getLocationInWindow(viewLocs);
        updateActionDrawerBottomMargin(viewLocs[0] + flightActionBar.getWidth(), Math.max((int) (view.getHeight() * v), bottomMargin));
    }

    @Override
    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
        switch(newState){
            case COLLAPSED:
            case HIDDEN:
                resetActionDrawerBottomMargin();
                break;

            case EXPANDED:
                //Update the bottom margin for the action drawer
                ViewGroup slidingPanel = (ViewGroup) ((ViewGroup)panel).getChildAt(1);
                final View flightActionBar = slidingPanel.getChildAt(0);
                final int[] viewLocs = new int[2];
                flightActionBar.getLocationInWindow(viewLocs);
                updateActionDrawerBottomMargin(viewLocs[0] + flightActionBar.getWidth(), slidingPanel.getHeight());
                break;
        }
    }

    private void updateActionDrawerBottomMargin(int rightEdge, int bottomMargin){
        final ViewGroup actionDrawerParent = (ViewGroup) getActionDrawer();
        final View actionDrawer = ((ViewGroup)actionDrawerParent.getChildAt(1)).getChildAt(0);

        final int[] actionDrawerLocs = new int[2];
        actionDrawer.getLocationInWindow(actionDrawerLocs);

        if(actionDrawerLocs[0] <= rightEdge) {
            updateActionDrawerBottomMargin(bottomMargin);
        }
    }

    private int getActionDrawerBottomMargin(){
        final ViewGroup actionDrawerParent = (ViewGroup) getActionDrawer();
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) actionDrawerParent.getLayoutParams();
        return lp.bottomMargin;
    }

    private void updateActionDrawerBottomMargin(int newBottomMargin){
        final ViewGroup actionDrawerParent = (ViewGroup) getActionDrawer();
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) actionDrawerParent.getLayoutParams();
        lp.bottomMargin = newBottomMargin;
        actionDrawerParent.requestLayout();
    }

    private void resetActionDrawerBottomMargin(){
        updateActionDrawerBottomMargin((int) getResources().getDimension(R.dimen.action_drawer_margin_bottom));
    }
}
