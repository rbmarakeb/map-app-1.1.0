package com.map.android.fragments.mode;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.map.android.activities.FlightActivity;
import com.map.android.fragments.FlightDataFragment;
import com.map.android.fragments.FlightMapFragment;
import com.map.android.fragments.control.FlightControlManagerFragment;
import com.map.android.fragments.control.RoverFlightControlFragment;
import com.map.android.lib.drone.property.Speed;
import com.map.android.lib.drone.property.State;
import com.map.android.proxy.mission.MissionProxy;
import com.map.android.proxy.mission.item.markers.MissionItemMarkerInfo;
import com.map.android.utils.analytics.GAUtils;
import com.map.android.view.spinnerWheel.adapters.NumericWheelAdapter;
import com.map.android.client.Drone;
import com.map.android.client.apis.MissionApi;
import com.map.android.client.apis.VehicleApi;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.coordinate.LatLongAlt;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeEventExtra;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.mission.Mission;
import com.map.android.lib.drone.mission.item.MissionItem;
import com.map.android.lib.drone.property.Gps;
import com.map.android.lib.drone.property.VehicleMode;
import com.map.android.lib.model.AbstractCommandListener;
import com.map.android.lib.util.MathUtils;

import com.map.android.DroidPlannerApp;
import com.map.android.R;

import com.map.android.view.spinnerWheel.CardWheelHorizontalView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

public class ModeAutoFragment extends Fragment implements View.OnClickListener, CardWheelHorizontalView.OnCardWheelScrollListener<Integer> {
    private Drone drone;

    private static final IntentFilter eventFilter = new IntentFilter();
    static{
        eventFilter.addAction(AttributeEvent.MISSION_ITEM_UPDATED);
        eventFilter.addAction(AttributeEvent.PARAMETER_RECEIVED);
        eventFilter.addAction(AttributeEvent.GPS_POSITION);
        eventFilter.addAction(AttributeEvent.MISSION_UPDATED);
        eventFilter.addAction(AttributeEvent.MISSION_RECEIVED);
    }
    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action){
                case AttributeEvent.MISSION_RECEIVED:
                case AttributeEvent.MISSION_UPDATED:
                    final MissionProxy missionProxy = getMissionProxy();
                    if(missionProxy != null) {
                        mission = drone.getAttribute(AttributeType.MISSION);
                        waypointSelectorAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered_custom,
                                missionProxy.getFirstWaypoint(), missionProxy.getLastWaypoint(), "%3d");
                        waypointSelector.setViewAdapter(waypointSelectorAdapter);
                    }
                    break;

                case AttributeEvent.MISSION_ITEM_UPDATED:
                    mission = drone.getAttribute(AttributeType.MISSION);
                    nextWaypoint = intent.getIntExtra(AttributeEventExtra.EXTRA_MISSION_CURRENT_WAYPOINT, 0);
                    waypointSelector.setCurrentValue(nextWaypoint);
                    break;
                case AttributeEvent.GPS_POSITION:
                    updateMission();
                    break;
            }
        }
    };
    private Mission mission;
    private int nextWaypoint;
    private ProgressBar missionProgress;
    private double remainingMissionLength;
    private boolean missionFinished;
    public static CardWheelHorizontalView<Integer> waypointSelector;
    private NumericWheelAdapter waypointSelectorAdapter;
    private ImageView restart_image;
    private TextView restart_tv;
    private ImageView select_waypoint_image;
    private TextView select_waypoint_tv;
    private Context context;
    private static boolean isRestart=true;
    private static SlidingUpPanelLayout slidingUpPanelLayout;
    private static Integer endValue=0;
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_mode_auto, container, false);
	}
    public static CardWheelHorizontalView getCardWheelHorizontalViewInstance()
    {
        return waypointSelector;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.mc_pause).setOnClickListener(this);
        view.findViewById(R.id.mc_restart).setOnClickListener(this);
        missionProgress = (ProgressBar) view.findViewById(R.id.mission_progress);
        waypointSelector = (CardWheelHorizontalView<Integer>) view.findViewById(R.id.waypoint_selector);
        waypointSelector.addScrollListener(this);
        mission = drone.getAttribute(AttributeType.MISSION);

        final DroidPlannerApp dpApp = (DroidPlannerApp) getActivity().getApplication();

        final MissionProxy missionProxy = getMissionProxy();
        waypointSelectorAdapter = new NumericWheelAdapter(getActivity().getApplicationContext(),
                R.layout.wheel_text_centered_custom,
                missionProxy.getFirstWaypoint(), missionProxy.getLastWaypoint(), "%3d");
        waypointSelector.setViewAdapter(waypointSelectorAdapter);
        view.findViewById(R.id.restart_line).setOnClickListener(this);
        view.findViewById(R.id.select_waypoint_line).setOnClickListener(this);
        view.findViewById(R.id.confirm).setOnClickListener(this);
        view.findViewById(R.id.cancel).setOnClickListener(this);
        context=view.getContext();
        restart_image =(ImageView)view.findViewById(R.id.restart_image);
        select_waypoint_image =(ImageView)view.findViewById(R.id.select_waypoint_image);
        restart_tv =(TextView)view.findViewById(R.id.restart_tv);
        select_waypoint_tv =(TextView)view.findViewById(R.id.select_waypoint_tv);
        slidingUpPanelLayout= FlightDataFragment.getSlidingUpPaneInstance();
        isRestart=true;
    }

    private MissionProxy getMissionProxy(){
        final Activity activity = getActivity();
        if(activity == null)
            return null;

        return ((DroidPlannerApp) activity.getApplication()).getMissionProxy();
    }

    @Override
	public void onClick(View v) {
        if(mission == null){
            mission = drone.getAttribute(AttributeType.MISSION);
        }
		switch(v.getId()){
			case R.id.mc_pause: {
                drone.pauseAtCurrentLocation();
                break;
            }
			case R.id.mc_restart: {
                gotoMissionItem(0);
                break;
            }
            case R.id.restart_line: {
                restart_image.setColorFilter(ContextCompat.getColor(context, R.color.enable_icon), android.graphics.PorterDuff.Mode.MULTIPLY);
                select_waypoint_image.setColorFilter(getResources().getColor(R.color.disable_icon));

                restart_tv.setTextColor(getResources().getColor(R.color.enable_text));
                select_waypoint_tv.setTextColor(getResources().getColor(R.color.disable_text));
                waypointSelector.setAlpha(0.1f);
                isRestart=true;
                MissionProxy missionProxy2 = getMissionProxy();
                FlightDataFragment flightDataFragment=FlightActivity.getFlightDataInstance();
                FlightMapFragment flightMapFragment=flightDataFragment.getFlightMapFragmentInstance();

                if(missionProxy2.getItems().size()!=0)
                {
//                missionProxy.selection.clearSelection();
//                missionProxy.selection.setSelectionTo(missionProxy.getItems().get(endValue-1));
                    flightMapFragment.setMissionItemMarkerInfoSelection((MissionItemMarkerInfo)missionProxy2.getMarkersInfos().get(0));

                }
                break;
            }
            case R.id.select_waypoint_line: {
                restart_image.setColorFilter(ContextCompat.getColor(context, R.color.disable_text), android.graphics.PorterDuff.Mode.MULTIPLY);
                select_waypoint_image.setColorFilter(getResources().getColor(R.color.enable_icon));

                restart_tv.setTextColor(getResources().getColor(R.color.disable_text));
                select_waypoint_tv.setTextColor(getResources().getColor(R.color.enable_text));
                waypointSelector.setAlpha(1);
                isRestart=false;
                break;
            }
            case R.id.confirm: {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                RoverFlightControlFragment actionsBarFragment= (RoverFlightControlFragment) FlightControlManagerFragment.getactionsBarFragmentInstance();
                final State droneState = drone.getAttribute(AttributeType.STATE);
                final VehicleMode vehicleMode=droneState.getVehicleMode();
                if (vehicleMode.getLabel().equals("Auto"))
                {
                    if(isRestart)
                    {

                        gotoMissionItem(1);
                    }
                    else
                    {
                        gotoMissionItem(endValue);
                    }
                }
                else
                {
                 if(drone!=null)
                 {
                    Speed droneSpeed = drone.getAttribute(AttributeType.SPEED);
                    double speed_kts=0;
                    if(droneSpeed!=null)
                        speed_kts=droneSpeed.getGroundSpeed() * 1.94384;
                    if(speed_kts<15)
                    {
                        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                                .setCategory(GAUtils.Category.FLIGHT);
                        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.ROVER_AUTO,new AbstractCommandListener() {
                            @Override
                            public void onSuccess() {
                                if(isRestart)
                                {

                                    gotoMissionItem(1);
                                }
                                else
                                {
                                    gotoMissionItem(endValue);
                                }
                            }

                            @Override
                            public void onError(int i) {}

                            @Override
                            public void onTimeout() {}

                        });
                        eventBuilder.setAction("Flight mode changed").setLabel(VehicleMode.ROVER_AUTO.getLabel());
                        actionsBarFragment.setAutoModeActive(true);
                    }
                    else
                    {
                        actionsBarFragment.setAutoModeActive(false);
                    }
                     }
                }

                break;
            }
            case R.id.cancel: {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                FlightDataFragment flightDataFragment=FlightActivity.getFlightDataInstance();
                FlightMapFragment flightMapFragment=flightDataFragment.getFlightMapFragmentInstance();
                flightMapFragment.clearSelectionMarker();
                break;
            }
		}
	}

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(eventReceiver);
    }

    private void gotoMissionItem(final int waypoint){
        if(missionFinished || waypoint == 0){
            VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    MissionApi.getApi(drone).startMission(true, true, new AbstractCommandListener() {
                        @Override
                        public void onSuccess() {
                            MissionApi.getApi(drone).gotoWaypoint(waypoint, null);
                        }

                        @Override
                        public void onError(int i) {

                        }

                        @Override
                        public void onTimeout() {

                        }
                    });
                    missionFinished = false;
                }

                @Override
                public void onError(int i) {}

                @Override
                public void onTimeout() {}
            });
        }else{
            MissionApi.getApi(drone).gotoWaypoint(waypoint, null);
        }
    }

        private double getRemainingMissionLength(){
        Gps gps = drone.getAttribute(AttributeType.GPS);
        if(mission == null || mission.getMissionItems().size() == 0 || gps == null || !gps.isValid())
            return -1;
        LatLong dronePos = gps.getPosition();
        List<MissionItem> missionItems = mission.getMissionItems();
        List<LatLong> path = new ArrayList<LatLong>();
        path.add(dronePos);
        for(int i = Math.max(nextWaypoint - 1, 0); i < missionItems.size(); i++){
            MissionItem item = missionItems.get(i);
            if(item instanceof MissionItem.SpatialItem){
                MissionItem.SpatialItem spatialItem = (MissionItem.SpatialItem)item;
                LatLongAlt coordinate = spatialItem.getCoordinate();
                path.add(new LatLong(coordinate.getLatitude(), coordinate.getLongitude()));
            }

        }
        return MathUtils.getPolylineLength(path);
    }

    private double getTotalMissionLength(){
        List<MissionItem> missionItems = mission.getMissionItems();
        List<LatLong> path = new ArrayList<LatLong>();
        for(int i = 0; i < missionItems.size(); i++){
            MissionItem item = missionItems.get(i);
            if(item instanceof MissionItem.SpatialItem){
                MissionItem.SpatialItem spatialItem = (MissionItem.SpatialItem)item;
                LatLongAlt coordinate = spatialItem.getCoordinate();
                path.add(new LatLong(coordinate.getLatitude(), coordinate.getLongitude()));
            }

        }
        return MathUtils.getPolylineLength(path);
    }



    private void updateMission(){
        if(mission == null)
            return;
        double totalLength = getTotalMissionLength();
        missionProgress.setMax((int) totalLength);
        remainingMissionLength = getRemainingMissionLength();
        missionProgress.setProgress((int) ((totalLength - remainingMissionLength)));
        missionFinished = remainingMissionLength < 5;
    }

    @Override
	public void onAttach(Activity activity) {
		drone = ((DroidPlannerApp)activity.getApplication()).getDrone();
		super.onAttach(activity);
	}

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, Integer startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, Integer oldValue, Integer newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView cardWheel, Integer startValue, Integer endValue) {
        if(cardWheel.getId() == R.id.waypoint_selector) {
           // gotoMissionItem(endValue);
            this.endValue=endValue;
            final MissionProxy missionProxy = getMissionProxy();
            FlightDataFragment flightDataFragment=FlightActivity.getFlightDataInstance();
            FlightMapFragment flightMapFragment=flightDataFragment.getFlightMapFragmentInstance();

            if(missionProxy.getItems().size()!=0)
            {
//                missionProxy.selection.clearSelection();
//                missionProxy.selection.setSelectionTo(missionProxy.getItems().get(endValue-1));
                flightMapFragment.setMissionItemMarkerInfoSelection((MissionItemMarkerInfo)missionProxy.getMarkersInfos().get(endValue-1));

            }

        }
    }
        public static SlidingUpPanelLayout.PanelState getslidingUpPanelState()
        {
            if (slidingUpPanelLayout != null)
                return slidingUpPanelLayout.getPanelState();
            else
                return null;
        }
        public static boolean getIsRestartSelected()
        {
            return isRestart;
        }

        public static void setEndValue( Integer newEndValue)
        {
          endValue=newEndValue;
         }
}
