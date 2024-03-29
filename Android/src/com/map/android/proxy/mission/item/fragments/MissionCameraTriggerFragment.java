package com.map.android.proxy.mission.item.fragments;

import android.view.View;

import com.map.android.view.spinnerWheel.adapters.LengthWheelAdapter;
import com.map.android.lib.drone.mission.MissionItemType;
import com.map.android.lib.drone.mission.item.MissionItem;
import com.map.android.lib.drone.mission.item.command.CameraTrigger;

import org.beyene.sius.unit.length.LengthUnit;
import com.map.android.R;
import com.map.android.utils.Utils;
import com.map.android.utils.unit.providers.length.LengthUnitProvider;
import com.map.android.view.spinnerWheel.CardWheelHorizontalView;

public class MissionCameraTriggerFragment extends MissionDetailFragment implements
        CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit> {

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_camera_trigger;
    }

    @Override
    public void onApiConnected() {
        super.onApiConnected();

        final View view = getView();
        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.CAMERA_TRIGGER));

        CameraTrigger item = (CameraTrigger) getMissionItems().get(0);

        final LengthUnitProvider lengthUnitProvider = getLengthUnitProvider();
        final LengthWheelAdapter adapter = new LengthWheelAdapter(getContext(), R.layout.wheel_text_centered,
                lengthUnitProvider.boxBaseValueToTarget(Utils.MIN_DISTANCE),
                lengthUnitProvider.boxBaseValueToTarget(Utils.MAX_DISTANCE));
        final CardWheelHorizontalView<LengthUnit> cardAltitudePicker = (CardWheelHorizontalView<LengthUnit>) view
                .findViewById(R.id.picker1);
        cardAltitudePicker.setViewAdapter(adapter);
        cardAltitudePicker.addScrollListener(this);
        cardAltitudePicker.setCurrentValue(lengthUnitProvider.boxBaseValueToTarget(item.getTriggerDistance()));
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
            case R.id.picker1:
                double baseValue = endValue.toBase().getValue();
                for (MissionItem missionItem : getMissionItems()) {
                    CameraTrigger item = (CameraTrigger) missionItem;
                    item.setTriggerDistance(baseValue);
                }
                getMissionProxy().notifyMissionUpdate();
                break;
        }
    }
}
