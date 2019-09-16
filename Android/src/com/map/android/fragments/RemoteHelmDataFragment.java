package com.map.android.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.map.android.R;
import com.map.android.activities.DrawerNavigationUI;
import com.map.android.client.Drone;
import com.map.android.fragments.control.FlightControlManagerFragment;
import com.map.android.fragments.helpers.ApiListenerFragment;
import com.map.android.fragments.mode.FlightModePanel;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeEventExtra;
import com.map.android.lib.drone.attribute.error.ErrorType;
import com.map.android.utils.prefs.AutoPanMode;
import com.map.android.view.SlidingDrawer;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cc.cloudist.acplibrary.ACProgressConstant;
import cc.cloudist.acplibrary.ACProgressFlower;

/**
 * Created by Pieter Bruegel on 6/27/16.
 */
public class RemoteHelmDataFragment extends ApiListenerFragment implements SlidingDrawer.OnDrawerOpenListener, SlidingDrawer.OnDrawerCloseListener {

    public static final String EXTRA_SHOW_ACTION_DRAWER_TOGGLE = "extra_show_action_drawer_toggle";
    private static final boolean DEFAULT_SHOW_ACTION_DRAWER_TOGGLE = false;

    private static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 101;

    /**
     * Determines how long the failsafe view is visible for.
     */
    private static final long WARNING_VIEW_DISPLAY_TIMEOUT = 10000l; //ms

    private static final IntentFilter eventFilter = new IntentFilter();

    static {
        eventFilter.addAction(AttributeEvent.AUTOPILOT_ERROR);
        eventFilter.addAction(AttributeEvent.AUTOPILOT_MESSAGE);
        eventFilter.addAction(AttributeEvent.STATE_ARMING);
        eventFilter.addAction(AttributeEvent.STATE_CONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_DISCONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_UPDATED);
        eventFilter.addAction(AttributeEvent.TYPE_UPDATED);
        eventFilter.addAction(AttributeEvent.FOLLOW_START);
        eventFilter.addAction(AttributeEvent.MISSION_DRONIE_CREATED);
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case AttributeEvent.AUTOPILOT_ERROR:
                    String errorName = intent.getStringExtra(AttributeEventExtra.EXTRA_AUTOPILOT_ERROR_ID);
                    final ErrorType errorType = ErrorType.getErrorById(errorName);
                    onAutopilotError(errorType);
                    break;

                case AttributeEvent.AUTOPILOT_MESSAGE:
                    final int logLevel = intent.getIntExtra(AttributeEventExtra.EXTRA_AUTOPILOT_MESSAGE_LEVEL, Log.VERBOSE);
                    final String message = intent.getStringExtra(AttributeEventExtra.EXTRA_AUTOPILOT_MESSAGE);
                    onAutopilotError(logLevel, message);
                    break;

                case AttributeEvent.STATE_ARMING:
                case AttributeEvent.STATE_CONNECTED:
                case AttributeEvent.STATE_DISCONNECTED:
                case AttributeEvent.STATE_UPDATED:
                case AttributeEvent.TYPE_UPDATED:
                    enableSlidingUpPanel(getDrone());
                    updateSeekbar();
                    break;

                case AttributeEvent.FOLLOW_START:
                    //Extend the sliding drawer if collapsed.
                    if (!mSlidingPanelCollapsing.get()
                            && mSlidingPanel.isEnabled()
                            && mSlidingPanel.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
                        mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    }
                    break;

                case AttributeEvent.MISSION_DRONIE_CREATED:
                    float dronieBearing = intent.getFloatExtra(AttributeEventExtra.EXTRA_MISSION_DRONIE_BEARING, -1);
                    if (dronieBearing != -1)
                        updateMapBearing(dronieBearing);
                    break;

                case SettingsFragment.ACTION_PREF_JOYSTICKCHANNEL_UPDATE:
                    updateSeekBarProgress();
                    break;
            }
        }
    };

    private final AtomicBoolean mSlidingPanelCollapsing = new AtomicBoolean(false);

    private final String disablePanelSlidingLabel = "disablingListener";
    private final SlidingUpPanelLayout.PanelSlideListener mDisablePanelSliding = new
            SlidingUpPanelLayout.PanelSlideListener() {
                @Override
                public void onPanelSlide(View view, float v) {
                }

                @Override
                public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                    switch(newState){
                        case COLLAPSED:
                            mSlidingPanel.setEnabled(false);
                            mSlidingPanelCollapsing.set(false);

                            //Remove the panel slide listener
                            slidingPanelListenerMgr.removePanelSlideListener(disablePanelSlidingLabel);
                            break;
                    }
                }
            };

    private final Runnable hideWarningViewCb = new Runnable() {
        @Override
        public void run() {
            hideWarningView();
        }
    };

    private final Handler handler = new Handler();

    private View actionbarShadow;

    private View warningContainer;
    private TextView warningText;

    private RemoteHelmMapFragment mapFragment;
    private FlightControlManagerFragment flightActions;

    private SlidingUpPanelLayout mSlidingPanel;

    private FloatingActionButton mGoToMyLocation;
    private FloatingActionButton mGoToDroneLocation;
    private FloatingActionButton actionDrawerToggle;
    private FloatingActionButton mPOIButton;

    private SeekBar mSeekBar;
    private int mChannelMinValue, mChannelMaxValue, mChannelCurValue;
    private int trim;

    private DrawerNavigationUI navActivity;

    private static class SlidingPanelListenerManager implements SlidingUpPanelLayout.PanelSlideListener {
        private final HashMap<String, SlidingUpPanelLayout.PanelSlideListener> panelListenerClients = new HashMap<>();

        public void addPanelSlideListener(String label, SlidingUpPanelLayout.PanelSlideListener listener){
            panelListenerClients.put(label, listener);
        }

        public void removePanelSlideListener(String label){
            panelListenerClients.remove(label);
        }

        @Override
        public void onPanelSlide(View view, float v) {
            for(SlidingUpPanelLayout.PanelSlideListener listener: panelListenerClients.values()){
                listener.onPanelSlide(view, v);
            }
        }

        @Override
        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            for(SlidingUpPanelLayout.PanelSlideListener listener: panelListenerClients.values()){
                listener.onPanelStateChanged(panel, previousState, newState);
            }
        }
    }
    ACProgressFlower dialog;
    public  void showUploadDownloadDialog(String s) {
        if(getActivity()!=null)
        { dialog = new ACProgressFlower.Builder(getActivity())
                .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                .themeColor(getActivity().getResources().getColor(R.color.green))
                .text(s)
                .textSize(30)
                .petalCount(15)
                .petalThickness(4)
                .bgAlpha(0.3f)
                .textColor(getActivity().getResources().getColor(R.color.white))
                .fadeColor(getActivity().getResources().getColor(R.color.white)).build();
            dialog.show();}
    }
    public  void dismissUploadDownloadDialog()
    {
        if(dialog!=null && dialog.isShowing())
            dialog.dismiss();
    }

    private final String parentActivityPanelListenerLabel = "parentListener";

    private final SlidingPanelListenerManager slidingPanelListenerMgr = new SlidingPanelListenerManager();

    private TextView port_tv;
    private TextView stbd_tv;
    private Button portbutt;
    private Button stbdbutt;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DrawerNavigationUI)
            navActivity = (DrawerNavigationUI) activity;

        if(activity instanceof SlidingUpPanelLayout.PanelSlideListener)
            slidingPanelListenerMgr.addPanelSlideListener(parentActivityPanelListenerLabel, (SlidingUpPanelLayout.PanelSlideListener) activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navActivity = null;
        slidingPanelListenerMgr.removePanelSlideListener(parentActivityPanelListenerLabel);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_remote_helm_data, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle arguments = getArguments();
        final boolean showActionDrawerToggle = arguments == null
                ? DEFAULT_SHOW_ACTION_DRAWER_TOGGLE
                : arguments.getBoolean(EXTRA_SHOW_ACTION_DRAWER_TOGGLE, DEFAULT_SHOW_ACTION_DRAWER_TOGGLE);

        actionbarShadow = view.findViewById(R.id.actionbar_shadow);

        final FragmentManager fm = getChildFragmentManager();

        mSlidingPanel = (SlidingUpPanelLayout) view.findViewById(R.id.slidingPanelContainer);
        mSlidingPanel.addPanelSlideListener(slidingPanelListenerMgr);

        warningText = (TextView) view.findViewById(R.id.failsafeTextView);
        warningContainer = view.findViewById(R.id.warningContainer);
        ImageView closeWarningView = (ImageView) view.findViewById(R.id.close_warning_view);
        closeWarningView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideWarningView();
            }
        });

        setupMapFragment();

        mGoToMyLocation = (FloatingActionButton) view.findViewById(R.id.my_location_button);
        mGoToDroneLocation = (FloatingActionButton) view.findViewById(R.id.drone_location_button);
        actionDrawerToggle = (FloatingActionButton) view.findViewById(R.id.toggle_action_drawer);
        mPOIButton = (FloatingActionButton) view.findViewById(R.id.poi_button);

        //zoom in button
        ImageButton zoomin = (ImageButton) view.findViewById(R.id.zoomin);
        zoomin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapFragment.onZoom(view);
            }
        });
        //zoom out button
        ImageButton zoomout = (ImageButton) view.findViewById(R.id.zoomout);
        zoomout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapFragment.onZoom(view);
            }
        });

        port_tv = (TextView) view.findViewById(R.id.port_tv);
        stbd_tv = (TextView) view.findViewById(R.id.textView2);

        if (showActionDrawerToggle) {
            actionDrawerToggle.setVisibility(View.VISIBLE);

            actionDrawerToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (navActivity == null)
                        return;

                    if (navActivity.isActionDrawerOpened())
                        navActivity.closeActionDrawer();
                    else
                        navActivity.openActionDrawer();
                }
            });
        }

        mGoToMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToMyLocation();
                    updateMapLocationButtons(AutoPanMode.DISABLED);
                }
            }
        });
        mGoToMyLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToMyLocation();
                    updateMapLocationButtons(AutoPanMode.USER);
                    return true;
                }
                return false;
            }
        });

        mGoToDroneLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToDroneLocation();
                    updateMapLocationButtons(AutoPanMode.DISABLED);
                }
            }
        });
        mGoToDroneLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mapFragment != null) {
                    mapFragment.goToDroneLocation();
                    updateMapLocationButtons(AutoPanMode.DRONE);
                    return true;
                }
                return false;
            }
        });
        mPOIButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mapFragment.isShowPOI = !mapFragment.isShowPOI;
                if (mapFragment.isShowPOI) {
                    mPOIButton.setImageResource(R.drawable.ic_poi_on);
                } else {
                    mPOIButton.setImageResource(R.drawable.ic_poi_off);
                }
                mapFragment.postUpdatePOI();
            }
        });

        mChannelMinValue = 0;
        mChannelMaxValue = 0;
        trim=0;

        mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);
        portbutt = (Button)view.findViewById(R.id.PORTtrim);
        stbdbutt = (Button)view.findViewById(R.id.STBDtrim);
        final TextView trimtextview=(TextView)view.findViewById(R.id.trimval);
        portbutt.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view) {
                        trim=trim-1;
                        trimtextview.setText(String.valueOf(trim));
                    }
                    });
        stbdbutt.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view) {
                        trim=trim+1;
                        trimtextview.setText(String.valueOf(trim));
                    }
                });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mChannelCurValue = seekBar.getProgress();
                int curValue = mChannelCurValue + mChannelMinValue;
                if(getDrone() != null && getDrone().isConnected()) {
                    getDrone().setCurChannelParameter(curValue);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mChannelCurValue = seekBar.getMax()/2+trim;
                seekBar.setProgress(seekBar.getMax()/2+trim);
                int curValue = mChannelCurValue + mChannelMinValue;
                if(getDrone() != null && getDrone().isConnected()) {
                    getDrone().setCurChannelParameter(curValue);
                }
            }
        });
        updateSeekBarProgress();

        flightActions = (FlightControlManagerFragment) fm.findFragmentById(R.id.flightActionsFragment);
        if (flightActions == null) {
            flightActions = new FlightControlManagerFragment();
            fm.beginTransaction().add(R.id.flightActionsFragment, flightActions).commit();
        }

        // Add the mode info panel fragment
        FlightModePanel flightModePanel = (FlightModePanel) fm.findFragmentById(R.id.sliding_drawer_content);
        if (flightModePanel == null) {
            flightModePanel = new FlightModePanel();
            fm.beginTransaction()
                    .add(R.id.sliding_drawer_content, flightModePanel)
                    .commit();
        }
    }

    private void hideWarningView(){
        handler.removeCallbacks(hideWarningViewCb);

        if (warningContainer != null && warningContainer.getVisibility() != View.GONE)
            warningContainer.setVisibility(View.GONE);
    }

    public void updateActionbarShadow(int shadowHeight){
        if(actionbarShadow == null || actionbarShadow.getLayoutParams().height == shadowHeight)
            return;

        actionbarShadow.getLayoutParams().height = shadowHeight;
        actionbarShadow.requestLayout();
    }

    @Override
    public void onStart() {
        super.onStart();
        setupMapFragment();
        updateMapLocationButtons(getAppPrefs().getAutoPanMode());
        if(getDrone() != null && getDrone().isConnected()) {
            updateSeekbar();
        }
    }

    @Override
    public void onApiConnected() {
        enableSlidingUpPanel(getDrone());
        getBroadcastManager().registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onApiDisconnected() {
        enableSlidingUpPanel(getDrone());
        getBroadcastManager().unregisterReceiver(eventReceiver);
    }

    @Override
    public void onDrawerClosed() {
        if (actionDrawerToggle != null)
            actionDrawerToggle.setActivated(false);
    }

    @Override
    public void onDrawerOpened() {
        if (actionDrawerToggle != null)
            actionDrawerToggle.setActivated(true);
    }

    /**
     * Used to setup the flight screen map fragment. Before attempting to
     * initialize the map fragment, this checks if the Google Play Services
     * binary is installed and up to date.
     */
    private void setupMapFragment() {
        final FragmentManager fm = getChildFragmentManager();
        if (mapFragment == null && isGooglePlayServicesValid(true)) {
            mapFragment = (RemoteHelmMapFragment) fm.findFragmentById(R.id.remote_helm_map_fragment);
            if (mapFragment == null) {
                mapFragment = new RemoteHelmMapFragment();
                fm.beginTransaction().add(R.id.remote_helm_map_fragment, mapFragment).commit();
            }
        }
    }

    private void updateMapLocationButtons(AutoPanMode mode) {
        mGoToMyLocation.setActivated(false);
        mGoToDroneLocation.setActivated(false);

        if (mapFragment != null) {
            mapFragment.setAutoPanMode(mode);
        }

        switch (mode) {
            case DRONE:
                mGoToDroneLocation.setActivated(true);
                break;

            case USER:
                mGoToMyLocation.setActivated(true);
                break;
            default:
                break;
        }
    }

    public void updateMapBearing(float bearing) {
        if (mapFragment != null)
            mapFragment.updateMapBearing(bearing);
    }

    private void updateSeekBarProgress() {
        //mChannelCurValue = Integer.parseInt(getAppPrefs().getJoystickChannel().split(" ")[1]) - 1;
        mChannelCurValue = mSeekBar.getMax() / 2;
        mSeekBar.setProgress(mChannelCurValue);
    }
    private void enableSlidingUpPanel(Drone api) {
        if (mSlidingPanel == null || api == null) {
            return;
        }

        final boolean isEnabled = flightActions != null && flightActions.isSlidingUpPanelEnabled(api);

        if (isEnabled) {
            mSlidingPanel.setEnabled(true);
            mSlidingPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    SlidingUpPanelLayout.PanelState panelState = mSlidingPanel.getPanelState();
                    slidingPanelListenerMgr.onPanelStateChanged(mSlidingPanel, panelState, panelState);
                    mSlidingPanel.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });

        } else {
            if (!mSlidingPanelCollapsing.get()) {
                SlidingUpPanelLayout.PanelState panelState = mSlidingPanel.getPanelState();
                if (panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    slidingPanelListenerMgr.addPanelSlideListener(disablePanelSlidingLabel, mDisablePanelSliding);
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    mSlidingPanelCollapsing.set(true);
                } else {
                    mSlidingPanel.setEnabled(false);
                    mSlidingPanelCollapsing.set(false);
                }
            }
        }
    }

    private void updateSeekbar() {
        mChannelMinValue = (int) getDrone().getChannelMinParameter();
        mChannelMaxValue = (int) getDrone().getChannelMaxParameter();
        mSeekBar.setMax(mChannelMaxValue - mChannelMinValue);
        mChannelCurValue = mSeekBar.getMax() / 2;
        mSeekBar.setProgress(mChannelCurValue);
     }

    private void onAutopilotError(ErrorType errorType) {
        if (errorType == null)
            return;

        final CharSequence errorLabel;
        switch (errorType) {
            case NO_ERROR:
                errorLabel = null;
                break;

            default:
                errorLabel = errorType.getLabel(getContext());
                break;
        }

        onAutopilotError(Log.ERROR, errorLabel);
    }

    private void onAutopilotError(int logLevel, CharSequence errorMsg) {
        if (TextUtils.isEmpty(errorMsg))
            return;

        switch (logLevel) {
            case Log.ERROR:
            case Log.WARN:
                handler.removeCallbacks(hideWarningViewCb);

                warningText.setText(errorMsg);
                warningContainer.setVisibility(View.VISIBLE);
                handler.postDelayed(hideWarningViewCb, WARNING_VIEW_DISPLAY_TIMEOUT);
                break;
        }
    }

    /**
     * Ensures that the device has the correct version of the Google Play
     * Services.
     *
     * @return true if the Google Play Services binary is valid
     */
    private boolean isGooglePlayServicesValid(boolean showErrorDialog) {
        // Check for the google play services is available
        final int playStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getContext());
        final boolean isValid = playStatus == ConnectionResult.SUCCESS;

        if (!isValid && showErrorDialog) {
            final Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(playStatus, getActivity(),
                    GOOGLE_PLAY_SERVICES_REQUEST_CODE, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (isAdded()) {
                                getActivity().finish();
                            }
                        }
                    });

            if (errorDialog != null)
                errorDialog.show();
        }

        return isValid;
    }

    public void setGuidedClickListener(OnGuidedClickListener listener) {
        mapFragment.setGuidedClickListener(listener);
    }

    public void addMapMarkerProvider(DroneMap.MapMarkerProvider provider) {
        mapFragment.addMapMarkerProvider(provider);
    }

    public void removeMapMarkerProvider(DroneMap.MapMarkerProvider provider) {
        mapFragment.removeMapMarkerProvider(provider);
    }
    //disable the remote helm views
    public void disableRemoteHelm() {
        Drawable seekbarThumb = getResources().getDrawable( R.drawable.channel_seekbar_thumb_grey);
        mSeekBar .setThumb(seekbarThumb );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSeekBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.dark_grey)));
        }
        else
        {
            mSeekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(getResources().getColor(R.color.dark_grey), PorterDuff.Mode.MULTIPLY));
        }
        port_tv.setTextColor(getResources().getColor(R.color.all_black));
        stbd_tv.setTextColor(getResources().getColor(R.color.dark_grey));
        portbutt.setEnabled(false);
        stbdbutt.setEnabled(false);
        portbutt.setTextColor(getResources().getColor(R.color.all_black));
        stbdbutt.setTextColor(getResources().getColor(R.color.dark_grey));
    }
    //enable the remote helm views
    public void enableRemoteHelm() {
        Drawable seekbarThumb = getResources().getDrawable( R.drawable.channel_seekbar_thumb );
        mSeekBar .setThumb(seekbarThumb );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSeekBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.green_dark)));
        }
        else
        {
            mSeekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(getResources().getColor(R.color.green_dark), PorterDuff.Mode.MULTIPLY));
        }

        port_tv.setTextColor(getResources().getColor(R.color.header_background));
        stbd_tv.setTextColor(getResources().getColor(R.color.holo_green_light));
        portbutt.setEnabled(true);
        stbdbutt.setEnabled(true);
        portbutt.setTextColor(getResources().getColor(R.color.header_background));
        stbdbutt.setTextColor(getResources().getColor(R.color.holo_green_light));
    }
}
