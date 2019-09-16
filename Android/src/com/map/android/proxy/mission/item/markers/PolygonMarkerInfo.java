package com.map.android.proxy.mission.item.markers;

import com.map.android.proxy.mission.item.MissionItemProxy;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.mission.item.complex.Survey;

import com.map.android.maps.MarkerInfo;

/**
 */
public class PolygonMarkerInfo extends MarkerInfo.SimpleMarkerInfo {

	private LatLong mPoint;
	private final MissionItemProxy markerOrigin;
    private final Survey survey;
    private final int polygonIndex;

	public PolygonMarkerInfo(LatLong point, MissionItemProxy origin, Survey mSurvey, int index) {
		this.markerOrigin = origin;
		mPoint = point;
		survey = mSurvey;
		polygonIndex = index;
	}

	public Survey getSurvey(){
		return survey;
	}

	public int getIndex(){
		return polygonIndex;
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
	public com.map.android.lib.coordinate.LatLong getPosition() {
		return mPoint;
	}

	@Override
	public void setPosition(LatLong coord) {
		mPoint = coord;
	}
	
	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public boolean isFlat() {
		return true;
	}

	public MissionItemProxy getMarkerOrigin() {
		return markerOrigin;
	}
}
