package com.map.android.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.map.android.graphic.map.GraphicGuided;
import com.map.android.graphic.map.GraphicWPHome;
import com.map.android.lib.drone.mission.item.POIItem;
import com.map.android.utils.prefs.DroidPlannerPrefs;
import com.map.android.client.Drone;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.CameraProxy;
import com.map.android.lib.drone.property.Gps;

import com.map.android.R;
import com.map.android.fragments.helpers.ApiListenerFragment;
import com.map.android.graphic.map.GraphicDrone;
import com.map.android.graphic.map.GraphicHome;
import com.map.android.maps.DPMap;
import com.map.android.maps.MarkerInfo;
import com.map.android.maps.providers.DPMapProvider;
import com.map.android.maps.providers.google_map.tiles.mapbox.offline.MapDownloader;
import com.map.android.proxy.mission.MissionProxy;
import com.map.android.utils.Utils;
import com.map.android.utils.prefs.AutoPanMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class DroneMap extends ApiListenerFragment {

    public static final String ACTION_UPDATE_MAP = Utils.PACKAGE_NAME + ".action.UPDATE_MAP";

	private static final IntentFilter eventFilter = new IntentFilter();
	static {
		eventFilter.addAction(MissionProxy.ACTION_MISSION_PROXY_UPDATE);
		eventFilter.addAction(MissionProxy.ACTION_POI_UPDATE);
		eventFilter.addAction(AttributeEvent.GPS_POSITION);
		eventFilter.addAction(AttributeEvent.GUIDED_POINT_UPDATED);
		eventFilter.addAction(AttributeEvent.HEARTBEAT_FIRST);
		eventFilter.addAction(AttributeEvent.HEARTBEAT_RESTORED);
		eventFilter.addAction(AttributeEvent.HEARTBEAT_TIMEOUT);
		eventFilter.addAction(AttributeEvent.STATE_CONNECTED);
		eventFilter.addAction(AttributeEvent.STATE_DISCONNECTED);
		eventFilter.addAction(AttributeEvent.CAMERA_FOOTPRINTS_UPDATED);
		eventFilter.addAction(AttributeEvent.ATTITUDE_UPDATED);
		eventFilter.addAction(AttributeEvent.HOME_UPDATED);
        eventFilter.addAction(ACTION_UPDATE_MAP);
	}

    private static final List<MarkerInfo> NO_EXTERNAL_MARKERS = Collections.emptyList();

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!isResumed())
				return;

			final String action = intent.getAction();
            switch (action) {
                case ACTION_UPDATE_MAP:
				case AttributeEvent.HOME_UPDATED:
                case MissionProxy.ACTION_MISSION_PROXY_UPDATE:
                    postUpdate();
                    break;
				case MissionProxy.ACTION_POI_UPDATE:
					postUpdatePOI();
					break;

                case AttributeEvent.GPS_POSITION: {
                    mMapFragment.updateMarker(graphicDrone);
                    mMapFragment.updateDroneLeashPath(guided);
                    final Gps droneGps = drone.getAttribute(AttributeType.GPS);
                    if (droneGps != null && droneGps.isValid()) {
                        mMapFragment.addFlightPathPoint(droneGps.getPosition());
                    }
                    break;
                }

                case AttributeEvent.GUIDED_POINT_UPDATED:
                    mMapFragment.updateMarker(guided);
                    mMapFragment.updateDroneLeashPath(guided);
                    break;

                case AttributeEvent.HEARTBEAT_FIRST:
                case AttributeEvent.HEARTBEAT_RESTORED:
				case AttributeEvent.STATE_CONNECTED:
                    mMapFragment.updateMarker(graphicDrone);
                    break;

                case AttributeEvent.STATE_DISCONNECTED:
                case AttributeEvent.HEARTBEAT_TIMEOUT:
                    mMapFragment.updateMarker(graphicDrone);
                    break;

                case AttributeEvent.CAMERA_FOOTPRINTS_UPDATED: {
					if(mAppPrefs.isRealtimeFootprintsEnabled()) {
						CameraProxy camera = drone.getAttribute(AttributeType.CAMERA);
						if (camera != null && camera.getLastFootPrint() != null)
							mMapFragment.addCameraFootprint(camera.getLastFootPrint());
					}
                    break;
                }

                case AttributeEvent.ATTITUDE_UPDATED: {
                    if (mAppPrefs.isRealtimeFootprintsEnabled()) {
                        final Gps droneGps = drone.getAttribute(AttributeType.GPS);
                        if (droneGps.isValid()) {
                            CameraProxy camera = drone.getAttribute(AttributeType.CAMERA);
                            if (camera != null && camera.getCurrentFieldOfView() != null)
                                mMapFragment.updateRealTimeFootprint(camera.getCurrentFieldOfView());
                        }

                    }
                    else{
                        mMapFragment.updateRealTimeFootprint(null);
                    }
                    break;
                }
            }
		}
	};

	private final Handler mHandler = new Handler();
	private final Handler mPOIHandler = new Handler();

	private final Runnable mUpdateMap = new Runnable() {
		@Override
		public void run() {
			if (getActivity() == null && mMapFragment == null)
				return;

			final List<MarkerInfo> missionMarkerInfos = missionProxy.getMarkersInfos();
            final List<MarkerInfo> externalMarkers = collectMarkersFromProviders();

			final boolean isThereMissionMarkers = !missionMarkerInfos.isEmpty();
            final boolean isThereExternalMarkers = !externalMarkers.isEmpty();
			final boolean isHomeValid = home.isValid();
			final boolean isGuidedVisible = guided.isVisible();

			// Get the list of markers currently on the map.
			final Set<MarkerInfo> markersOnTheMap = mMapFragment.getMarkerInfoList();
			GraphicWPHome homeMarker = null;
			for(MarkerInfo info : markersOnTheMap) {
				if(info instanceof GraphicWPHome) {
					homeMarker = (GraphicWPHome) info;
					break;
				}
			}

			if (!markersOnTheMap.isEmpty()) {
				if (isHomeValid) {
					markersOnTheMap.remove(home);
				}

				if (isGuidedVisible) {
					markersOnTheMap.remove(guided);
				}

				if (isThereMissionMarkers) {
					markersOnTheMap.removeAll(missionMarkerInfos);
				}

                if(isThereExternalMarkers)
                    markersOnTheMap.removeAll(externalMarkers);

				mMapFragment.removeMarkers(markersOnTheMap);
				if(homeMarker != null) {
					markersOnTheMap.add(homeMarker);
					mMapFragment.updateMarker(homeMarker, false);
				}
			}

			if (isHomeValid) {
				mMapFragment.updateMarker(home);
			}

			if (isGuidedVisible) {
				mMapFragment.updateMarker(guided);
			}

			if (isThereMissionMarkers) {
				mMapFragment.updateMarkers(missionMarkerInfos, isMissionDraggable());
			}

            if(isThereExternalMarkers)
                mMapFragment.updateMarkers(externalMarkers, false);

			mMapFragment.updateMissionPath(missionProxy);

			mMapFragment.updatePolygonsPaths(missionProxy.getPolygonsPath());

			mHandler.removeCallbacks(this);
		}
	};
	private final Runnable mPOIUpdateMap = new Runnable() {
		@Override
		public void run() {
			if (getActivity() == null && mMapFragment == null)
				return;

			mMapFragment.removePOIMarkers();

			final List<POIItem> poiItems = missionProxy.getPoiList();
			final boolean isTherePOIMarkers = !poiItems.isEmpty();

			if (isTherePOIMarkers && isShowPOI)
				mMapFragment.updatePOIMarkers(poiItems);

			mPOIHandler.removeCallbacks(this);
		}
	};

    private final ConcurrentLinkedQueue<MapMarkerProvider> markerProviders = new ConcurrentLinkedQueue<>();

	protected DPMap mMapFragment;

	protected DroidPlannerPrefs mAppPrefs;

	private GraphicHome home;
	public GraphicDrone graphicDrone;
	public GraphicGuided guided;

	protected MissionProxy missionProxy;
	public Drone drone;
    public boolean isShowPOI = false;

	protected Context context;

	protected abstract boolean isMissionDraggable();

	public DPMap getMapFragment(){
		return mMapFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
		final View view = inflater.inflate(R.layout.fragment_drone_map, viewGroup, false);

		updateMapFragment();

		return view;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mHandler.removeCallbacksAndMessages(null);
		mPOIHandler.removeCallbacksAndMessages(null);
	}

	@Override
	public void onApiConnected() {
		if (mMapFragment != null)
			mMapFragment.clearMarkers();

		getBroadcastManager().registerReceiver(eventReceiver, eventFilter);

		drone = getDrone();
		missionProxy = getMissionProxy();
		missionProxy.openPOIFile();

		home = new GraphicHome(drone, getContext());
		graphicDrone = new GraphicDrone(drone);
		guided = new GraphicGuided(drone);

		postUpdate();
	}

	public void downloadMapTiles(MapDownloader mapDownloader, int minimumZ, int maximumZ){
		if(mMapFragment == null)
			return;

		mMapFragment.downloadMapTiles(mapDownloader, getVisibleMapArea(), minimumZ, maximumZ);
	}

	@Override
	public void onApiDisconnected() {
		getBroadcastManager().unregisterReceiver(eventReceiver);
	}

	private void updateMapFragment() {
		// Add the map fragment instance (based on user preference)
		final DPMapProvider mapProvider = mAppPrefs.getMapProvider();

		final FragmentManager fm = getChildFragmentManager();
		mMapFragment = (DPMap) fm.findFragmentById(R.id.map_fragment_container);
		if (mMapFragment == null || mMapFragment.getProvider() != mapProvider) {
			final Bundle mapArgs = new Bundle();
			mapArgs.putInt(DPMap.EXTRA_MAX_FLIGHT_PATH_SIZE, getMaxFlightPathSize());

			mMapFragment = mapProvider.getMapFragment();
			((Fragment) mMapFragment).setArguments(mapArgs);
			fm.beginTransaction().replace(R.id.map_fragment_container, (Fragment) mMapFragment)
					.commit();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mMapFragment.saveCameraPosition();
	}

	@Override
	public void onResume() {
		super.onResume();
		mMapFragment.loadCameraPosition();
	}

	@Override
	public void onStart() {
		super.onStart();
		updateMapFragment();
	}

	@Override
	public void onStop() {
		super.onStop();
		mHandler.removeCallbacksAndMessages(null);
		mPOIHandler.removeCallbacksAndMessages(null);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity.getApplicationContext();
		mAppPrefs = DroidPlannerPrefs.getInstance(context);
	}

	public final void postUpdate() {
		mHandler.post(mUpdateMap);
	}

	public final void postUpdatePOI() {
		mPOIHandler.post(mPOIUpdateMap);
	}

	protected int getMaxFlightPathSize() {
		return 0;
	}

	/**
	 * Adds padding around the edges of the map.
	 * 
	 * @param left
	 *            the number of pixels of padding to be added on the left of the
	 *            map.
	 * @param top
	 *            the number of pixels of padding to be added on the top of the
	 *            map.
	 * @param right
	 *            the number of pixels of padding to be added on the right of
	 *            the map.
	 * @param bottom
	 *            the number of pixels of padding to be added on the bottom of
	 *            the map.
	 */
	public void setMapPadding(int left, int top, int right, int bottom) {
		mMapFragment.setMapPadding(left, top, right, bottom);
	}

	public void saveCameraPosition() {
		mMapFragment.saveCameraPosition();
	}

	public List<LatLong> projectPathIntoMap(List<LatLong> path) {
		return mMapFragment.projectPathIntoMap(path);
	}

	/**
	 * Set map panning mode on the specified target.
	 * 
	 * @param target
	 */
	public abstract boolean setAutoPanMode(AutoPanMode target);

	/**
	 * Move the map to the user location.
	 */
	public void goToMyLocation() {
		mMapFragment.goToMyLocation();
	}

	/**
	 * Move the map to the drone location.
	 */
	public void goToDroneLocation() {
		mMapFragment.goToDroneLocation();
	}

	/**
	 * Update the map rotation.
	 * 
	 * @param bearing
	 */
	public void updateMapBearing(float bearing) {
		mMapFragment.updateCameraBearing(bearing);
	}

	/**
	 * Ignore marker clicks on the map and instead report the event as a
	 * mapClick
	 * 
	 * @param skip
	 *            if it should skip further events
	 */
	public void skipMarkerClickEvents(boolean skip) {
		mMapFragment.skipMarkerClickEvents(skip);
	}

    public void addMapMarkerProvider(MapMarkerProvider provider){
        if(provider != null) {
            markerProviders.add(provider);
            postUpdate();
        }
    }

    public void removeMapMarkerProvider(MapMarkerProvider provider){
        if(provider != null) {
            markerProviders.remove(provider);
            postUpdate();
        }
    }

    public interface MapMarkerProvider {
        MarkerInfo[] getMapMarkers();
    }

    private List<MarkerInfo> collectMarkersFromProviders(){
        if(markerProviders.isEmpty())
            return NO_EXTERNAL_MARKERS;

        List<MarkerInfo> markers = new ArrayList<>();
        for(MapMarkerProvider provider : markerProviders){
            MarkerInfo[] externalMarkers = provider.getMapMarkers();
            Collections.addAll(markers, externalMarkers);
        }

        if(markers.isEmpty())
            return NO_EXTERNAL_MARKERS;

        return markers;
    }

	public DPMap.VisibleMapArea getVisibleMapArea(){
		return mMapFragment == null ? null : mMapFragment.getVisibleMapArea();
	}
}
