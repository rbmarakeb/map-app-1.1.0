package com.map.android.proxy.mission.item.fragments;

import android.content.Context;
import android.view.View;

import com.map.android.utils.unit.providers.length.LengthUnitProvider;
import com.map.android.view.spinnerWheel.adapters.LengthWheelAdapter;
import com.map.android.view.spinnerWheel.adapters.NumericWheelAdapter;
import com.map.android.lib.drone.mission.MissionItemType;
import com.map.android.lib.drone.mission.item.MissionItem;
import com.map.android.lib.drone.mission.item.spatial.SplineWaypoint;

import org.beyene.sius.unit.length.LengthUnit;
import com.map.android.R;

import com.map.android.view.spinnerWheel.CardWheelHorizontalView;

/**
 * This class renders the detail view for a spline waypoint mission item.
 */
public class MissionSplineWaypointFragment extends MissionDetailFragment implements
        CardWheelHorizontalView.OnCardWheelScrollListener {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_spline_waypoint;
    }

    @Override
    public void onApiConnected() {
        super.onApiConnected();

        final View view = getView();
        final Context context = getContext();

        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.SPLINE_WAYPOINT));

        final NumericWheelAdapter delayAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, 0,
                60, "%d s");
        CardWheelHorizontalView<Integer> delayPicker = (CardWheelHorizontalView<Integer>) view.findViewById(R.id
                .waypointDelayPicker);
        delayPicker.setViewAdapter(delayAdapter);
        delayPicker.addScrollListener(this);

        final LengthUnitProvider lengthUP = getLengthUnitProvider();
        final LengthWheelAdapter altitudeAdapter = new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE));
        CardWheelHorizontalView<LengthUnit> altitudePicker = (CardWheelHorizontalView<LengthUnit>) view.findViewById
                (R.id.altitudePicker);
        altitudePicker.setViewAdapter(altitudeAdapter);
        altitudePicker.addScrollListener(this);

        SplineWaypoint item = (SplineWaypoint) getMissionItems().get(0);
        delayPicker.setCurrentValue((int) item.getDelay());
        altitudePicker.setCurrentValue(lengthUP.boxBaseValueToTarget(item.getCoordinate().getAltitude()));
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, Object startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, Object oldValue, Object newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView wheel, Object startValue, Object endValue) {
        switch (wheel.getId()) {
            case R.id.altitudePicker:
                final double baseValue = ((LengthUnit) endValue).toBase().getValue();
                for (MissionItem item : getMissionItems()) {
                    ((SplineWaypoint) item).getCoordinate().setAltitude(baseValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;

            case R.id.waypointDelayPicker:
                final int delay = (Integer) endValue;
                for (MissionItem item : getMissionItems()) {
                    ((SplineWaypoint) item).setDelay(delay);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
