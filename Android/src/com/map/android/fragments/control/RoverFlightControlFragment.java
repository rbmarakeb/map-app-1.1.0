package com.map.android.fragments.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.map.android.activities.helpers.SuperUI;
import com.map.android.fragments.FlightDataFragment;
import com.map.android.lib.drone.property.Speed;
import com.map.android.utils.analytics.GAUtils;
import com.map.android.client.Drone;
import com.map.android.client.apis.VehicleApi;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.State;
import com.map.android.lib.drone.property.VehicleMode;

import com.map.android.R;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

/**
 * Created by Fredia Huya-Kouadio on 3/4/15.
 */
public class RoverFlightControlFragment extends BaseFlightControlFragment {

    private static final String ACTION_FLIGHT_ACTION_BUTTON = "Rover flight action button";

    private static final IntentFilter intentFilter = new IntentFilter();

    static {
        intentFilter.addAction(AttributeEvent.STATE_ARMING);
        intentFilter.addAction(AttributeEvent.STATE_CONNECTED);
        intentFilter.addAction(AttributeEvent.STATE_DISCONNECTED);
        intentFilter.addAction(AttributeEvent.STATE_UPDATED);
        intentFilter.addAction(AttributeEvent.STATE_VEHICLE_MODE);
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case AttributeEvent.STATE_ARMING:
                case AttributeEvent.STATE_CONNECTED:
                case AttributeEvent.STATE_DISCONNECTED:
                case AttributeEvent.STATE_UPDATED:
                    setupButtonsByFlightState();
                    break;

                case AttributeEvent.STATE_VEHICLE_MODE:
                    updateFlightModeButtons();
                    break;

            }
        }
    };

    private View mDisconnectedButtons;
    private View mActiveButtons;

    //    private Button homeBtn;
    private Button pauseBtn;
    private Button autoBtn;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private LinearLayout fraqment_rover_control_linear;
    Context mcontext;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rover_mission_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDisconnectedButtons = view.findViewById(R.id.mc_disconnected_buttons);
        mActiveButtons = view.findViewById(R.id.mc_connected_buttons);

        final View connectBtn = view.findViewById(R.id.mc_connectBtn);
        connectBtn.setOnClickListener(this);
        ////////////////////

        //////////////////////
//        homeBtn = (Button) view.findViewById(R.id.mc_homeBtn);
//        homeBtn.setOnClickListener(this);
        mcontext=view.getContext();
        pauseBtn = (Button) view.findViewById(R.id.mc_pause);
        pauseBtn.setOnClickListener(this);

        autoBtn = (Button) view.findViewById(R.id.mc_autoBtn);
        autoBtn.setOnClickListener(this);
        slidingUpPanelLayout= FlightDataFragment.getSlidingUpPaneInstance();
        fraqment_rover_control_linear= (LinearLayout) view.findViewById(R.id.fraqment_rover_control_linear);
    }

    private void updateFlightModeButtons() {
        resetFlightModeButtons();

        final Drone drone = getDrone();
        final State droneState = drone.getAttribute(AttributeType.STATE);
        final VehicleMode flightMode = droneState.getVehicleMode();
        if (flightMode != null) {
            switch (flightMode) {
                case ROVER_AUTO:
                    // autoBtn.setActivated(true);
                    //change color of auto mode icon
                    Drawable[] compoundDrawables=autoBtn.getCompoundDrawables();
                    Drawable drawableTop=compoundDrawables[1].mutate();
                    drawableTop.setColorFilter(new PorterDuffColorFilter(getResources().getColor(R.color.blue_bg), PorterDuff.Mode.SRC_IN));
                    autoBtn.setTextColor(getResources().getColor(R.color.blue_bg));
                    break;

                case ROVER_HOLD:
//                case ROVER_GUIDED:
                    pauseBtn.setActivated(true);
                    break;

//                case ROVER_RTL:
//                    homeBtn.setActivated(true);
//                    break;
            }
        }
    }

    private void resetFlightModeButtons() {
//        homeBtn.setActivated(false);
        pauseBtn.setActivated(false);
        autoBtn.setActivated(false);
        //change color of auto mode icon
        Drawable[] compoundDrawables=autoBtn.getCompoundDrawables();
        Drawable drawableTop=compoundDrawables[1].mutate();
        drawableTop.setColorFilter(new PorterDuffColorFilter(getResources().getColor(R.color.medium_grey), PorterDuff.Mode.SRC_IN));
        autoBtn.setTextColor(getResources().getColor(R.color.medium_grey));
    }

    private void setupButtonsByFlightState() {
        final State droneState = getDrone().getAttribute(AttributeType.STATE);
        if (droneState != null && droneState.isConnected()) {
            setupButtonsForConnected();
        } else {
            setupButtonsForDisconnected();
        }
    }

    private void resetButtonsContainerVisibility() {
        mDisconnectedButtons.setVisibility(View.GONE);
        mActiveButtons.setVisibility(View.GONE);
    }

    private void setupButtonsForDisconnected() {
        resetButtonsContainerVisibility();
        mDisconnectedButtons.setVisibility(View.VISIBLE);
    }

    private void setupButtonsForConnected() {
        resetButtonsContainerVisibility();
        mActiveButtons.setVisibility(View.VISIBLE);
    }

    @Override
    public void onApiConnected() {
        super.onApiConnected();
        setupButtonsByFlightState();
        updateFlightModeButtons();
        getBroadcastManager().registerReceiver(eventReceiver, intentFilter);
    }

    @Override
    public void onApiDisconnected() {
        super.onApiDisconnected();
        getBroadcastManager().unregisterReceiver(eventReceiver);
    }

    @Override
    public boolean isSlidingUpPanelEnabled(Drone drone) {
        final State droneState = drone.getAttribute(AttributeType.STATE);
        return droneState.isConnected();
    }

    @Override
    public void onClick(View v) {
        final Drone drone = getDrone();
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(GAUtils.Category.FLIGHT);
        ///////////////////////////

        ///////////////////////////
        switch (v.getId()) {
            case R.id.mc_connectBtn:

                ((SuperUI) getActivity()).toggleDroneConnection();

                break;

//            case R.id.mc_homeBtn:
//                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.ROVER_RTL);
//                eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel(VehicleMode.ROVER_RTL.getLabel());
//                break;

            case R.id.mc_pause: {
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.ROVER_HOLD);
                eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel(VehicleMode.ROVER_HOLD.getLabel());
                break;
            }

            case R.id.mc_autoBtn:
                if(slidingUpPanelLayout !=null && slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                else {
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
//                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.ROVER_AUTO);
//                eventBuilder.setAction(ACTION_FLIGHT_ACTION_BUTTON).setLabel(VehicleMode.ROVER_AUTO.getLabel());
                break;

            default:
                eventBuilder = null;
                break;
        }

        if (eventBuilder != null) {
            GAUtils.sendEvent(eventBuilder);
        }
    }
    public void changeFraqmentRoverControlBourder(Boolean enable)
    {   if(mcontext!=null)
        if(enable)
            fraqment_rover_control_linear.setBackgroundResource(R.drawable.customborder);
        else
            fraqment_rover_control_linear.setBackgroundResource(R.drawable.customborder_red);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mcontext=context;
    }

    public void setAutoModeActive(boolean state)
    {     if(mcontext!=null)
        if(state)
        {
            Drawable[] compoundDrawables=autoBtn.getCompoundDrawables();
            Drawable drawableTop=compoundDrawables[1].mutate();
            drawableTop.setColorFilter(new PorterDuffColorFilter(mcontext.getResources().getColor(R.color.blue_bg), PorterDuff.Mode.SRC_IN));
            autoBtn.setTextColor(mcontext.getResources().getColor(R.color.blue_bg));
            fraqment_rover_control_linear.setBackgroundResource(R.drawable.customborder);
        }
        else
        {
            Drawable[] compoundDrawables=autoBtn.getCompoundDrawables();
            Drawable drawableTop=compoundDrawables[1].mutate();
            drawableTop.setColorFilter(new PorterDuffColorFilter(mcontext.getResources().getColor(R.color.medium_grey), PorterDuff.Mode.SRC_IN));
            autoBtn.setTextColor(mcontext.getResources().getColor(R.color.medium_grey));

            FlightDataFragment.showWarningView();

            fraqment_rover_control_linear.setBackgroundResource(R.drawable.customborder_red);
        }
    }
}
