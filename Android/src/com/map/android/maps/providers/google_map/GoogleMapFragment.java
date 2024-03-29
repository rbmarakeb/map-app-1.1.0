package com.map.android.maps.providers.google_map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.map.android.graphic.map.GraphicWPHome;
import com.map.android.lib.drone.mission.item.POIItem;
import com.map.android.maps.MarkerWithText;
import com.map.android.utils.DroneHelper;
import com.map.android.utils.prefs.DroidPlannerPrefs;
import com.map.android.client.Drone;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.FootPrint;
import com.map.android.lib.drone.property.Gps;
import com.map.android.lib.util.googleApi.GoogleApiClientManager;
import com.map.android.lib.util.googleApi.GoogleApiClientManager.GoogleApiClientTask;

import com.map.android.DroidPlannerApp;
import com.map.android.R;
import com.map.android.fragments.SettingsFragment;
import com.map.android.graphic.map.GraphicHome;
import com.map.android.maps.DPMap;
import com.map.android.maps.MarkerInfo;
import com.map.android.maps.providers.DPMapProvider;
import com.map.android.maps.providers.google_map.tiles.TileProviderManager;
import com.map.android.maps.providers.google_map.tiles.arcgis.ArcGISTileProviderManager;
import com.map.android.maps.providers.google_map.tiles.mapbox.MapboxTileProviderManager;
import com.map.android.maps.providers.google_map.tiles.mapbox.MapboxUtils;
import com.map.android.maps.providers.google_map.tiles.mapbox.offline.MapDownloader;
import com.map.android.utils.collection.HashBiMap;
import com.map.android.utils.prefs.AutoPanMode;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

public class GoogleMapFragment extends SupportMapFragment implements DPMap, GoogleApiClientManager.ManagerListener {

    private static final long USER_LOCATION_UPDATE_INTERVAL = 30000; // ms
    private static final long USER_LOCATION_UPDATE_FASTEST_INTERVAL = 5000; // ms
    private static final float USER_LOCATION_UPDATE_MIN_DISPLACEMENT = 0; // m

    private static final float GO_TO_MY_LOCATION_ZOOM = 17f;

    private static final int ONLINE_TILE_PROVIDER_Z_INDEX = -1;
    private static final int OFFLINE_TILE_PROVIDER_Z_INDEX = -2;

    private static final IntentFilter eventFilter = new IntentFilter();

    static {
        eventFilter.addAction(AttributeEvent.GPS_POSITION);
        eventFilter.addAction(SettingsFragment.ACTION_MAP_ROTATION_PREFERENCE_UPDATED);
    }

    private GoogleMap mMap;
    private final static Api<? extends Api.ApiOptions.NotRequiredOptions>[] apisList = new Api[]{LocationServices.API};

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case AttributeEvent.GPS_POSITION:
                    if (mPanMode.get() == AutoPanMode.DRONE) {
                        final Drone drone = getDroneApi();
                        if (!drone.isConnected())
                            return;

                        final Gps droneGps = drone.getAttribute(AttributeType.GPS);
                        if (droneGps != null && droneGps.isValid()) {
                            final LatLong droneLocation = droneGps.getPosition();
                            updateCamera(droneLocation);
                        }
                    }
                    break;

                case SettingsFragment.ACTION_MAP_ROTATION_PREFERENCE_UPDATED:
                    getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            setupMapUI(googleMap);
                        }
                    });
                    break;
            }
        }
    };

    private final HashBiMap<MarkerInfo, Marker> mBiMarkersMap = new HashBiMap<MarkerInfo, Marker>();
    private final List<Marker> mPOIMarkerList = new ArrayList<>();

    private DroidPlannerPrefs mAppPrefs;

    private final AtomicReference<AutoPanMode> mPanMode = new AtomicReference<AutoPanMode>(
            AutoPanMode.DISABLED);

    private final Handler handler = new Handler();

    private final LocationCallback locationCb = new LocationCallback() {
        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
        }

        @Override
        public void onLocationResult(LocationResult result) {
            super.onLocationResult(result);

            final Location location = result.getLastLocation();
            if (location == null)
                return;

            //Update the user location icon.
            if (userMarker == null) {
                boolean visible_my_location_icon=false;
                final Drone drone = getDroneApi();
                if (drone.isConnected())
                    visible_my_location_icon=false;
                else
                    visible_my_location_icon=true;

                final MarkerOptions options = new MarkerOptions()
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .draggable(false)
                        .flat(true)
                        .visible(visible_my_location_icon)
                        .anchor(0.5f, 0.5f);
                if(mAppPrefs.getHomeMarkerEnabled()) {
                   // options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home));
                    if (!drone.isConnected())
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location));
                } else {
                    if (!drone.isConnected())
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location));
                }

                getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        userMarker = googleMap.addMarker(options);
                    }
                });
            } else {
                if(mAppPrefs.getHomeMarkerEnabled()) {
                  //  userMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home));
                }
                userMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
            }

            if (mPanMode.get() == AutoPanMode.USER) {
                Timber.d("User location changed.");
                updateCamera(DroneHelper.LocationToCoord(location), (int) mMap.getCameraPosition().zoom);
            }

            if (mLocationListener != null) {
                mLocationListener.onLocationChanged(location);
            }
        }
    };

    private final GoogleApiClientTask mGoToMyLocationTask = new GoogleApiClientTask() {
        @Override
        public void doRun() {
            final Location myLocation = LocationServices.FusedLocationApi.getLastLocation(getGoogleApiClient());
            if (myLocation != null) {
                updateCamera(DroneHelper.LocationToCoord(myLocation), GO_TO_MY_LOCATION_ZOOM);

                if (mLocationListener != null)
                    mLocationListener.onLocationChanged(myLocation);
            }
        }
    };

    private final GoogleApiClientTask mRemoveLocationUpdateTask = new GoogleApiClientTask() {
        @Override
        public void doRun() {
            LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleApiClient(), locationCb);
        }
    };

    private final GoogleApiClientTask mRequestLocationUpdateTask = new GoogleApiClientTask() {
        @Override
        public void doRun() {
            final LocationRequest locationReq = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setFastestInterval(USER_LOCATION_UPDATE_FASTEST_INTERVAL)
                    .setInterval(USER_LOCATION_UPDATE_INTERVAL)
                    .setSmallestDisplacement(USER_LOCATION_UPDATE_MIN_DISPLACEMENT);

            LocationServices.FusedLocationApi.requestLocationUpdates(getGoogleApiClient(), locationReq,
                    locationCb, handler.getLooper());
        }
    };

    private final GoogleApiClientTask requestLastLocationTask = new GoogleApiClientTask() {
        @Override
        protected void doRun() {
            final Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(getGoogleApiClient());
            if (lastLocation != null && mLocationListener != null) {
                mLocationListener.onLocationChanged(lastLocation);
            }
        }
    };

    private GoogleApiClientManager mGApiClientMgr;

    private Marker userMarker;

    private Polyline flightPath;
    private Polyline missionPath;
    private Polyline mDroneLeashPath;
    private int maxFlightPathSize;

    /*
     * DP Map listeners
     */
    private DPMap.OnMapClickListener mMapClickListener;
    private DPMap.OnMapLongClickListener mMapLongClickListener;
    private DPMap.OnMarkerClickListener mMarkerClickListener;
    private DPMap.OnMarkerDragListener mMarkerDragListener;
    private android.location.LocationListener mLocationListener;

    private LatLong mMyCoordinate;

    protected boolean useMarkerClickAsMapClick = false;

    private List<Polygon> polygonsPaths = new ArrayList<Polygon>();

    protected DroidPlannerApp dpApp;
    private Polygon footprintPoly;

    /*
    Tile overlay
     */
    private TileOverlay onlineTileOverlay;
    private TileOverlay offlineTileOverlay;

    private TileProviderManager tileProviderManager;

    private final OnMapReadyCallback loadCameraPositionTask = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            final SharedPreferences settings = mAppPrefs.prefs;

            final CameraPosition.Builder camera = new CameraPosition.Builder();
            camera.bearing(settings.getFloat(PREF_BEA, DEFAULT_BEARING));
            camera.tilt(settings.getFloat(PREF_TILT, DEFAULT_TILT));
            camera.zoom(settings.getFloat(PREF_ZOOM, DEFAULT_ZOOM_LEVEL));
            camera.target(new LatLng(settings.getFloat(PREF_LAT, DEFAULT_LATITUDE),
                    settings.getFloat(PREF_LNG, DEFAULT_LONGITUDE)));

            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera.build()));
        }
    };

    private final OnMapReadyCallback setupMapTask = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            setupMapUI(googleMap);
            setupMapOverlay(googleMap);
            setupMapListeners(googleMap);
        }
    };

    private LocalBroadcastManager lbm;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dpApp = (DroidPlannerApp) activity.getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        setHasOptionsMenu(true);

        final FragmentActivity activity = getActivity();
        final Context context = activity.getApplicationContext();

        lbm = LocalBroadcastManager.getInstance(context);

        final View view = super.onCreateView(inflater, viewGroup, bundle);

        mGApiClientMgr = new GoogleApiClientManager(context, new Handler(), apisList);
        mGApiClientMgr.setManagerListener(this);

        mAppPrefs = DroidPlannerPrefs.getInstance(context);

        final Bundle args = getArguments();
        if (args != null) {
            maxFlightPathSize = args.getInt(EXTRA_MAX_FLIGHT_PATH_SIZE);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mGApiClientMgr.start();

        mGApiClientMgr.addTask(mRequestLocationUpdateTask);
        lbm.registerReceiver(eventReceiver, eventFilter);
        setupMap();
    }

    @Override
    public void onStop() {
        super.onStop();

        mGApiClientMgr.addTask(mRemoveLocationUpdateTask);
        lbm.unregisterReceiver(eventReceiver);

        mGApiClientMgr.stopSafely();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_google_map, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_download_mapbox_map:
                startActivity(new Intent(getContext(), DownloadMapboxMapActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu){
        final MenuItem item = menu.findItem(R.id.menu_download_mapbox_map);
        if(item != null) {
            final boolean isEnabled = shouldShowDownloadMapMenuOption();
            item.setEnabled(isEnabled);
            item.setVisible(isEnabled);
        }

    }

    private boolean shouldShowDownloadMapMenuOption(){
        final Context context = getContext();
        final @GoogleMapPrefConstants.TileProvider String tileProvider = GoogleMapPrefFragment.PrefManager
                .getMapTileProvider(context);

        return (GoogleMapPrefConstants.MAPBOX_TILE_PROVIDER.equals(tileProvider)
            || GoogleMapPrefConstants.ARC_GIS_TILE_PROVIDER.equals(tileProvider))
                && GoogleMapPrefFragment.PrefManager.addDownloadMenuOption(context);
    }

    @Override
    public void clearFlightPath() {
        if (flightPath != null) {
            List<LatLng> oldFlightPath = flightPath.getPoints();
            oldFlightPath.clear();
            flightPath.setPoints(oldFlightPath);
        }
    }

    @Override
    public void downloadMapTiles(MapDownloader mapDownloader, VisibleMapArea mapRegion, int minimumZ, int maximumZ) {
        if(tileProviderManager == null)
            return;

        tileProviderManager.downloadMapTiles(mapDownloader, mapRegion, minimumZ, maximumZ);
    }

    @Override
    public LatLong getMapCenter() {
        return DroneHelper.LatLngToCoord(mMap.getCameraPosition().target);
    }

    @Override
    public LatLong getMyCoordinate() {
        if(mMyCoordinate != null) return mMyCoordinate;
        final Location myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoToMyLocationTask.getGoogleApiClient());
        if(myLocation == null) return null;
        mMyCoordinate = new LatLong(myLocation.getLatitude(), myLocation.getLongitude());
        return mMyCoordinate;
    }

    @Override
    public float getMapZoomLevel() {
        return mMap.getCameraPosition().zoom;
    }

    @Override
    public float getMaxZoomLevel() {
        return mMap.getMaxZoomLevel();
    }

    @Override
    public float getMinZoomLevel() {
        return mMap.getMinZoomLevel();
    }

    @Override
    public void selectAutoPanMode(AutoPanMode target) {
        final AutoPanMode currentMode = mPanMode.get();
        if (currentMode == target)
            return;

        setAutoPanMode(currentMode, target);
    }

    private Drone getDroneApi() {
        return dpApp.getDrone();
    }

    private void setAutoPanMode(AutoPanMode current, AutoPanMode update) {
        mPanMode.compareAndSet(current, update);
    }

    @Override
    public DPMapProvider getProvider() {
        return DPMapProvider.GOOGLE_MAP;
    }

    @Override
    public void addFlightPathPoint(LatLong coord) {
        final LatLng position = DroneHelper.CoordToLatLang(coord);

        if (maxFlightPathSize > 0) {
            if (flightPath == null) {
                PolylineOptions flightPathOptions = new PolylineOptions();
                flightPathOptions.color(Color.parseColor("#e33d1b"))
                        .width(FLIGHT_PATH_DEFAULT_WIDTH).zIndex(1);
                flightPath = mMap.addPolyline(flightPathOptions);
            }

            List<LatLng> oldFlightPath = flightPath.getPoints();
            if (oldFlightPath.size() > maxFlightPathSize) {
                oldFlightPath.remove(0);
            }
            oldFlightPath.add(position);
            flightPath.setPoints(oldFlightPath);
        }
    }

    @Override
    public void clearMarkers() {
        for (Marker marker : mBiMarkersMap.valueSet()) {
            marker.remove();
        }

        mBiMarkersMap.clear();
    }

    @Override
    public void updateMarker(MarkerInfo markerInfo) {
        updateMarker(markerInfo, markerInfo.isDraggable());
    }

    @Override
    public void updateMarker(MarkerInfo markerInfo, boolean isDraggable) {
        // if the drone hasn't received a gps signal yet
        final LatLong coord = markerInfo.getPosition();
        if (coord == null) {
            return;
        }

        final LatLng position = DroneHelper.CoordToLatLang(coord);
        Marker marker = mBiMarkersMap.getValue(markerInfo);
        if (marker == null) {
            // Generate the marker
            generateMarker(markerInfo, position, isDraggable);
        } else {
            // Update the marker
            updateMarker(marker, markerInfo, position, isDraggable);
        }
    }

    private void generateMarker(MarkerInfo markerInfo, LatLng position, boolean isDraggable) {
        final MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .draggable(isDraggable)
                .alpha(markerInfo.getAlpha())
                .anchor(markerInfo.getAnchorU(), markerInfo.getAnchorV())
                .infoWindowAnchor(markerInfo.getInfoWindowAnchorU(),
                        markerInfo.getInfoWindowAnchorV()).rotation(markerInfo.getRotation())
                .snippet(markerInfo.getSnippet()).title(markerInfo.getTitle())
                .flat(markerInfo.isFlat()).visible(markerInfo.isVisible());

        final Bitmap markerIcon = markerInfo.getIcon(getResources());
        if (markerIcon != null) {
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerIcon));
        }
        if (mMap != null) {
            Marker marker = mMap.addMarker(markerOptions);
            mBiMarkersMap.put(markerInfo, marker);
        }
    }

    private void updateMarker(Marker marker, MarkerInfo markerInfo, LatLng position,
                              boolean isDraggable) {
        final Bitmap markerIcon = markerInfo.getIcon(getResources());
        if (markerIcon != null) {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(markerIcon));
        }

        marker.setAlpha(markerInfo.getAlpha());
        marker.setAnchor(markerInfo.getAnchorU(), markerInfo.getAnchorV());
        marker.setInfoWindowAnchor(markerInfo.getInfoWindowAnchorU(),
                markerInfo.getInfoWindowAnchorV());
        marker.setPosition(position);
        marker.setRotation(markerInfo.getRotation());
        marker.setSnippet(markerInfo.getSnippet());
        marker.setTitle(markerInfo.getTitle());
        marker.setDraggable(isDraggable);
        marker.setFlat(markerInfo.isFlat());
        marker.setVisible(markerInfo.isVisible());
    }

    @Override
    public void updatePOIMarkers(List<POIItem> items) {
        for (POIItem item : items) {
            MarkerOptions markerOption = new MarkerOptions().position(new LatLng(item.getLatitude(), item.getLongitude()));
            markerOption.icon(BitmapDescriptorFactory.fromBitmap(MarkerWithText.drawTextToBitmap(getContext(), R.drawable.ic_poi_marker, item.getName())));
            Marker marker = mMap.addMarker(markerOption);
            mPOIMarkerList.add(marker);
        }
    }

    @Override
    public void updateMarkers(List<MarkerInfo> markersInfos) {
        for (MarkerInfo info : markersInfos) {
            updateMarker(info);
        }
    }

    @Override
    public void updateMarkers(List<MarkerInfo> markersInfos, boolean isDraggable) {
        for (MarkerInfo info : markersInfos) {
            updateMarker(info, isDraggable);
        }
    }

    @Override
    public Set<MarkerInfo> getMarkerInfoList() {
        return new HashSet<MarkerInfo>(mBiMarkersMap.keySet());
    }

    @Override
    public List<LatLong> projectPathIntoMap(List<LatLong> path) {
        List<LatLong> coords = new ArrayList<LatLong>();
        Projection projection = mMap.getProjection();

        for (LatLong point : path) {
            LatLng coord = projection.fromScreenLocation(new Point((int) point
                    .getLatitude(), (int) point.getLongitude()));
            coords.add(DroneHelper.LatLngToCoord(coord));
        }

        return coords;
    }

    @Override
    public void removeMarkers(Collection<MarkerInfo> markerInfoList) {
        if (markerInfoList == null || markerInfoList.isEmpty()) {
            return;
        }

        for (MarkerInfo markerInfo : markerInfoList) {
            Marker marker = mBiMarkersMap.getValue(markerInfo);
            if (marker != null) {
                marker.remove();
                mBiMarkersMap.removeKey(markerInfo);
            }
        }
    }

    @Override
    public void removePOIMarkers() {
        for (Marker marker : mPOIMarkerList) {
            marker.remove();
        }
        mPOIMarkerList.clear();
    }

    @Override
    public void setMapPadding(int left, int top, int right, int bottom) {
        mMap.setPadding(left, top, right, bottom);
    }

    @Override
    public void setOnMapClickListener(OnMapClickListener listener) {
        mMapClickListener = listener;
    }

    @Override
    public void setOnMapLongClickListener(OnMapLongClickListener listener) {
        mMapLongClickListener = listener;
    }

    @Override
    public void setOnMarkerDragListener(OnMarkerDragListener listener) {
        mMarkerDragListener = listener;
    }

    @Override
    public void setOnMarkerClickListener(OnMarkerClickListener listener) {
        mMarkerClickListener = listener;
    }

    @Override
    public void setLocationListener(android.location.LocationListener receiver) {
        mLocationListener = receiver;

        //Update the listener with the last received location
        if (mLocationListener != null) {
            mGApiClientMgr.addTask(requestLastLocationTask);
        }
    }

    public void updateCamera(final LatLong coord) {
        if (coord != null) {
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    final float zoomLevel = googleMap.getCameraPosition().zoom;
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(DroneHelper.CoordToLatLang(coord),
                            zoomLevel));
                }
            });
        }
    }

    @Override
    public void updateCamera(final LatLong coord, final float zoomLevel) {
        if (coord != null) {
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            DroneHelper.CoordToLatLang(coord), zoomLevel));
                }
            });
        }
    }

    @Override
    public void updateCameraBearing(float bearing) {
        final CameraPosition cameraPosition = new CameraPosition(DroneHelper.CoordToLatLang
                (getMapCenter()), getMapZoomLevel(), 0, bearing);
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void updateDroneLeashPath(PathSource pathSource) {
        List<LatLong> pathCoords = pathSource.getPathPoints();
        final List<LatLng> pathPoints = new ArrayList<LatLng>(pathCoords.size());
        for (LatLong coord : pathCoords) {
            pathPoints.add(DroneHelper.CoordToLatLang(coord));
        }

        if (mDroneLeashPath == null) {
            final PolylineOptions flightPath = new PolylineOptions();
            flightPath.color(Color.parseColor("#e33d1b")).width(
                    DroneHelper.scaleDpToPixels(DRONE_LEASH_DEFAULT_WIDTH, getResources()));

            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mDroneLeashPath = mMap.addPolyline(flightPath);
                    mDroneLeashPath.setPoints(pathPoints);
                }
            });
        }
        else {
            mDroneLeashPath.setPoints(pathPoints);
        }
    }

    @Override
    public void updateMissionPath(PathSource pathSource) {
        List<LatLong> pathCoords = pathSource.getPathPoints();
        final List<LatLng> pathPoints = new ArrayList<>(pathCoords.size());
        for (LatLong coord : pathCoords) {
            pathPoints.add(DroneHelper.CoordToLatLang(coord));
        }

        if (missionPath == null) {
            final PolylineOptions pathOptions = new PolylineOptions();
            pathOptions.color(Color.parseColor("#e33d1b")).width(MISSION_PATH_DEFAULT_WIDTH);
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    missionPath = mMap.addPolyline(pathOptions);
                    missionPath.setPoints(pathPoints);
                }
            });
        }
        else {
            missionPath.setPoints(pathPoints);
        }
    }


    @Override
    public void updatePolygonsPaths(List<List<LatLong>> paths) {
        for (Polygon poly : polygonsPaths) {
            poly.remove();
        }

        for (List<LatLong> contour : paths) {
            PolygonOptions pathOptions = new PolygonOptions();
            pathOptions.strokeColor(POLYGONS_PATH_DEFAULT_COLOR).strokeWidth(
                    POLYGONS_PATH_DEFAULT_WIDTH);
            final List<LatLng> pathPoints = new ArrayList<LatLng>(contour.size());
            for (LatLong coord : contour) {
                pathPoints.add(DroneHelper.CoordToLatLang(coord));
            }
            pathOptions.addAll(pathPoints);
            polygonsPaths.add(mMap.addPolygon(pathOptions));
        }

    }

    @Override
    public void addCameraFootprint(FootPrint footprintToBeDraw) {
        PolygonOptions pathOptions = new PolygonOptions();
        pathOptions.strokeColor(FOOTPRINT_DEFAULT_COLOR).strokeWidth(FOOTPRINT_DEFAULT_WIDTH);
        pathOptions.fillColor(FOOTPRINT_FILL_COLOR);

        for (LatLong vertex : footprintToBeDraw.getVertexInGlobalFrame()) {
            pathOptions.add(DroneHelper.CoordToLatLang(vertex));
        }
        mMap.addPolygon(pathOptions);

    }

    /**
     * Save the map camera state on a preference file
     * http://stackoverflow.com/questions
     * /16697891/google-maps-android-api-v2-restoring
     * -map-state/16698624#16698624
     */
    @Override
    public void saveCameraPosition() {
        final GoogleMap googleMap = mMap;
        if(googleMap == null)
            return;

        CameraPosition camera = googleMap.getCameraPosition();
        mAppPrefs.prefs.edit()
                .putFloat(PREF_LAT, (float) camera.target.latitude)
                .putFloat(PREF_LNG, (float) camera.target.longitude)
                .putFloat(PREF_BEA, camera.bearing)
                .putFloat(PREF_TILT, camera.tilt)
                .putFloat(PREF_ZOOM, camera.zoom).apply();
    }

    @Override
    public void loadCameraPosition() {
        getMapAsync(loadCameraPositionTask);
    }

    private void setupMap() {
        // Make sure the map is initialized
        MapsInitializer.initialize(getActivity().getApplicationContext());

        getMapAsync(setupMapTask);
    }

    @Override
    public void zoomToFit(List<LatLong> coords) {
        if (!coords.isEmpty()) {
            final List<LatLng> points = new ArrayList<LatLng>();
            for (LatLong coord : coords)
                points.add(DroneHelper.CoordToLatLang(coord));

            final LatLngBounds bounds = getBounds(points);
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    final Activity activity = getActivity();
                    if (activity == null)
                        return;

                    final View rootView = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
                    if (rootView == null)
                        return;

                    final int height = rootView.getHeight();
                    final int width = rootView.getWidth();
                    Timber.d("Screen W %d, H %d", width, height);
                    if (height > 0 && width > 0) {
                        CameraUpdate animation = CameraUpdateFactory.newLatLngBounds(bounds, width, height, 100);
                        googleMap.animateCamera(animation);
                    }
                }
            });
        }
    }

    @Override
    public void zoomToFitMyLocation(final List<LatLong> coords) {
        mGApiClientMgr.addTask(new GoogleApiClientTask() {
            @Override
            protected void doRun() {
                final Location myLocation = LocationServices.FusedLocationApi.getLastLocation(getGoogleApiClient());
                if (myLocation != null) {
                    final List<LatLong> updatedCoords = new ArrayList<LatLong>(coords);
                    updatedCoords.add(DroneHelper.LocationToCoord(myLocation));
                    zoomToFit(updatedCoords);
                } else {
                    zoomToFit(coords);
                }
            }
        });
    }

    @Override
    public void goToMyLocation() {
        if (!mGApiClientMgr.addTask(mGoToMyLocationTask)) {
            Timber.e("Unable to add google api client task.");
        }
    }

    @Override
    public void goToDroneLocation() {
        Drone dpApi = getDroneApi();
        if (!dpApi.isConnected())
            return;

        Gps gps = dpApi.getAttribute(AttributeType.GPS);
        if (!gps.isValid()) {
            Toast.makeText(getActivity().getApplicationContext(), R.string.drone_no_location, Toast.LENGTH_SHORT).show();
            return;
        }

        final float currentZoomLevel = mMap.getCameraPosition().zoom;
        final LatLong droneLocation = gps.getPosition();
        updateCamera(droneLocation, (int) currentZoomLevel);
    }

    private void setupMapListeners(GoogleMap googleMap) {
        final GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mMapClickListener != null) {
                    mMapClickListener.onMapClick(DroneHelper.LatLngToCoord(latLng));
                }
            }
        };
        googleMap.setOnMapClickListener(onMapClickListener);

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (mMapLongClickListener != null) {
                    mMapLongClickListener.onMapLongClick(DroneHelper.LatLngToCoord(latLng));
                }
            }
        });

        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                if (mMarkerDragListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    if(!(markerInfo instanceof GraphicHome)) {
                        markerInfo.setPosition(DroneHelper.LatLngToCoord(marker.getPosition()));
                        mMarkerDragListener.onMarkerDragStart(markerInfo);
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                if (mMarkerDragListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    if(!(markerInfo instanceof GraphicHome)) {
                        markerInfo.setPosition(DroneHelper.LatLngToCoord(marker.getPosition()));
                        mMarkerDragListener.onMarkerDrag(markerInfo);
                    }
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (mMarkerDragListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    markerInfo.setPosition(DroneHelper.LatLngToCoord(marker.getPosition()));
                    mMarkerDragListener.onMarkerDragEnd(markerInfo);
                }
            }
        });

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (useMarkerClickAsMapClick) {
                    onMapClickListener.onMapClick(marker.getPosition());
                    return true;
                }

                if (mMarkerClickListener != null) {
                    final MarkerInfo markerInfo = mBiMarkersMap.getKey(marker);
                    if (markerInfo != null)
                        return mMarkerClickListener.onMarkerClick(markerInfo);
                }
                return false;
            }
        });
    }

    private void setupMapUI(GoogleMap map) {
        map.setMyLocationEnabled(false);
        UiSettings mUiSettings = map.getUiSettings();
        mUiSettings.setMyLocationButtonEnabled(false);
        mUiSettings.setMapToolbarEnabled(false);
        mUiSettings.setCompassEnabled(false);
        mUiSettings.setTiltGesturesEnabled(false);
        mUiSettings.setZoomControlsEnabled(false);
        mUiSettings.setRotateGesturesEnabled(mAppPrefs.isMapRotationEnabled());
    }

    private void setupMapOverlay(GoogleMap map) {
        final Context context = getContext();
        if(context == null)
            return;

        final @GoogleMapPrefConstants.TileProvider String tileProvider = GoogleMapPrefFragment.PrefManager.getMapTileProvider(context);
        switch(tileProvider){
            case GoogleMapPrefConstants.GOOGLE_TILE_PROVIDER:
                setupGoogleTileProvider(context, map);
                break;

            case GoogleMapPrefConstants.MAPBOX_TILE_PROVIDER:
                setupMapboxTileProvider(context, map);
                break;

            case GoogleMapPrefConstants.ARC_GIS_TILE_PROVIDER:
                setupArcGISTileProvider(context, map);
                break;
        }
    }

    private void setupGoogleTileProvider(Context context, GoogleMap map){
        //Reset the tile provider manager
        tileProviderManager = null;

        //Remove the mapbox tile providers
        if(offlineTileOverlay != null){
            offlineTileOverlay.remove();
            offlineTileOverlay = null;
        }

        if(onlineTileOverlay != null){
            onlineTileOverlay.remove();
            onlineTileOverlay = null;
        }

        map.setMapType(GoogleMapPrefFragment.PrefManager.getMapType(context));
        //try {

        //}
        //catch (IOException e) {
          //  e.printStackTrace();}
        }

    private void setupArcGISTileProvider(Context context, GoogleMap map){
        Timber.i("Enabling ArcGIS tile provider.");

        //Remove the default google map layer
        map.setMapType(GoogleMap.MAP_TYPE_NONE);

        final GoogleMapPrefFragment.PrefManager prefManager = GoogleMapPrefFragment.PrefManager;
        String selectedMap = prefManager.getArcGISMapType(context);

        if(!(tileProviderManager instanceof ArcGISTileProviderManager)
            || !selectedMap.equals(((ArcGISTileProviderManager) tileProviderManager).getSelectedMap())){

            //Setup the online tile overlay
            if(onlineTileOverlay != null){
                onlineTileOverlay.remove();
                onlineTileOverlay = null;
            }

            tileProviderManager = new ArcGISTileProviderManager(context, selectedMap);
            TileOverlayOptions options = new TileOverlayOptions()
                .tileProvider(tileProviderManager.getOnlineTileProvider())
                .zIndex(ONLINE_TILE_PROVIDER_Z_INDEX);

            onlineTileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(getResources().getAssets())).zIndex(-1));//map.addTileOverlay(options);

            //Setup the offline tile overlay
            if(offlineTileOverlay != null){
                offlineTileOverlay.remove();
                offlineTileOverlay = null;
            }

            if(prefManager.isOfflineMapLayerEnabled(context)){
                options = new TileOverlayOptions()
                    .tileProvider(tileProviderManager.getOfflineTileProvider())
                    .zIndex(OFFLINE_TILE_PROVIDER_Z_INDEX);

                offlineTileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(getResources().getAssets())).zIndex(-1));//map.addTileOverlay(options);
            }
        }
    }

    private void setupMapboxTileProvider(Context context, GoogleMap map){
        Timber.d("Enabling mapbox tile provider.");

        //Remove the default google map layer.
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        final GoogleMapPrefFragment.PrefManager prefManager = GoogleMapPrefFragment.PrefManager;
        final String mapboxId = prefManager.getMapboxId(context);
        final String mapboxAccessToken = prefManager.getMapboxAccessToken(context);
        final int maxZoomLevel = (int) map.getMaxZoomLevel();

        if (!(tileProviderManager instanceof MapboxTileProviderManager)
            || !mapboxId.equals(((MapboxTileProviderManager) tileProviderManager).getMapboxId())
            || !mapboxAccessToken.equals(((MapboxTileProviderManager) tileProviderManager).getMapboxAccessToken())) {

            //Setup the online tile overlay
            if (onlineTileOverlay != null) {
                onlineTileOverlay.remove();
                onlineTileOverlay = null;
            }

            tileProviderManager = new MapboxTileProviderManager(context, mapboxId, mapboxAccessToken, maxZoomLevel);
            TileOverlayOptions options = new TileOverlayOptions()
                .tileProvider(tileProviderManager.getOnlineTileProvider())
                .zIndex(ONLINE_TILE_PROVIDER_Z_INDEX);

            onlineTileOverlay = map.addTileOverlay(options);
           // map.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(getResources().getAssets())));
            //Setup the offline tile overlay
            if(offlineTileOverlay != null){
                offlineTileOverlay.remove();
                offlineTileOverlay = null;
            }

            if(prefManager.isOfflineMapLayerEnabled(context)){
                options = new TileOverlayOptions()
                    .tileProvider(tileProviderManager.getOfflineTileProvider())
                    .zIndex(OFFLINE_TILE_PROVIDER_Z_INDEX);

                offlineTileOverlay = map.addTileOverlay(options);
            }
        }

        //Check if the mapbox credentials are valid.
        new AsyncTask<Void, Void, Integer>(){

            @Override
            protected Integer doInBackground(Void... params) {
                final Context context = getContext();
                return MapboxUtils.fetchReferenceTileUrl(context, mapboxId, mapboxAccessToken);
            }

            @Override
            protected void onPostExecute(Integer result){
                if(result != null){
                    switch(result){
                        case HttpURLConnection.HTTP_UNAUTHORIZED:
                        case HttpURLConnection.HTTP_NOT_FOUND:
                            //Invalid mapbox credentials
                            Toast.makeText(getContext(), R.string.alert_invalid_mapbox_credentials, Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }
        }.execute();
    }

    protected void clearMap() {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.clear();
                setupMapOverlay(googleMap);
            }
        });
    }

    private LatLngBounds getBounds(List<LatLng> pointsList) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : pointsList) {
            builder.include(point);
        }
        return builder.build();
    }

    public double getMapRotation() {
        GoogleMap map = mMap;
        if (map != null) {
            return map.getCameraPosition().bearing;
        } else {
            return 0;
        }
    }

    public VisibleMapArea getVisibleMapArea(){
        final GoogleMap map = mMap;
        if(map == null)
            return null;

        final VisibleRegion mapRegion = map.getProjection().getVisibleRegion();
        return new VisibleMapArea(DroneHelper.LatLngToCoord(mapRegion.farLeft),
                DroneHelper.LatLngToCoord(mapRegion.nearLeft),
                DroneHelper.LatLngToCoord(mapRegion.nearRight),
                DroneHelper.LatLngToCoord(mapRegion.farRight));
    }

    @Override
    public void skipMarkerClickEvents(boolean skip) {
        useMarkerClickAsMapClick = skip;
    }

    @Override
    public void updateRealTimeFootprint(FootPrint footprint) {
        List<LatLong> pathPoints = footprint == null
                ? Collections.<LatLong>emptyList()
                : footprint.getVertexInGlobalFrame();

        if (pathPoints.isEmpty()) {
            if (footprintPoly != null) {
                footprintPoly.remove();
                footprintPoly = null;
            }
        } else {
            if (footprintPoly == null) {
                PolygonOptions pathOptions = new PolygonOptions()
                        .strokeColor(FOOTPRINT_DEFAULT_COLOR)
                        .strokeWidth(FOOTPRINT_DEFAULT_WIDTH)
                        .fillColor(FOOTPRINT_FILL_COLOR);

                for (LatLong vertex : pathPoints) {
                    pathOptions.add(DroneHelper.CoordToLatLang(vertex));
                }
                footprintPoly = mMap.addPolygon(pathOptions);
            } else {
                List<LatLng> list = new ArrayList<LatLng>();
                for (LatLong vertex : pathPoints) {
                    list.add(DroneHelper.CoordToLatLang(vertex));
                }
                footprintPoly.setPoints(list);
            }
        }

    }

    @Override
    public void onGoogleApiConnectionError(ConnectionResult connectionResult) {
        final Activity activity = getActivity();
        if (activity == null)
            return;

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(activity, 0);
            } catch (IntentSender.SendIntentException e) {
                //There was an error with the resolution intent. Try again.
                if (mGApiClientMgr != null)
                    mGApiClientMgr.start();
            }
        } else {
            onUnavailableGooglePlayServices(connectionResult.getErrorCode());
        }
    }

    @Override
    public void onUnavailableGooglePlayServices(int i) {
        final Activity activity = getActivity();
        if (activity != null) {
            GooglePlayServicesUtil.showErrorDialogFragment(i, getActivity(), 0, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    activity.finish();
                }
            });
        }
    }

    @Override
    public void onManagerStarted() {

    }

    @Override
    public void onManagerStopped() {

    }

    @Override
    public void onZoom(View view) {
        if(view.getId()==R.id.zoomin)
        {
            mMap.animateCamera(CameraUpdateFactory.zoomIn());
        }
        if(view.getId()==R.id.zoomout)
        {
            mMap.animateCamera(CameraUpdateFactory.zoomOut());
        }
    }
    @Override
    public void updateNextMarkerSetGreenIcon(MarkerInfo markerInfo,String title) {
        Marker marker = mBiMarkersMap.getValue(markerInfo);
        if (marker != null) {
            final LatLong coord = markerInfo.getPosition();
            if (coord == null) {
                return;
            }

            final LatLng position = DroneHelper.CoordToLatLang(coord);

            Bitmap bitmap=MarkerWithText.getMarkerWithTextAndDetail(  R.drawable.ic_wp_map_green,
                    title,null,getResources());

            marker.setAlpha(markerInfo.getAlpha());
            marker.setAnchor(markerInfo.getAnchorU(), markerInfo.getAnchorV());
            marker.setInfoWindowAnchor(markerInfo.getInfoWindowAnchorU(),
                    markerInfo.getInfoWindowAnchorV());
            marker.setPosition(position);
            marker.setRotation(markerInfo.getRotation());
            marker.setSnippet(markerInfo.getSnippet());
            String s=markerInfo.getTitle();
            marker.setTitle(title);
//            marker.setDraggable(isDraggable);
            marker.setFlat(markerInfo.isFlat());
            marker.setVisible(markerInfo.isVisible());

            Bitmap green_next_wp_bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_wp_map_green);
            if (bitmap != null) {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
            }


        }
    }
}
