package com.map.android.graphic.map;

import com.map.android.R;
import com.map.android.maps.MarkerInfo;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.map.android.client.Drone;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.Attitude;
import com.map.android.lib.drone.property.Gps;

public class GraphicDrone extends MarkerInfo.SimpleMarkerInfo {

	private Drone drone;

	public GraphicDrone(Drone drone) {
		this.drone = drone;
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
        Gps droneGps = drone.getAttribute(AttributeType.GPS);
        return isValid() ? droneGps.getPosition() :  null;
	}

	@Override
	public Bitmap getIcon(Resources res) {
		if (drone.isConnected()) {
			return BitmapFactory.decodeResource(res, R.drawable.quad);
		}
		return BitmapFactory.decodeResource(res, R.drawable.quad_disconnect);

	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public boolean isFlat() {
		return true;
	}

	@Override
	public float getRotation() {
        Attitude attitude = drone.getAttribute(AttributeType.ATTITUDE);
		return attitude == null ? 0 : (float) attitude.getYaw();
	}

	public boolean isValid() {
        Gps droneGps = drone.getAttribute(AttributeType.GPS);
		return droneGps != null && droneGps.isValid();
	}
}
