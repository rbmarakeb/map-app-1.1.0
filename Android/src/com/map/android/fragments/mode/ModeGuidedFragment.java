package com.map.android.fragments.mode;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.map.android.fragments.FlightMapFragment;
import com.map.android.fragments.OnGuidedClickListener;
import com.map.android.fragments.RemoteHelmDataFragment;
import com.map.android.utils.prefs.DroidPlannerPrefs;
import com.map.android.client.Drone;
import com.map.android.client.apis.ControlApi;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.coordinate.LatLongAlt;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.GuidedState;
import com.map.android.lib.drone.property.Type;

import org.beyene.sius.unit.length.LengthUnit;
import com.map.android.R;
import com.map.android.fragments.FlightDataFragment;
import com.map.android.fragments.helpers.ApiListenerFragment;
import com.map.android.utils.unit.providers.length.LengthUnitProvider;
import com.map.android.view.spinnerWheel.CardWheelHorizontalView;
import com.map.android.view.spinnerWheel.adapters.LengthWheelAdapter;

public class ModeGuidedFragment extends ApiListenerFragment implements
        CardWheelHorizontalView.OnCardWheelScrollListener<LengthUnit>, OnGuidedClickListener {

    private CardWheelHorizontalView<LengthUnit> mAltitudeWheel;
    protected ApiListenerFragment parent;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        final Fragment parentFragment = getParentFragment().getParentFragment();
        if(parentFragment instanceof FlightDataFragment) {
            parent = (FlightDataFragment) parentFragment;
        } else if (parentFragment instanceof RemoteHelmDataFragment) {
            parent = (RemoteHelmDataFragment) parentFragment;
        } else {
            throw new IllegalStateException("Parent fragment must be an instance of " + FlightDataFragment.class.getName());
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        parent = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mode_guided, container, false);
    }

    @Override
    public void onViewCreated(View parentView, Bundle savedInstanceState) {
        super.onViewCreated(parentView, savedInstanceState);

        final DroidPlannerPrefs dpPrefs = getAppPrefs();

        final LengthUnitProvider lengthUnitProvider = getLengthUnitProvider();
        final LengthWheelAdapter altitudeAdapter = new LengthWheelAdapter(getContext(), R.layout.wheel_text_centered,
                lengthUnitProvider.boxBaseValueToTarget(dpPrefs.getMinAltitude()),
                lengthUnitProvider.boxBaseValueToTarget(dpPrefs.getMaxAltitude()));

        mAltitudeWheel = (CardWheelHorizontalView<LengthUnit>) parentView.findViewById(R.id.altitude_spinner);
        mAltitudeWheel.setViewAdapter(altitudeAdapter);
        mAltitudeWheel.addScrollListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAltitudeWheel != null) {
            mAltitudeWheel.removeChangingListener(this);
        }
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, LengthUnit startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, LengthUnit oldValue, LengthUnit newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView cardWheel, LengthUnit startValue, LengthUnit endValue) {
        switch (cardWheel.getId()) {
            case R.id.altitude_spinner:
                final Drone drone = getDrone();
                if (drone.isConnected()) {
                    ControlApi.getApi(drone).climbTo(endValue.toBase().getValue());
                }
                break;
        }
    }

    @Override
    public void onApiConnected() {
        final Drone drone = getDrone();

        if (mAltitudeWheel != null) {
            final DroidPlannerPrefs dpPrefs = getAppPrefs();

            final double maxAlt = dpPrefs.getMaxAltitude();
            final double minAlt = dpPrefs.getMinAltitude();
            final double defaultAlt = dpPrefs.getDefaultAltitude();

            GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
            LatLongAlt coordinate = guidedState == null ? null : guidedState.getCoordinate();

            final double baseValue = Math.min(maxAlt,
                    Math.max(minAlt, coordinate == null ? defaultAlt : coordinate.getAltitude()));
            final LengthUnit initialValue = getLengthUnitProvider().boxBaseValueToTarget(baseValue);
            mAltitudeWheel.setCurrentValue(initialValue);
        }

        if(parent instanceof FlightDataFragment) {
            ((FlightDataFragment) parent).setGuidedClickListener(this);
        } else if(parent instanceof RemoteHelmDataFragment) {
            ((RemoteHelmDataFragment) parent).setGuidedClickListener(this);
        }
        Type droneType = drone.getAttribute(AttributeType.TYPE);
        if (droneType.getDroneType() == Type.TYPE_ROVER) {
            mAltitudeWheel.setVisibility(View.GONE);
        } else {
            mAltitudeWheel.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onApiDisconnected() {
        if(parent instanceof FlightDataFragment) {
            ((FlightDataFragment) parent).setGuidedClickListener(null);
        } else if(parent instanceof RemoteHelmDataFragment) {
            ((RemoteHelmDataFragment) parent).setGuidedClickListener(null);
        }
    }

    @Override
    public void onGuidedClick(LatLong coord) {
        final Drone drone = getDrone();
        if (drone != null) {
            ControlApi.getApi(drone).goTo(coord, false, null);
        }
    }
}
