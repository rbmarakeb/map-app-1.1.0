package com.map.android.fragments.actionbar;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.map.android.activities.RemoteHelmActivity;
import com.map.android.client.apis.VehicleApi;
import com.map.android.fragments.FlightDataFragment;
import com.map.android.fragments.RemoteHelmDataFragment;
import com.map.android.fragments.control.FlightControlManagerFragment;
import com.map.android.fragments.control.RoverFlightControlFragment;
import com.map.android.lib.drone.property.Speed;
import com.map.android.lib.drone.property.Type;
import com.map.android.lib.drone.property.VehicleMode;
import com.map.android.utils.analytics.GAUtils;
import com.map.android.utils.prefs.DroidPlannerPrefs;
import com.map.android.client.Drone;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.State;

import com.map.android.R;
import com.map.android.dialogs.SelectionListDialog;
import com.map.android.fragments.SettingsFragment;
import com.map.android.fragments.helpers.ApiListenerFragment;
import com.map.android.utils.Utils;

import java.util.List;
import java.util.Locale;

/**
 * Created by Fredia Huya-Kouadio on 1/14/15.
 */
public class ActionBarTelemFragment extends ApiListenerFragment {

    private final static IntentFilter eventFilter = new IntentFilter();
    private boolean showFirstTimeOnly = true;

    static {
        eventFilter.addAction(AttributeEvent.BATTERY_UPDATED);
        eventFilter.addAction(AttributeEvent.STATE_CONNECTED);
        eventFilter.addAction(AttributeEvent.STATE_DISCONNECTED);
        eventFilter.addAction(AttributeEvent.GPS_POSITION);
        eventFilter.addAction(AttributeEvent.GPS_COUNT);
        eventFilter.addAction(AttributeEvent.GPS_FIX);
        eventFilter.addAction(AttributeEvent.SIGNAL_UPDATED);
        eventFilter.addAction(AttributeEvent.STATE_VEHICLE_MODE);
        eventFilter.addAction(AttributeEvent.TYPE_UPDATED);
        eventFilter.addAction(AttributeEvent.ALTITUDE_UPDATED);

        eventFilter.addAction(SettingsFragment.ACTION_PREF_HDOP_UPDATE);
        eventFilter.addAction(SettingsFragment.ACTION_PREF_UNIT_SYSTEM_UPDATE);

        eventFilter.addAction(DroidPlannerPrefs.ACTION_PREF_RETURN_TO_ME_UPDATED);
        eventFilter.addAction(AttributeEvent.RETURN_TO_ME_STATE_UPDATE);
        eventFilter.addAction(AttributeEvent.HOME_UPDATED);
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getActivity() == null)
                return;

            switch (intent.getAction()) {
//                case AttributeEvent.BATTERY_UPDATED:
//                    updateBatteryTelem();
//                    break;

                case AttributeEvent.STATE_CONNECTED:
                    showTelemBar();
                    updateAllTelem();
                    break;

                case AttributeEvent.STATE_DISCONNECTED:
                    hideTelemBar();
                    updateAllTelem();
                    break;

//                case DroidPlannerPrefs.ACTION_PREF_RETURN_TO_ME_UPDATED:
//                case AttributeEvent.RETURN_TO_ME_STATE_UPDATE:
//                case AttributeEvent.GPS_POSITION:
//                case AttributeEvent.HOME_UPDATED:
//                    updateHomeTelem();
//                    break;
//
//                case AttributeEvent.GPS_COUNT:
//                case AttributeEvent.GPS_FIX:
//                    updateGpsTelem();
//                    break;
//
//                case AttributeEvent.SIGNAL_UPDATED:
//                    updateSignalTelem();
//                    break;

                case AttributeEvent.STATE_VEHICLE_MODE:
                case AttributeEvent.TYPE_UPDATED:
                    updateFlightModeTelem();
                    break;

//                case SettingsFragment.ACTION_PREF_HDOP_UPDATE:
//                    updateGpsTelem();
//                    break;
//
//                case SettingsFragment.ACTION_PREF_UNIT_SYSTEM_UPDATE:
//                    updateHomeTelem();
//                    break;
//
//                case AttributeEvent.ALTITUDE_UPDATED:
//                    updateAltitudeTelem();
//                    break;

                default:
                    break;
            }
        }
    };

    private DroidPlannerPrefs appPrefs;

//    private TextView homeTelem;
//    private TextView altitudeTelem;

//    private TextView gpsTelem;
//    private PopupWindow gpsPopup;

//    private TextView batteryTelem;
//    private PopupWindow batteryPopup;

//    private TextView signalTelem;
//    private PopupWindow signalPopup;

    private TextView flightModeTelem;
    // EMERGENCY STOP button enable/disable hold mode.
    private Button hold_mode_btn;

    private String emptyString;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_action_bar_telem, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyString = getString(R.string.empty_content);

        final Context context = getActivity().getApplicationContext();
        final LayoutInflater inflater = LayoutInflater.from(context);

        final int popupWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
        final int popupHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
        final Drawable popupBg = getResources().getDrawable(android.R.color.transparent);

//        homeTelem = (TextView) view.findViewById(R.id.bar_home);
//        homeTelem.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //Launch dialog to allow the user to select between rtl and rtm
//                final SelectionListDialog selectionDialog = SelectionListDialog.newInstance(new ReturnToHomeAdapter(context, getDrone(), appPrefs));
//                Utils.showDialog(selectionDialog, getChildFragmentManager(), "Return to home type", true);
//            }
//        });

//        altitudeTelem = (TextView) view.findViewById(R.id.bar_altitude);

//        gpsTelem = (TextView) view.findViewById(R.id.bar_gps);
//        final View gpsPopupView = inflater.inflate(R.layout.popup_info_gps, (ViewGroup) view, false);
//        gpsPopup = new PopupWindow(gpsPopupView, popupWidth, popupHeight, true);
//        gpsPopup.setBackgroundDrawable(popupBg);
//        gpsTelem.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                gpsPopup.showAsDropDown(gpsTelem);
//            }
//        });

//        batteryTelem = (TextView) view.findViewById(R.id.bar_battery);
//        final View batteryPopupView = inflater.inflate(R.layout.popup_info_power, (ViewGroup) view, false);
//        batteryPopup = new PopupWindow(batteryPopupView, popupWidth, popupHeight, true);
//        batteryPopup.setBackgroundDrawable(popupBg);
//        batteryTelem.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                batteryPopup.showAsDropDown(batteryTelem);
//            }
//        });

//        signalTelem = (TextView) view.findViewById(R.id.bar_signal);
//        final View signalPopupView = inflater.inflate(R.layout.popup_info_signal, (ViewGroup) view, false);
//        signalPopup = new PopupWindow(signalPopupView, popupWidth, popupHeight, true);
//        signalPopup.setBackgroundDrawable(popupBg);
//        signalTelem.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                signalPopup.showAsDropDown(signalTelem);
//            }
//        });

        flightModeTelem = (TextView) view.findViewById(R.id.bar_flight_mode);
        flightModeTelem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Launch dialog to allow the user to select vehicle modes
                final Drone drone = getDrone();

                final SelectionListDialog selectionDialog = SelectionListDialog.newInstance(new FlightModeAdapter(context, drone));
                Utils.showDialog(selectionDialog, getChildFragmentManager(), "Flight modes selection", true);
            }
        });
        // EMERGENCY STOP button enable/disable hold mode.
        hold_mode_btn= (Button) view.findViewById(R.id.hold_mode_btn);
        hold_mode_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Drone drone = getDrone();
                final boolean isDroneConnected =drone.isConnected();;
                final State droneState = drone.getAttribute(AttributeType.STATE);
                if (isDroneConnected) {

                    Type type = drone.getAttribute(AttributeType.TYPE);
                    //get current mode
                    final VehicleMode vehicleMode = droneState.getVehicleMode();
                    List<VehicleMode> flightModes = VehicleMode.getVehicleModePerDroneType(type.getDroneType());
                    VehicleMode new_vehicleMode = vehicleMode;
                    boolean isChanged = false;
                    //if mode hold
                    for (int i = 0; i < flightModes.size(); i++) {
                        if (flightModes.get(i).getLabel().equals("Hold")) {
                            new_vehicleMode = flightModes.get(i);
                            isChanged = true;
                            break;
                        }
                    }
                    VehicleApi.getApi(drone).setVehicleMode(new_vehicleMode);
                    HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                            .setCategory(GAUtils.Category.FLIGHT);
                    eventBuilder.setAction("Flight mode changed").setLabel(new_vehicleMode.getLabel());
                    flightModeTelem.setText(new_vehicleMode.getLabel());
                    flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_light_blue_a400_24dp, 0, 0, 0);

                    if (!isChanged) {
                        Toast.makeText(getContext(), "Can't switch to Hold Mode", Toast.LENGTH_LONG).show();
                        hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_enable));
                        hold_mode_btn.setEnabled(true);
                    }
                    else
                        {
                        hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_enable));
                        hold_mode_btn.setEnabled(false);

                    }
                }
            }
        });

        appPrefs = DroidPlannerPrefs.getInstance(context);
    }

    private void showTelemBar() {
        final View view = getView();
        if (view != null)
            view.setVisibility(View.VISIBLE);
    }

    private void hideTelemBar() {
        final View view = getView();
        if (view != null)
            view.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        hideTelemBar();
        super.onStart();
    }

    @Override
    public void onApiConnected() {
        final Drone drone = getDrone();
        if (drone.isConnected())
            showTelemBar();
        else
            hideTelemBar();

        updateAllTelem();
        getBroadcastManager().registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onApiDisconnected() {
        getBroadcastManager().unregisterReceiver(eventReceiver);
    }

    private void updateAllTelem() {
        updateFlightModeTelem();
//        updateSignalTelem();
//        updateGpsTelem();
//        updateHomeTelem();
//        updateBatteryTelem();
//        updateAltitudeTelem();
    }

    private void updateFlightModeTelem() {
        final Drone drone = getDrone();

        final boolean isDroneConnected = drone.isConnected();
        final State droneState = drone.getAttribute(AttributeType.STATE);
        if (isDroneConnected) {

            //get current mode
            final VehicleMode vehicleMode=droneState.getVehicleMode();
            //check if the current activity is RemoteHelmActivity or not
            if(getActivity().getClass().getSimpleName().equals("RemoteHelmActivity"))
            {
                //if current mode not equal zero 0 => mode is not Manual
                if( !vehicleMode.getLabel().equals("Manual")) {
                    //if mode auto
                    if (vehicleMode.getLabel().equals("Auto")) {
                        hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_enable));
                        hold_mode_btn.setEnabled(true);
                        RemoteHelmDataFragment f = RemoteHelmActivity.getInstance();
                        f.enableRemoteHelm();
                    }
                    //if mode hold
                    else if (vehicleMode.getLabel().equals("Hold")) {
                        hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_disabled));
                        hold_mode_btn.setEnabled(false);
                        RemoteHelmDataFragment f = RemoteHelmActivity.getInstance();
                        f.disableRemoteHelm();
                    } else {
                        hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.light_grey));
                        hold_mode_btn.setEnabled(false);
                        RemoteHelmDataFragment f = RemoteHelmActivity.getInstance();
                        f.disableRemoteHelm();
                    }
                    flightModeTelem.setText(droneState.getVehicleMode().getLabel());
                    flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_light_blue_a400_24dp, 0, 0, 0);
                    if (showFirstTimeOnly)
                    {
                      showFirstTimeOnly=false;
                     final Dialog dialog = new Dialog(getContext());
                    dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.dialog_switch_mode);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));


                    Button confirm = (Button) dialog.findViewById(R.id.confirm);
                    Button ignore = (Button) dialog.findViewById(R.id.ignore);
                    // if button is clicked, close the custom dialog
                    confirm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Type type = drone.getAttribute(AttributeType.TYPE);
                            List<VehicleMode> flightModes = VehicleMode.getVehicleModePerDroneType(type.getDroneType());
                            VehicleMode new_vehicleMode = vehicleMode;
                            boolean isChanged = false;
                            for (int i = 0; i < flightModes.size(); i++) {
                                if (flightModes.get(i).getLabel().equals("Manual")) {
                                    new_vehicleMode = flightModes.get(i);
                                    isChanged = true;
                                    break;
                                }
                            }
                            VehicleApi.getApi(drone).setVehicleMode(new_vehicleMode);
                            HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                                    .setCategory(GAUtils.Category.FLIGHT);
                            eventBuilder.setAction("Flight mode changed").setLabel(new_vehicleMode.getLabel());

                            flightModeTelem.setText(new_vehicleMode.getLabel());
                            flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_light_blue_a400_24dp, 0, 0, 0);
                            RemoteHelmDataFragment f = RemoteHelmActivity.getInstance();
                            if (!isChanged) {
                                Toast.makeText(getContext(), "Can't switch to Manual Mode", Toast.LENGTH_LONG).show();
                                f.disableRemoteHelm();
                            } else {
                                f.enableRemoteHelm();

                                hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_enable));
                                hold_mode_btn.setEnabled(true);

                            }
                            dialog.dismiss();
                        }
                    });
                    ignore.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            flightModeTelem.setText(droneState.getVehicleMode().getLabel());
                            flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_light_blue_a400_24dp, 0, 0, 0);
                            // /if current mode equal 4 => mode is Hold
                            if (vehicleMode.getLabel().equals("Hold")) {
                                RemoteHelmDataFragment f = RemoteHelmActivity.getInstance();
                                f.disableRemoteHelm();
                            }
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
                }
                // current mode equal zero 0 => mode is Manual
                else
                {
                    showFirstTimeOnly=false;
                    hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_enable));
                    hold_mode_btn.setEnabled(true);

                    RemoteHelmDataFragment f= RemoteHelmActivity.getInstance();
                    f.enableRemoteHelm();
                    flightModeTelem.setText(droneState.getVehicleMode().getLabel());
                    flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_light_blue_a400_24dp, 0, 0, 0);
                }
            }
            else
            {
                showFirstTimeOnly=true;
                if(getActivity().getClass().getSimpleName().equals("FlightActivity"))
                {
                    if(vehicleMode.getLabel().equals("Auto"))
                    {
                        hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_enable));
                        hold_mode_btn.setEnabled(true);
                    }
                    //if mode hold
                    else if(vehicleMode.getLabel().equals("Hold"))
                    {
                        hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_disabled));
                        hold_mode_btn.setEnabled(false);
                    }
                    //if mode Manual
                    else if(vehicleMode.getLabel().equals("Manual"))
                    {
                        hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_enable));
                        hold_mode_btn.setEnabled(true);
                    }
                    else
                    {
                        hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.light_grey));
                        hold_mode_btn.setEnabled(false);

                    }
                }

                flightModeTelem.setText(droneState.getVehicleMode().getLabel());
                flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_light_blue_a400_24dp, 0, 0, 0);
            }

            if (vehicleMode.getLabel().equals("Auto")) {
                Speed droneSpeed = drone.getAttribute(AttributeType.SPEED);
                double speed_kts=0;
                if(droneSpeed!=null)
                    speed_kts=droneSpeed.getGroundSpeed() * 1.94384;
                if(speed_kts>15)
                {   FlightDataFragment.showWarningView();
                    RoverFlightControlFragment actionsBarFragment= (RoverFlightControlFragment) FlightControlManagerFragment.getactionsBarFragmentInstance();
                    actionsBarFragment.changeFraqmentRoverControlBourder(false);
                    updateModeBySpeed();

                }
                else
                {
                    try {
                    RoverFlightControlFragment actionsBarFragment= (RoverFlightControlFragment) FlightControlManagerFragment.getactionsBarFragmentInstance();
                    actionsBarFragment.changeFraqmentRoverControlBourder(true);
                }catch (Exception e)

                {

                }
                }
            }

        } else {
            //check if the current activity is RemoteHelmActivity or not
            if(getActivity().getClass().getSimpleName().equals("RemoteHelmActivity")) {
                RemoteHelmDataFragment f = RemoteHelmActivity.getInstance();
                f.disableRemoteHelm();
            }
            flightModeTelem.setText(emptyString);
            flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_grey_700_24dp, 0, 0, 0);
        }
    }
    //used to update mode from Auto to Hold by speed
    public void updateModeBySpeed()
    {
        final Drone drone = getDrone();

        final boolean isDroneConnected =drone.isConnected();
        final State droneState = drone.getAttribute(AttributeType.STATE);

        if (isDroneConnected) {
            //get current mode
            final VehicleMode vehicleMode=droneState.getVehicleMode();
            //if mode auto
            if(vehicleMode.getLabel().equals("Auto")) {
                Type type = drone.getAttribute(AttributeType.TYPE);
                List<VehicleMode> flightModes = VehicleMode.getVehicleModePerDroneType(type.getDroneType());
                VehicleMode new_vehicleMode = vehicleMode;
                boolean isChanged = false;
                for (int i = 0; i < flightModes.size(); i++) {
                    if (flightModes.get(i).getLabel().equals("Hold")) {
                        new_vehicleMode = flightModes.get(i);
                        isChanged = true;
                        break;
                    }
                }
                VehicleApi.getApi(drone).setVehicleMode(new_vehicleMode);
                HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                        .setCategory(GAUtils.Category.FLIGHT);
                eventBuilder.setAction("Flight mode changed").setLabel(new_vehicleMode.getLabel());
                flightModeTelem.setText(new_vehicleMode.getLabel());
                flightModeTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigation_light_blue_a400_24dp, 0, 0, 0);
                RemoteHelmDataFragment f = RemoteHelmActivity.getInstance();
                if (!isChanged) {
                    Toast.makeText(getContext(), "Can't switch to Hold Mode", Toast.LENGTH_LONG).show();
                    hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_enable));
                    hold_mode_btn.setEnabled(true);
                } else {
                    hold_mode_btn.setBackgroundColor(getResources().getColor(R.color.orange_light_enable));
                    hold_mode_btn.setEnabled(false);

                }
            }
        }
    }
    private void updateSignalTelem() {
        final Drone drone = getDrone();

//        final View popupView = signalPopup.getContentView();
//        TextView rssiView = (TextView) popupView.findViewById(R.id.bar_signal_rssi);
//        TextView remRssiView = (TextView) popupView.findViewById(R.id.bar_signal_remrssi);
//        TextView noiseView = (TextView) popupView.findViewById(R.id.bar_signal_noise);
//        TextView remNoiseView = (TextView) popupView.findViewById(R.id.bar_signal_remnoise);
//        TextView fadeView = (TextView) popupView.findViewById(R.id.bar_signal_fade);
//        TextView remFadeView = (TextView) popupView.findViewById(R.id.bar_signal_remfade);
//
//        final Signal droneSignal = drone.getAttribute(AttributeType.SIGNAL);
//        if (!drone.isConnected() || !droneSignal.isValid()) {
//            signalTelem.setText(emptyString);
//            signalTelem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_signal_cellular_null_grey_700_18dp,
//                    0, 0, 0);
//
//            rssiView.setText("RSSI: " + emptyString);
//            remRssiView.setText("RemRSSI: " + emptyString);
//            noiseView.setText("Noise: " + emptyString);
//            remNoiseView.setText("RemNoise: " + emptyString);
//            fadeView.setText("Fade: " + emptyString);
//            remFadeView.setText("RemFade: " + emptyString);
//        } else {
//            final int signalStrength = (int) droneSignal.getSignalStrength();
//            final int signalIcon;
//            if (signalStrength >= 100)
//                signalIcon = R.drawable.ic_signal_cellular_4_bar_grey_700_18dp;
//            else if (signalStrength >= 75)
//                signalIcon = R.drawable.ic_signal_cellular_3_bar_grey_700_18dp;
//            else if (signalStrength >= 50)
//                signalIcon = R.drawable.ic_signal_cellular_2_bar_grey_700_18dp;
//            else if (signalStrength >= 25)
//                signalIcon = R.drawable.ic_signal_cellular_1_bar_grey_700_18dp;
//            else
//                signalIcon = R.drawable.ic_signal_cellular_0_bar_grey_700_18dp;
//
//            signalTelem.setText(String.format(Locale.ENGLISH, "%d%%", signalStrength));
//            signalTelem.setCompoundDrawablesWithIntrinsicBounds(signalIcon, 0, 0, 0);
//
//            rssiView.setText(String.format("RSSI %2.0f dB", droneSignal.getRssi()));
//            remRssiView.setText(String.format("RemRSSI %2.0f dB", droneSignal.getRemrssi()));
//            noiseView.setText(String.format("Noise %2.0f dB", droneSignal.getNoise()));
//            remNoiseView.setText(String.format("RemNoise %2.0f dB", droneSignal.getRemnoise()));
//            fadeView.setText(String.format("Fade %2.0f dB", droneSignal.getFadeMargin()));
//            remFadeView.setText(String.format("RemFade %2.0f dB", droneSignal.getRemFadeMargin()));
//        }
//
//        signalPopup.update();
    }

    private void updateGpsTelem() {
        final Drone drone = getDrone();
        final boolean displayHdop = appPrefs.shouldGpsHdopBeDisplayed();

//        final View popupView = gpsPopup.getContentView();
//        TextView satNoView = (TextView) popupView.findViewById(R.id.bar_gps_satno);
//        TextView hdopStatusView = (TextView) popupView.findViewById(R.id.bar_gps_hdop_status);
//        hdopStatusView.setVisibility(displayHdop ? View.GONE : View.VISIBLE);
//
//        final String update;
//        final int gpsIcon;
//        if (!drone.isConnected()) {
//            update = (displayHdop ? "hdop: " : "") + emptyString;
//            gpsIcon = R.drawable.ic_gps_off_grey_700_18dp;
//            satNoView.setText("S: " + emptyString);
//            hdopStatusView.setText("hdop: " + emptyString);
//        } else {
//            Gps droneGps = drone.getAttribute(AttributeType.GPS);
//            final String fixStatus = droneGps.getFixStatus();
//
//            if (displayHdop) {
//                update = String.format(Locale.ENGLISH, "hdop: %.1f", droneGps.getGpsEph());
//            } else {
//                update = String.format(Locale.ENGLISH, "%s", fixStatus);
//            }
//
//            switch (fixStatus) {
//                case Gps.LOCK_3D:
//                case Gps.LOCK_3D_DGPS:
//                case Gps.LOCK_3D_RTK:
//                    gpsIcon = R.drawable.ic_gps_fixed_black_24dp;
//                    break;
//
//                case Gps.LOCK_2D:
//                case Gps.NO_FIX:
//                default:
//                    gpsIcon = R.drawable.ic_gps_not_fixed_grey_700_18dp;
//                    break;
//            }
//
//            satNoView.setText(String.format(Locale.ENGLISH, "S: %d", droneGps.getSatellitesCount()));
//            if (appPrefs.shouldGpsHdopBeDisplayed()) {
//                hdopStatusView.setText(String.format(Locale.ENGLISH, "%s", fixStatus));
//            } else {
//                hdopStatusView.setText(String.format(Locale.ENGLISH, "hdop: %.1f", droneGps.getGpsEph()));
//            }
//        }
//
//        gpsTelem.setText(update);
//        gpsTelem.setCompoundDrawablesWithIntrinsicBounds(gpsIcon, 0, 0, 0);
//        gpsPopup.update();
    }

    private void updateHomeTelem() {
        final Drone drone = getDrone();

//        String update = getString(R.string.empty_content);
//        int drawableResId = appPrefs.isReturnToMeEnabled()
//                ? R.drawable.ic_person_grey_700_18dp
//                : R.drawable.ic_home_grey_700_18dp;
//
//        if (drone.isConnected()) {
//            final Gps droneGps = drone.getAttribute(AttributeType.GPS);
//            final Home droneHome = drone.getAttribute(AttributeType.HOME);
//            if (droneGps.isValid() && droneHome.isValid()) {
//                LengthUnit distanceToHome = getLengthUnitProvider().boxBaseValueToTarget
//                        (MathUtils.getDistance2D(droneHome.getCoordinate(), droneGps.getPosition()));
//                update = String.format("%s", distanceToHome);
//
//                final ReturnToMeState returnToMe = drone.getAttribute(AttributeType.RETURN_TO_ME_STATE);
//                switch (returnToMe.getState()) {
//
//                    case ReturnToMeState.STATE_UPDATING_HOME:
//                        //Change the home telemetry icon
//                        drawableResId = R.drawable.ic_person_blue_a400_18dp;
//                        break;
//
//                    case ReturnToMeState.STATE_USER_LOCATION_INACCURATE:
//                    case ReturnToMeState.STATE_USER_LOCATION_UNAVAILABLE:
//                    case ReturnToMeState.STATE_WAITING_FOR_VEHICLE_GPS:
//                    case ReturnToMeState.STATE_ERROR_UPDATING_HOME:
//                        drawableResId = R.drawable.ic_person_red_500_18dp;
//                        update = getString(R.string.empty_content);
//                        break;
//                }
//            }
//        }
//
//        homeTelem.setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, 0, 0);
//        homeTelem.setText(update);
    }

    private void updateBatteryTelem() {
        final Drone drone = getDrone();

//        final View batteryPopupView = batteryPopup.getContentView();
//        final TextView dischargeView = (TextView) batteryPopupView.findViewById(R.id.bar_power_discharge);
//        final TextView currentView = (TextView) batteryPopupView.findViewById(R.id.bar_power_current);
//        final TextView remainView = (TextView) batteryPopupView.findViewById(R.id.bar_power_remain);
//
//        String update;
//        Battery droneBattery;
//        final int batteryIcon;
//        if (!drone.isConnected() || ((droneBattery = drone.getAttribute(AttributeType.BATTERY)) == null)) {
//            update = emptyString;
//            dischargeView.setText("D: " + emptyString);
//            currentView.setText("C: " + emptyString);
//            remainView.setText("R: " + emptyString);
//            batteryIcon = R.drawable.ic_battery_circle_0_24dp;
//        } else {
//            Double discharge = droneBattery.getBatteryDischarge();
//            String dischargeText;
//            if (discharge == null) {
//                dischargeText = "D: " + emptyString;
//            } else {
//                dischargeText = "D: " + electricChargeToString(discharge);
//            }
//
//            dischargeView.setText(dischargeText);
//
//            final double battRemain = droneBattery.getBatteryRemain();
//            remainView.setText(String.format(Locale.ENGLISH, "R: %2.0f %%", battRemain));
//            currentView.setText(String.format("C: %2.1f A", droneBattery.getBatteryCurrent()));
//
//
//            update = String.format(Locale.ENGLISH, "%2.1f V", droneBattery.getBatteryVoltage());
//
//            if (battRemain >= 100) {
//                batteryIcon = R.drawable.ic_battery_circle_8_24dp;
//            } else if (battRemain >= 87.5) {
//                batteryIcon = R.drawable.ic_battery_circle_7_24dp;
//            } else if (battRemain >= 75) {
//                batteryIcon = R.drawable.ic_battery_circle_6_24dp;
//            } else if (battRemain >= 62.5) {
//                batteryIcon = R.drawable.ic_battery_circle_5_24dp;
//            } else if (battRemain >= 50) {
//                batteryIcon = R.drawable.ic_battery_circle_4_24dp;
//            } else if (battRemain >= 37.5) {
//                batteryIcon = R.drawable.ic_battery_circle_3_24dp;
//            } else if (battRemain >= 25) {
//                batteryIcon = R.drawable.ic_battery_circle_2_24dp;
//            } else if (battRemain >= 12.5) {
//                batteryIcon = R.drawable.ic_battery_circle_1_24dp;
//            } else {
//                batteryIcon = R.drawable.ic_battery_circle_0_24dp;
//            }
//        }
//
//        batteryPopup.update();
//        batteryTelem.setText(update);
//        batteryTelem.setCompoundDrawablesWithIntrinsicBounds(batteryIcon, 0, 0, 0);
    }

    private String electricChargeToString(double chargeInmAh) {
        double absCharge = Math.abs(chargeInmAh);
        if (absCharge >= 1000) {
            return String.format(Locale.US, "%2.1f Ah", chargeInmAh / 1000);
        } else {
            return String.format(Locale.ENGLISH, "%2.0f mAh", chargeInmAh);
        }
    }

    private void updateAltitudeTelem() {
        final Drone drone = getDrone();
//        final Altitude altitude = drone.getAttribute(AttributeType.ALTITUDE);
//        if (altitude != null) {
//            double alt = altitude.getAltitude();
//            LengthUnit altUnit = getLengthUnitProvider().boxBaseValueToTarget(alt);
//
//            this.altitudeTelem.setText(altUnit.toString());
//        }
    }

}
