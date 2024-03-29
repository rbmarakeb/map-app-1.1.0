package com.map.android.fragments.helpers;

import android.app.Activity;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;

import com.map.android.proxy.mission.MissionProxy;
import com.map.android.client.Drone;

import com.map.android.DroidPlannerApp;

/**
 * Provides access to the DroidPlannerApi to its derived class.
 */
public abstract class ApiListenerListFragment extends ListFragment implements
		DroidPlannerApp.ApiListener {

	private DroidPlannerApp dpApp;
	private LocalBroadcastManager broadcastManager;

    protected MissionProxy getMissionProxy(){ return dpApp.getMissionProxy();}
	protected Drone getDrone() {
		return dpApp.getDrone();
	}

	protected LocalBroadcastManager getBroadcastManager() {
		return broadcastManager;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		dpApp = (DroidPlannerApp) activity.getApplication();
		broadcastManager = LocalBroadcastManager.getInstance(activity.getApplicationContext());
	}

	@Override
	public void onStart() {
		super.onStart();
		dpApp.addApiListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		dpApp.removeApiListener(this);
	}
}
