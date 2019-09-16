package com.map.android.proxy.mission.item.fragments;

import com.map.android.R;

import com.map.android.lib.drone.mission.MissionItemType;

public class MissionEpmGrabberFragment extends MissionDetailFragment {

	@Override
	protected int getResource() {
		return R.layout.fragment_editor_detail_epm_grabber;
	}

	@Override
	public void onApiConnected() {
        super.onApiConnected();
		typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.EPM_GRIPPER));
	}
}
