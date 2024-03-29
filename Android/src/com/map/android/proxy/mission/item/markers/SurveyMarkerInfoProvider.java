package com.map.android.proxy.mission.item.markers;

import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.mission.item.complex.Survey;

import java.util.ArrayList;
import java.util.List;

import com.map.android.maps.MarkerInfo;
import com.map.android.proxy.mission.item.MissionItemProxy;

/**
 *
 */
public class SurveyMarkerInfoProvider {

	private final Survey mSurvey;
	protected final MissionItemProxy markerOrigin;
	private final List<MarkerInfo> mPolygonMarkers = new ArrayList<MarkerInfo>();

	protected SurveyMarkerInfoProvider(MissionItemProxy origin) {
		this.markerOrigin = origin;
		mSurvey = (Survey) origin.getMissionItem();
		updateMarkerInfoList();
	}

	private void updateMarkerInfoList() {
        List<LatLong> points = mSurvey.getPolygonPoints();
        if(points != null) {
            final int pointsCount = points.size();
            for (int i = 0; i < pointsCount; i++) {
                mPolygonMarkers.add(new PolygonMarkerInfo(points.get(i), markerOrigin, mSurvey, i));
            }
        }
	}

	public List<MarkerInfo> getMarkersInfos() {
		return mPolygonMarkers;
	}

	public MissionItemProxy getMarkerOrigin() {
		return markerOrigin;
	}
}
