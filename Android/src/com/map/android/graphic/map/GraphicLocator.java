package com.map.android.graphic.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.map.android.lib.coordinate.LatLong;

import com.map.android.R;
import com.map.android.maps.MarkerInfo;

public class GraphicLocator extends MarkerInfo.SimpleMarkerInfo {

	private LatLong lastPosition;

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
		return lastPosition;
	}

	@Override
	public Bitmap getIcon(Resources res) {
		return BitmapFactory.decodeResource(res, R.drawable.quad);
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
		return 0;
	}

	public void setLastPosition(LatLong lastPosition) {
		this.lastPosition = lastPosition;
	}
}