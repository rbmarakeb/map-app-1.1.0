package com.map.android.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.widget.Toast;

import com.map.android.R;
import com.map.android.client.Drone;
import com.map.android.client.apis.MissionApi;
import com.map.android.fragments.SettingsFragment;
import com.map.android.proxy.mission.MissionProxy;

/**
 * This activity holds the SettingsFragment.
 */
public class SettingsActivity extends DrawerNavigationUI {

	static  private Fragment settingsFragment;
	//this function used to access to SettingsFragment Instance
	public static SettingsFragment getSettingsFragmentInstance(){
		return (SettingsFragment) settingsFragment;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		FragmentManager fm = getFragmentManager();

		settingsFragment = fm.findFragmentById(R.id.fragment_settings_layout);
		if (settingsFragment == null) {
			settingsFragment = new SettingsFragment();
			fm.beginTransaction().add(R.id.fragment_settings_layout, settingsFragment).commit();
		}
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
					if(!missionProxy.getItems().isEmpty())
						missionProxy.sendMissionToAPM(drone);
					else
						Toast.makeText(context,"There is't any waypoint to sent",Toast.LENGTH_LONG).show();
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
	protected int getToolbarId() {
		return R.id.actionbar_toolbar;
	}

	@Override
	protected int getNavigationDrawerMenuItemId() {
		return R.id.navigation_settings;
	}

	@Override
	public void onApiConnected() {
		super.onApiConnected();
	}
}
