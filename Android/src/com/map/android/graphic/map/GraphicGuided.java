package com.map.android.graphic.map;

import java.util.ArrayList;
import java.util.List;

import com.map.android.R;
import com.map.android.maps.DPMap.PathSource;
import com.map.android.maps.MarkerInfo;
import com.map.android.maps.MarkerWithText;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import com.map.android.client.Drone;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.Gps;
import com.map.android.lib.drone.property.GuidedState;

public class GraphicGuided extends MarkerInfo.SimpleMarkerInfo implements PathSource {

	private final static String TAG = GraphicGuided.class.getSimpleName();

    private final Drone drone;

	public GraphicGuided(Drone drone) {
        this.drone = drone;
	}

	@Override
	public List<LatLong> getPathPoints() {
		List<LatLong> path = new ArrayList<LatLong>();
        GuidedState guidedPoint = drone.getAttribute(AttributeType.GUIDED_STATE);
		if (guidedPoint != null && guidedPoint.isActive()) {
            Gps gps = drone.getAttribute(AttributeType.GPS);
			if (gps != null && gps.isValid()) {
				path.add(gps.getPosition());
			}
			path.add(guidedPoint.getCoordinate());
		}
		return path;
	}

	@Override
	public boolean isVisible() {
        GuidedState guidedPoint = drone.getAttribute(AttributeType.GUIDED_STATE);
		return guidedPoint != null && guidedPoint.isActive();
	}

	@Override
	public float getAnchorU() {
		return 0.5f;
	}

	@Override
	public float getAnchorV() {
		return 0.5f;
	}

	@Override
	public LatLong getPosition() {
        GuidedState guidedPoint = drone.getAttribute(AttributeType.GUIDED_STATE);
		return guidedPoint == null ? null : guidedPoint.getCoordinate();
	}

	@Override
	public void setPosition(LatLong coord) {
		try {
			drone.sendGuidedPoint(coord, true);
		} catch (Exception e) {
			Log.e(TAG, "Unable to update guided point position.", e);
		}
	}

	@Override
	public Bitmap getIcon(Resources res) {
		return MarkerWithText.getMarkerWithTextAndDetail(R.drawable.ic_wp_map, "Guided", "", res);
	}

	@Override
	public boolean isDraggable() {
		return true;
	}
}