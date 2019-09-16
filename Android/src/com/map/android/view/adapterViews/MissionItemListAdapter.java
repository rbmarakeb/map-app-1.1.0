package com.map.android.view.adapterViews;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.map.android.proxy.mission.item.MissionItemProxy;
import com.map.android.lib.drone.mission.item.MissionItem;
import com.map.android.lib.drone.mission.item.command.CameraTrigger;
import com.map.android.lib.drone.mission.item.command.ChangeSpeed;
import com.map.android.lib.drone.mission.item.command.EpmGripper;
import com.map.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.map.android.lib.drone.mission.item.command.SetServo;
import com.map.android.lib.drone.mission.item.command.Takeoff;
import com.map.android.lib.drone.mission.item.command.YawCondition;
import com.map.android.lib.drone.mission.item.complex.SplineSurvey;
import com.map.android.lib.drone.mission.item.complex.Survey;
import com.map.android.lib.drone.mission.item.spatial.Circle;
import com.map.android.lib.drone.mission.item.spatial.Land;
import com.map.android.lib.drone.mission.item.spatial.RegionOfInterest;
import com.map.android.lib.drone.mission.item.spatial.SplineWaypoint;

import org.beyene.sius.unit.composition.speed.SpeedUnit;
import org.beyene.sius.unit.length.LengthUnit;
import com.map.android.R;
import com.map.android.activities.interfaces.OnEditorInteraction;
import com.map.android.proxy.mission.MissionProxy;
import com.map.android.utils.ReorderRecyclerView;
import com.map.android.utils.unit.UnitManager;
import com.map.android.utils.unit.providers.length.LengthUnitProvider;
import com.map.android.utils.unit.providers.speed.SpeedUnitProvider;
import com.map.android.utils.unit.systems.UnitSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fhuya on 12/9/14.
 */
public class MissionItemListAdapter extends ReorderRecyclerView.ReorderAdapter<MissionItemListAdapter.ViewHolder> {

    // Provide a reference to the views for each data item
    public static class ViewHolder extends RecyclerView.ViewHolder {

        final View viewContainer;
        final TextView nameView;
//        final TextView altitudeView;

        public ViewHolder(View container, TextView nameView, TextView altitudeView) {
            super(container);
            this.viewContainer = container;
            this.nameView = nameView;
//            this.altitudeView = altitudeView;
        }

    }

    public List<MissionItemProxy> missionItemProxyList;
    private final MissionProxy missionProxy;
    private final OnEditorInteraction editorListener;
    private final LengthUnitProvider lengthUnitProvider;
    private final SpeedUnitProvider speedUnitProvider;

    public MissionItemListAdapter(Context context, MissionProxy missionProxy, OnEditorInteraction editorListener) {
        this.missionProxy = missionProxy;
        this.editorListener = editorListener;
        this.missionItemProxyList = new ArrayList<>();
        this.missionItemProxyList.addAll(missionProxy.getItems());
        if (missionItemProxyList.size() == 0) {
            missionItemProxyList.add(new MissionItemProxy());
        }

        final UnitSystem unitSystem = UnitManager.getUnitSystem(context);
        this.lengthUnitProvider = unitSystem.getLengthUnitProvider();
        this.speedUnitProvider = unitSystem.getSpeedUnitProvider();
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position){
        return missionItemProxyList.get(position).getStableId();
    }

    @Override
    public int getItemCount() {
        return missionItemProxyList.size();
    }

    @Override
    public void swapElements(int fromIndex, int toIndex) {
        if(isIndexValid(fromIndex) && isIndexValid(toIndex)) {
            missionProxy.swap(fromIndex, toIndex);
        }
    }

    private boolean isIndexValid(int index){
        return index >= 0 && index < getItemCount();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_editor_list_item, parent, false);

        final TextView nameView = (TextView) view.findViewById(R.id.rowNameView);
//        final TextView altitudeView = (TextView) view.findViewById(R.id.rowAltitudeView);

//        return new ViewHolder(view, nameView, altitudeView);
        return new ViewHolder(view, nameView, null);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        final MissionItemProxy proxy = missionItemProxyList.get(position);
        if (proxy.getMission() == null) { // Add POI
            final View container = viewHolder.viewContainer;
            container.setBackgroundResource(R.drawable.ic_mission_add_poi_button);
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(editorListener != null)
                        editorListener.onItemClickToAddPOI();
                }
            });

            final TextView nameView = viewHolder.nameView;
            nameView.setText("+");
        } else {
            final View container = viewHolder.viewContainer;
            container.setBackgroundResource(R.drawable.ic_mission_button);
            container.setActivated(missionProxy.selection.selectionContains(proxy));
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(editorListener != null)
                        editorListener.onItemClick(proxy, true);
                }
            });

            final TextView nameView = viewHolder.nameView;
//        final TextView altitudeView = viewHolder.altitudeView;

            final MissionProxy missionProxy = proxy.getMission();
            final MissionItem missionItem = proxy.getMissionItem();

            nameView.setText(String.format("%3d", missionProxy.getOrder(proxy)));

            int leftDrawable;

            // Spatial item's icons
            if (missionItem instanceof MissionItem.SpatialItem) {
                if (missionItem instanceof SplineWaypoint) {
                    leftDrawable = R.drawable.ic_mission_spline_wp;
                } else if (missionItem instanceof Circle) {
                    leftDrawable = R.drawable.ic_mission_circle_wp;
                } else if (missionItem instanceof RegionOfInterest) {
                    leftDrawable = R.drawable.ic_mission_roi_wp;
                } else if (missionItem instanceof Land) {
                    leftDrawable = R.drawable.ic_mission_land_wp;
                } else {
                    leftDrawable = R.drawable.ic_mission_wp;
                }
                // Command icons
            } else if (missionItem instanceof MissionItem.Command) {
                if (missionItem instanceof CameraTrigger) {
                    leftDrawable = R.drawable.ic_mission_camera_trigger_wp;
                } else if (missionItem instanceof ChangeSpeed) {
                    leftDrawable = R.drawable.ic_mission_change_speed_wp;
                } else if (missionItem instanceof EpmGripper) {
                    leftDrawable = R.drawable.ic_mission_epm_gripper_wp;
                } else if (missionItem instanceof ReturnToLaunch) {
                    leftDrawable = R.drawable.ic_mission_rtl_wp;
                } else if (missionItem instanceof SetServo) {
                    leftDrawable = R.drawable.ic_mission_set_servo_wp;
                } else if (missionItem instanceof Takeoff) {
                    leftDrawable = R.drawable.ic_mission_takeoff_wp;
                } else if (missionItem instanceof YawCondition) {
                    leftDrawable = R.drawable.ic_mission_yaw_cond_wp;
                } else {
                    leftDrawable = R.drawable.ic_mission_command_wp;
                }
                // Complex item's icons
                // TODO CameraDetail (inconvertible type) and StructureScanner (condition always false) WPs
            } else if (missionItem instanceof MissionItem.ComplexItem) {
                if (missionItem instanceof SplineSurvey) {
                    leftDrawable = R.drawable.ic_mission_spline_survey_wp;
                } else if (missionItem instanceof Survey) {
                    leftDrawable = R.drawable.ic_mission_survey_wp;
                } else {
                    leftDrawable = R.drawable.ic_mission_command_wp;
                }
                // Fallback icon
            } else {
                leftDrawable = R.drawable.ic_mission_wp;
            }
        }

//        altitudeView.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, 0, 0, 0);
//
//        if (missionItem instanceof MissionItem.SpatialItem) {
//            MissionItem.SpatialItem waypoint = (MissionItem.SpatialItem) missionItem;
//            double altitude = waypoint.getCoordinate().getAltitude();
//            LengthUnit convertedAltitude = lengthUnitProvider.boxBaseValueToTarget(altitude);
//            LengthUnit roundedConvertedAltitude = (LengthUnit) convertedAltitude.valueOf(Math.round(convertedAltitude.getValue()));
//            altitudeView.setText(roundedConvertedAltitude.toString());
//
//            if (altitude < 0)
//                altitudeView.setTextColor(Color.YELLOW);
//            else
//                altitudeView.setTextColor(Color.WHITE);
//
//        } else if (missionItem instanceof Survey) {
//            double altitude = ((Survey) missionItem).getSurveyDetail().getAltitude();
//            LengthUnit convertedAltitude = lengthUnitProvider.boxBaseValueToTarget(altitude);
//            LengthUnit roundedConvertedAltitude = (LengthUnit) convertedAltitude.valueOf(Math.round(convertedAltitude.getValue()));
//            altitudeView.setText(roundedConvertedAltitude.toString());
//
//            if (altitude < 0)
//                altitudeView.setTextColor(Color.YELLOW);
//            else
//                altitudeView.setTextColor(Color.WHITE);
//
//        } else if (missionItem instanceof Takeoff) {
//            double altitude = ((Takeoff) missionItem).getTakeoffAltitude();
//            LengthUnit convertedAltitude = lengthUnitProvider.boxBaseValueToTarget(altitude);
//            LengthUnit roundedConvertedAltitude = (LengthUnit) convertedAltitude.valueOf(Math.round(convertedAltitude.getValue()));
//            altitudeView.setText(roundedConvertedAltitude.toString());
//
//            if (altitude < 0)
//                altitudeView.setTextColor(Color.YELLOW);
//            else
//                altitudeView.setTextColor(Color.WHITE);
//        }else if(missionItem instanceof ChangeSpeed){
//            final double speed = ((ChangeSpeed) missionItem).getSpeed();
//            final SpeedUnit convertedSpeed = speedUnitProvider.boxBaseValueToTarget(speed);
//            altitudeView.setText(convertedSpeed.toString());
//        }
//        else {
//            altitudeView.setText("");
//        }
    }
}
