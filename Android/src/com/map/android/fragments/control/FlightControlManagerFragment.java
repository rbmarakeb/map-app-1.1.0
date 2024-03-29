package com.map.android.fragments.control;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.map.android.fragments.RemoteHelmDataFragment;
import com.map.android.fragments.helpers.ApiListenerFragment;
import com.map.android.client.Drone;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.Type;

import com.map.android.R;
import com.map.android.fragments.FlightDataFragment;

public class FlightControlManagerFragment extends ApiListenerFragment {

	private static final String EXTRA_LAST_VEHICLE_TYPE = "extra_last_vehicle_type";
	private static final int DEFAULT_LAST_VEHICLE_TYPE = Type.TYPE_UNKNOWN;

	public interface SlidingUpHeader {
		boolean isSlidingUpPanelEnabled(Drone drone);
	}

	private static final IntentFilter eventFilter = new IntentFilter();
    static {
        eventFilter.addAction(AttributeEvent.TYPE_UPDATED);
        eventFilter.addAction(AttributeEvent.STATE_CONNECTED);
    }

	private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
            switch (action) {
                case AttributeEvent.STATE_CONNECTED:
                case AttributeEvent.TYPE_UPDATED:
                    Type type = getDrone().getAttribute(AttributeType.TYPE);
                    selectActionsBar(type.getDroneType());
                    break;
            }
		}
	};

	private int droneType;
	static Fragment actionsBarFragment;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);

		final Fragment parent = getParentFragment();
		if(!(parent instanceof FlightDataFragment) && !(parent instanceof RemoteHelmDataFragment)){
			throw new IllegalStateException("Parent must be an instance of " + FlightDataFragment.class
					.getName());
		}
	}
	//this function used to access to Fragment(RoverFlightControlFragment) Instance
	public static Fragment getactionsBarFragmentInstance(){
		return  actionsBarFragment;
	}

	private SlidingUpHeader header;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_flight_actions_bar, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);

		this.droneType = savedInstanceState == null
				? DEFAULT_LAST_VEHICLE_TYPE
				: savedInstanceState.getInt(EXTRA_LAST_VEHICLE_TYPE, DEFAULT_LAST_VEHICLE_TYPE);
		selectActionsBar(this.droneType, true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt(EXTRA_LAST_VEHICLE_TYPE, droneType);
	}

	@Override
	public void onApiConnected() {
        Drone drone = getDrone();
        if(drone.isConnected()) {
            Type type = getDrone().getAttribute(AttributeType.TYPE);
            selectActionsBar(type.getDroneType());
        }
        else{
            selectActionsBar(Type.TYPE_UNKNOWN);
        }
		getBroadcastManager().registerReceiver(eventReceiver, eventFilter);
	}

	@Override
	public void onApiDisconnected() {
		getBroadcastManager().unregisterReceiver(eventReceiver);
        if(isResumed())
            selectActionsBar(Type.TYPE_UNKNOWN);
	}

	private void selectActionsBar(int droneType){
		selectActionsBar(droneType, false);
	}

	private void selectActionsBar(int droneType, boolean force) {
		if(this.droneType == droneType && !force)
			return;

		this.droneType = droneType;

		final FragmentManager fm = getChildFragmentManager();

		actionsBarFragment = null;
		switch (droneType) {
			case Type.TYPE_COPTER:
				actionsBarFragment = new CopterFlightControlFragment();
				break;

			case Type.TYPE_PLANE:
				actionsBarFragment = new PlaneFlightControlFragment();
				break;

			case Type.TYPE_ROVER:
				actionsBarFragment = new RoverFlightControlFragment();
				fm.beginTransaction().replace(R.id.flight_actions_bar, actionsBarFragment).commitAllowingStateLoss();
				break;

			case Type.TYPE_UNKNOWN:
			default:
				if(getParentFragment() instanceof FlightDataFragment)
				actionsBarFragment = new GenericActionsFragment();
				break;
		}

		if(getParentFragment() instanceof FlightDataFragment) {
			fm.beginTransaction().replace(R.id.flight_actions_bar, actionsBarFragment).commitAllowingStateLoss();
			header = (SlidingUpHeader) actionsBarFragment;
		}
	}

	public boolean isSlidingUpPanelEnabled(Drone api) {
		Type type = api.getAttribute(AttributeType.TYPE);
		selectActionsBar(type.getDroneType());
		return header != null && header.isSlidingUpPanelEnabled(api);
	}

	public void updateMapBearing(float bearing) {
		if(getParentFragment() instanceof FlightDataFragment) {
			final FlightDataFragment parent = (FlightDataFragment) getParentFragment();
			if (parent != null) {
				parent.updateMapBearing(bearing);
			}
		} else if(getParentFragment() instanceof RemoteHelmDataFragment) {
			final RemoteHelmDataFragment parent = (RemoteHelmDataFragment) getParentFragment();
			if (parent != null) {
				parent.updateMapBearing(bearing);
			}
		}
	}
}
