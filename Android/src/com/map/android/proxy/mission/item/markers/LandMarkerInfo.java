package com.map.android.proxy.mission.item.markers;

import com.map.android.proxy.mission.item.MissionItemProxy;

import com.map.android.R;

/**
 * This implements the marker source for the land mission item.
 */
class LandMarkerInfo extends MissionItemMarkerInfo {
	protected LandMarkerInfo(MissionItemProxy origin) {
		super(origin);
	}

	@Override
	protected int getSelectedIconResource() {
		return R.drawable.ic_wp_land_selected;
	}

	@Override
	protected int getIconResource() {
		return R.drawable.ic_wp_land;
	}
}
