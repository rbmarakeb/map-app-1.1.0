package com.map.android.maps;

import android.graphics.Color;
import android.location.LocationListener;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;
import com.map.android.lib.drone.mission.item.POIItem;
import com.map.android.maps.providers.DPMapProvider;
import com.map.android.maps.providers.google_map.tiles.mapbox.offline.MapDownloader;
import com.map.android.utils.prefs.AutoPanMode;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.property.FootPrint;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Defines the functionality expected from the map providers.
 */
public interface DPMap {

	String PACKAGE_NAME = DPMap.class.getPackage().getName();

	String EXTRA_MAX_FLIGHT_PATH_SIZE = PACKAGE_NAME + ""
			+ ".EXTRA_MAX_FLIGHT_PATH_SIZE";

	int FLIGHT_PATH_DEFAULT_COLOR = 0xfffd693f;
	int FLIGHT_PATH_DEFAULT_WIDTH = 12;

	int MISSION_PATH_DEFAULT_COLOR = Color.WHITE;
	int MISSION_PATH_DEFAULT_WIDTH = 12;

	int DRONE_LEASH_DEFAULT_COLOR = Color.WHITE;
	int DRONE_LEASH_DEFAULT_WIDTH = 12;

	int POLYGONS_PATH_DEFAULT_COLOR = Color.RED;
	int POLYGONS_PATH_DEFAULT_WIDTH = 12;
	
	int FOOTPRINT_DEFAULT_COLOR = 0;
	int FOOTPRINT_DEFAULT_WIDTH = 2;
	int FOOTPRINT_FILL_COLOR = Color.argb(80, 0, 0, 200);
	
	String PREF_LAT = "pref_map_lat";
	float DEFAULT_LATITUDE = 37.8575523f;

	String PREF_LNG = "pref_map_lng";
	float DEFAULT_LONGITUDE = -122.292767f;

	String PREF_BEA = "pref_map_bea";
	int DEFAULT_BEARING = 0;

	String PREF_TILT = "pref_map_tilt";
	int DEFAULT_TILT = 0;

	String PREF_ZOOM = "pref_map_zoom";
	int DEFAULT_ZOOM_LEVEL = 17;

	interface PathSource {
		List<LatLong> getPathPoints();
	}

	/**
	 * Implemented by classes interested in map click events.
	 */
	interface OnMapClickListener {
		/**
		 * Triggered when the map is clicked.
		 * 
		 * @param coord
		 *            location where the map was clicked.
		 */
		void onMapClick(LatLong coord);
	}

	/**
	 * Implemented by classes interested in map long click events.
	 */
	interface OnMapLongClickListener {
		/**
		 * Triggered when the map is long clicked.
		 * 
		 * @param coord
		 *            location where the map was long clicked.
		 */
		void onMapLongClick(LatLong coord);
	}

	/**
	 * Implemented by classes interested in marker(s) click events.
	 */
	interface OnMarkerClickListener {
		/**
		 * Triggered when a marker is clicked.
		 * 
		 * @param markerInfo
		 *            info about the clicked marker
		 * @return true if the listener has consumed the event.
		 */
		boolean onMarkerClick(MarkerInfo markerInfo);
	}

	/**
	 * Callback interface for drag events on markers.
	 */
	interface OnMarkerDragListener {
		/**
		 * Called repeatedly while a marker is being dragged. The marker's
		 * location can be accessed via {@link MarkerInfo#getPosition()}
		 * 
		 * @param markerInfo
		 *            info about the marker that was dragged.
		 */
		void onMarkerDrag(MarkerInfo markerInfo);

		/**
		 * Called when a marker has finished being dragged. The marker's
		 * location can be accessed via {@link MarkerInfo#getPosition()}
		 * 
		 * @param markerInfo
		 *            info about the marker that was dragged.
		 */
		void onMarkerDragEnd(MarkerInfo markerInfo);

		/**
		 * Called when a marker starts being dragged. The marker's location can
		 * be accessed via {@link MarkerInfo#getPosition()}; this position may
		 * be different to the position prior to the start of the drag because
		 * the marker is popped up above the touch point.
		 * 
		 * @param markerInfo
		 *            info about the marker that was dragged.
		 */
		void onMarkerDragStart(MarkerInfo markerInfo);

	}

	class VisibleMapArea implements Parcelable {
		public final LatLong nearLeft;
		public final LatLong nearRight;
		public final LatLong farLeft;
		public final LatLong farRight;

		public VisibleMapArea(LatLong farLeft, LatLong nearLeft, LatLong nearRight, LatLong farRight) {
			this.farLeft = farLeft;
			this.nearLeft = nearLeft;
			this.nearRight = nearRight;
			this.farRight = farRight;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeParcelable(this.nearLeft, 0);
			dest.writeParcelable(this.nearRight, 0);
			dest.writeParcelable(this.farLeft, 0);
			dest.writeParcelable(this.farRight, 0);
		}

		protected VisibleMapArea(Parcel in) {
			this.nearLeft = in.readParcelable(LatLong.class.getClassLoader());
			this.nearRight = in.readParcelable(LatLong.class.getClassLoader());
			this.farLeft = in.readParcelable(LatLong.class.getClassLoader());
			this.farRight = in.readParcelable(LatLong.class.getClassLoader());
		}

		public static final Parcelable.Creator<VisibleMapArea> CREATOR = new Parcelable.Creator<VisibleMapArea>() {
			public VisibleMapArea createFromParcel(Parcel source) {
				return new VisibleMapArea(source);
			}

			public VisibleMapArea[] newArray(int size) {
				return new VisibleMapArea[size];
			}
		};
	}

	/**
	 * Adds a coordinate to the drone's flight path.
	 * 
	 * @param coord
	 *            drone's coordinate
	 */
	void addFlightPathPoint(LatLong coord);

	/**
	 * Draw the footprint of the camera in the ground
	 * @param footprintToBeDraw
	 */
	void addCameraFootprint(FootPrint footprintToBeDraw);

	/**
	 * Remove all markers from the map.
	 */
	void clearMarkers();

	/**
	 * Clears the drone's flight path.
	 */
	void clearFlightPath();

	/**
	 * Download the map tiles if supported
	 * @param mapDownloader
	 * @param mapRegion
	 * @param minimumZ
	 * @param maximumZ
     */
	void downloadMapTiles(MapDownloader mapDownloader, DPMap.VisibleMapArea mapRegion, int minimumZ, int maximumZ);

	/**
	 * @return the map center coordinates.
	 */
	LatLong getMapCenter();

	LatLong getMyCoordinate();

	/**
	 * @return the map current zoom level.
	 */
	float getMapZoomLevel();

	/**
	 * @return a list of marker info currently on the map.
	 */
	Set<MarkerInfo> getMarkerInfoList();

	/**
	 * @return the map maximum zoom level.
	 */
	float getMaxZoomLevel();

	/**
	 * @return the map minimum zoom level.
	 */
	float getMinZoomLevel();

	/**
	 * @return this map's provider.
	 */
	DPMapProvider getProvider();

	/**
	 * @return the bounds of the map area visible on screen.
	 */
	VisibleMapArea getVisibleMapArea();

	/**
	 * Move the map to the drone location.
	 */
	void goToDroneLocation();

	/**
	 * Move the map to the user location.
	 */
	void goToMyLocation();

	/**
	 * Move camera to zoom in /out the  map.
	 */
	void onZoom(View view);
	/**
	 * Updates the map's center, and zoom level.
	 *
	 * @param coord
	 * location for the map center
	 */
	void updateCamera(LatLong coord);
	/**
	 * Restores the map's camera settings from preferences.
	 */
	void loadCameraPosition();

	List<LatLong> projectPathIntoMap(List<LatLong> pathPoints);

	/**
	 * Remove the markers whose info is in the list from the map.
	 * 
	 * @param markerInfoList
	 *            list of markers to remove.
	 */
	void removeMarkers(Collection<MarkerInfo> markerInfoList);

	/**
	 * Stores the map camera settings.
	 */
	void saveCameraPosition();

	/**
	 * Enable map auto panning on the passed target type.
	 * 
	 * @param mode
	 *            auto pan target (user / drone / disabled).
	 */
	void selectAutoPanMode(AutoPanMode mode);

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
	void setMapPadding(int left, int top, int right, int bottom);

	/**
	 * Sets a callback that's invoked when the map is tapped.
	 * 
	 * @param listener
	 *            The callback that's invoked when the map is tapped. To unset
	 *            the callback, use null.
	 */
	void setOnMapClickListener(OnMapClickListener listener);

	/**
	 * Sets a callback that's invoked when the map is long pressed.
	 * 
	 * @param listener
	 *            The callback that's invoked when the map is long pressed. To
	 *            unset the callback, use null.
	 */
	void setOnMapLongClickListener(OnMapLongClickListener listener);

	/**
	 * Sets a callback that's invoked when a marker is clicked.
	 * 
	 * @param listener
	 *            The callback that's invoked when a marker is clicked. To unset
	 *            the callback, use null.
	 */
	void setOnMarkerClickListener(OnMarkerClickListener listener);

	/**
	 * Sets a callback that's invoked when a marker is dragged.
	 * 
	 * @param listener
	 *            The callback that's invoked on marker drag events. To unset
	 *            the callback, use null.
	 */
	void setOnMarkerDragListener(OnMarkerDragListener listener);

    /**
     * Sets a callback that's invoked when the user location is updated.
     * @param listener
     */
	void setLocationListener(LocationListener listener);

	/**
	 * Updates the map's center, and zoom level.
	 * 
	 * @param coord
	 *            location for the map center
	 * @param zoomLevel
	 *            zoom level for the map
	 */
	void updateCamera(LatLong coord, float zoomLevel);

    /**
     * Updates the map's bearing.
     * @param bearing direction that the camera is pointing in.
     */
	void updateCameraBearing(float bearing);

	/**
	 * Updates the drone leash path on the map.
	 * 
	 * @param pathSource
	 *            source to use to generate the drone leash path.
	 */
	void updateDroneLeashPath(PathSource pathSource);

	/**
	 * Adds / updates the marker corresponding to the given marker info
	 * argument.
	 * 
	 * @param markerInfo
	 *            used to generate / update the marker
	 */
	void updateMarker(MarkerInfo markerInfo);

	/**
	 * Adds / updates the marker corresponding to the given marker info
	 * argument.
	 *
	 * @param markerInfo
	 *            used to generate / update the marker
	 * @param title
	 *            title for the marker
	 */
	void updateNextMarkerSetGreenIcon(MarkerInfo markerInfo,String title);

	/**
	 * Adds / updates the marker corresponding to the given marker info
	 * argument.
	 * 
	 * @param markerInfo
	 *            used to generate / update the marker
	 * @param isDraggable
	 *            overwrites markerInfo draggable preference
	 */
	void updateMarker(MarkerInfo markerInfo, boolean isDraggable);

	/**
	 * Adds / updates the markers corresponding to the given list of markers
	 * infos.
	 * 
	 * @param markersInfos
	 *            source for the new markers to add/update
	 */
	void updateMarkers(List<MarkerInfo> markersInfos);

	/**
	 * Adds / updates the markers corresponding to the given list of markers
	 * infos.
	 * 
	 * @param markersInfos
	 *            source for the new markers to add/update
	 * @param isDraggable
	 *            overwrites markerInfo draggable preference
	 */
	void updateMarkers(List<MarkerInfo> markersInfos, boolean isDraggable);

	void removePOIMarkers();
	void updatePOIMarkers(List<POIItem> poiItems);

	/**
	 * Updates the mission path on the map.
	 * 
	 * @param pathSource
	 *            source to use to draw the mission path
	 */
	void updateMissionPath(PathSource pathSource);
	
	/**
	 * Updates the polygons on the map.
	 * 
	 */
	void updatePolygonsPaths(List<List<LatLong>> paths);

	/**
	 * Zoom to fit coordinates on map
	 * 
	 * @param coords
	 *            to be displayed
	 */
	void zoomToFit(List<LatLong> coords);

    /**
     * Zoom to fit my location and the given coordinates on map
     * @param coords
     */
	void zoomToFitMyLocation(List<LatLong> coords);
    
    /**
     * Ignore marker clicks on the map and instead report the event as a mapClick
     * @param skip if it should skip further events
     */
	void skipMarkerClickEvents(boolean skip);

	void updateRealTimeFootprint(FootPrint footprint);
    
}
