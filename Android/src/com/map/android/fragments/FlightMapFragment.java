package com.map.android.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.map.android.fragments.mode.ModeAutoFragment;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.Gps;
import com.map.android.lib.drone.property.GuidedState;
import com.map.android.lib.drone.property.State;

import com.map.android.R;
import com.map.android.dialogs.GuidedDialog;
import com.map.android.dialogs.GuidedDialog.GuidedDialogListener;
import com.map.android.graphic.map.GraphicHome;
import com.map.android.maps.DPMap;
import com.map.android.maps.MarkerInfo;
import com.map.android.proxy.mission.MissionProxy;
import com.map.android.proxy.mission.item.markers.MissionItemMarkerInfo;
import com.map.android.utils.DroneHelper;
import com.map.android.utils.prefs.AutoPanMode;
import com.map.android.view.spinnerWheel.CardWheelHorizontalView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.List;

public class FlightMapFragment extends DroneMap implements DPMap.OnMapLongClickListener,
        DPMap.OnMarkerClickListener, DPMap.OnMarkerDragListener, GuidedDialogListener {

//    public interface OnGuidedClickListener {
//        void onGuidedClick(LatLong coord);
//    }

    private static final int MAX_TOASTS_FOR_LOCATION_PRESS = 3;

    private static final String PREF_USER_LOCATION_FIRST_PRESS = "pref_user_location_first_press";
    private static final int DEFAULT_USER_LOCATION_FIRST_PRESS = 0;

    private static final String PREF_DRONE_LOCATION_FIRST_PRESS = "pref_drone_location_first_press";
    private static final int DEFAULT_DRONE_LOCATION_FIRST_PRESS = 0;

    /**
     * The map should zoom on the user location the first time it's acquired. This flag helps
     * enable the behavior.
     */
    private static boolean didZoomOnUserLocation = false;


    private static final IntentFilter eventFilter = new IntentFilter(AttributeEvent.STATE_ARMING);

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AttributeEvent.STATE_ARMING.equals(action)) {
                final State droneState = drone.getAttribute(AttributeType.STATE);
                if (droneState.isArmed()) {
                    mMapFragment.clearFlightPath();
                }
            }
        }
    };

    private OnGuidedClickListener guidedClickListener;

    public void setGuidedClickListener(OnGuidedClickListener guidedClickListener) {
        this.guidedClickListener = guidedClickListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        View view = super.onCreateView(inflater, viewGroup, bundle);

        mMapFragment.setOnMapLongClickListener(this);
        mMapFragment.setOnMarkerDragListener(this);
        mMapFragment.setOnMarkerClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapFragment.selectAutoPanMode(mAppPrefs.getAutoPanMode());

        if (!didZoomOnUserLocation) {
            super.goToMyLocation();
            didZoomOnUserLocation = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapFragment.selectAutoPanMode(AutoPanMode.DISABLED);
    }

    @Override
    protected int getMaxFlightPathSize() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.valueOf(prefs.getString("pref_max_flight_path_size", "0"));
    }

    @Override
    public boolean setAutoPanMode(AutoPanMode target) {
        // Update the map panning preferences.
        if (mAppPrefs != null)
            mAppPrefs.setAutoPanMode(target);

        if (mMapFragment != null)
            mMapFragment.selectAutoPanMode(target);
        return true;
    }

    @Override
    public void onApiConnected() {
        super.onApiConnected();
        getBroadcastManager().registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onApiDisconnected() {
        super.onApiDisconnected();
        getBroadcastManager().unregisterReceiver(eventReceiver);
    }

    @Override
    public void onMapLongClick(LatLong coord) {
        if (drone != null && drone.isConnected()) {
//            final GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
//            if (guidedState.isInitialized()) {
//                if(guidedClickListener != null)
//                    guidedClickListener.onGuidedClick(coord);
//            } else {
//                GuidedDialog dialog = new GuidedDialog();
//                dialog.setCoord(DroneHelper.CoordToLatLang(coord));
//                dialog.setListener(this);
//                dialog.show(getChildFragmentManager(), "GUIDED dialog");
//            }
        }
    }

    @Override
    public void onForcedGuidedPoint(LatLng coord) {
        try {
            drone.sendGuidedPoint(DroneHelper.LatLngToCoord(coord), true);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMarkerDragStart(MarkerInfo markerInfo) {
    }

    @Override
    public void onMarkerDrag(MarkerInfo markerInfo) {
    }

    @Override
    public void onMarkerDragEnd(MarkerInfo markerInfo) {
        if (!(markerInfo instanceof GraphicHome)) {
           // drone.sendGuidedPoint(markerInfo.getPosition(), false);
        }
    }

    @Override
    public boolean onMarkerClick(MarkerInfo markerInfo) {
        if(markerInfo == null)
            return false;
//        drone.sendGuidedPoint(markerInfo.getPosition(), false);
        if (markerInfo instanceof MissionItemMarkerInfo) {
            CardWheelHorizontalView cardWheelHorizontalView= ModeAutoFragment.getCardWheelHorizontalViewInstance();
            boolean isRestart = ModeAutoFragment.getIsRestartSelected();
            SlidingUpPanelLayout.PanelState slidingUpPanelState= ModeAutoFragment.getslidingUpPanelState();

            if(cardWheelHorizontalView!=null && slidingUpPanelState != null)
            if(!isRestart && slidingUpPanelState == SlidingUpPanelLayout.PanelState.EXPANDED)
            { List<MarkerInfo> markerInfoList = getMissionProxy().getMarkersInfos();
            int selectedInedx=0;
            for (int i = 0 ; i < markerInfoList.size() ; i++ )
            {
             if(markerInfoList.get(i).equals(markerInfo))
                 selectedInedx=i;
            }
            cardWheelHorizontalView.setCurrentValue(selectedInedx+1);
            ModeAutoFragment.setEndValue(selectedInedx+1);
            setMissionItemMarkerInfoSelection((MissionItemMarkerInfo)markerInfo);
            }
        }

        return true;
    }
    //select marker on map when click it
    public void setMissionItemMarkerInfoSelection(MissionItemMarkerInfo markerInfo)
    {
        final MissionProxy missionProxy = getMissionProxy();
        missionProxy.selection.getSelected().clear();
        missionProxy.selection.setSelectionTo( markerInfo.getMarkerOrigin());
        mMapFragment.updateMarkers(missionProxy.getMarkersInfos(),false);

    }
    //change marker icon to green when nextWp change
    public void setNextMissionItemMarkerInfoGreenIcon(int nextWaypoint)
    {
        MissionProxy missionProxy2 = getMissionProxy();
        if(missionProxy2.getItems().size()!=0)
        {
            mMapFragment.updateMarkers(missionProxy.getMarkersInfos(),false);
            if( nextWaypoint > 0 )
            mMapFragment.updateNextMarkerSetGreenIcon(missionProxy2.getMarkersInfos().get(nextWaypoint-1), String.valueOf(nextWaypoint));
        }
    }
    //clear all selection marker
    public void clearSelectionMarker()
    {
        final MissionProxy missionProxy = getMissionProxy();
        if(!missionProxy.getItems().isEmpty())
        { missionProxy.selection.getSelected().clear();
            mMapFragment.updateMarkers(missionProxy.getMarkersInfos(),false);}

    }
    @Override
    protected boolean isMissionDraggable() {
        return false;
    }

    @Override
    public void goToMyLocation() {
        super.goToMyLocation();
        int pressCount = mAppPrefs.prefs.getInt(PREF_USER_LOCATION_FIRST_PRESS,
                DEFAULT_USER_LOCATION_FIRST_PRESS);
        if (pressCount < MAX_TOASTS_FOR_LOCATION_PRESS) {
            Toast.makeText(context, R.string.user_autopan_long_press, Toast.LENGTH_LONG).show();
            mAppPrefs.prefs.edit().putInt(PREF_USER_LOCATION_FIRST_PRESS, pressCount + 1).apply();
        }
    }

    @Override
    public void goToDroneLocation() {
        super.goToDroneLocation();

        if(this.drone == null)
            return;

        final Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        if (droneGps == null || !droneGps.isValid())
            return;

        final int pressCount = mAppPrefs.prefs.getInt(PREF_DRONE_LOCATION_FIRST_PRESS,
                DEFAULT_DRONE_LOCATION_FIRST_PRESS);
        if (pressCount < MAX_TOASTS_FOR_LOCATION_PRESS) {
            Toast.makeText(context, R.string.drone_autopan_long_press, Toast.LENGTH_LONG).show();
            mAppPrefs.prefs.edit().putInt(PREF_DRONE_LOCATION_FIRST_PRESS, pressCount + 1).apply();
        }
    }

    public void onZoom(View view){
        if(view.getId()== R.id.zoomin)
        {
            mMapFragment.onZoom(view);
        }
        if(view.getId()==R.id.zoomout)
        {
            mMapFragment.onZoom(view);
        }
    }

}
