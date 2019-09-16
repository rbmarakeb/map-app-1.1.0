package com.map.android.proxy.mission.item.fragments;

import android.view.View;

import com.map.android.lib.drone.mission.MissionItemType;
import com.map.android.lib.drone.mission.item.MissionItem;
import com.map.android.lib.drone.mission.item.spatial.RegionOfInterest;

import org.beyene.sius.unit.length.LengthUnit;
import com.map.android.R;
import com.map.android.utils.unit.providers.length.LengthUnitProvider;
import com.map.android.view.spinnerWheel.CardWheelHorizontalView;
import com.map.android.view.spinnerWheel.adapters.LengthWheelAdapter;

public class MissionRegionOfInterestFragment extends MissionDetailFragment implements
        CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit> {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_roi;
    }

    @Override
    public void onApiConnected() {
        super.onApiConnected();

        final View view = getView();
        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.REGION_OF_INTEREST));

        final LengthUnitProvider lengthUP = getLengthUnitProvider();
        final LengthWheelAdapter altitudeAdapter = new LengthWheelAdapter(getContext(), R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE));
        CardWheelHorizontalView<LengthUnit> altitudePicker = (CardWheelHorizontalView<LengthUnit>) view
                .findViewById(R.id.altitudePicker);
        altitudePicker.setViewAdapter(altitudeAdapter);
        altitudePicker.addScrollListener(this);

        altitudePicker.setCurrentValue(lengthUP.boxBaseValueToTarget(((RegionOfInterest) getMissionItems().get(0))
                .getCoordinate().getAltitude()));
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, LengthUnit startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, LengthUnit oldValue, LengthUnit newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView wheel, LengthUnit startValue, LengthUnit endValue) {
        switch (wheel.getId()) {
            case R.id.altitudePicker:
                final double baseValue = endValue.toBase().getValue();
                for (MissionItem missionItem : getMissionItems()) {
                    ((RegionOfInterest) missionItem).getCoordinate().setAltitude(baseValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
