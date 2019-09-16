package com.map.android.proxy.mission.item.markers;

import com.map.android.proxy.mission.item.MissionItemProxy;

import com.map.android.R;

/**
 * This implements the marker source for a takeoff mission item.
 */
class TakeoffMarkerInfo extends MissionItemMarkerInfo {

	protected TakeoffMarkerInfo(MissionItemProxy origin) {
		super(origin);
	}

	@Override
	protected int getSelectedIconResource() {
		return R.drawable.ic_wp_takeof_selected;
	}

	@Override
	protected int getIconResource() {
		return R.drawable.ic_wp_takeof_selected;
	}
}
