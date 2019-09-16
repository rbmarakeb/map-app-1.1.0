package com.map.android.activities.interfaces;

import com.map.android.proxy.mission.item.MissionItemProxy;
import com.map.android.lib.coordinate.LatLong;

public interface OnEditorInteraction {
	void onItemClick(MissionItemProxy item, boolean zoomToFit);
	void onItemClickToAddPOI();

	void onMapClick(LatLong coord);

	void onListVisibilityChanged();
}
