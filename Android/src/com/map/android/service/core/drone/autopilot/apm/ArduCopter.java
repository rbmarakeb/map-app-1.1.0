package com.map.android.service.core.drone.autopilot.apm;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.github.zafarkhaja.semver.Version;
import com.MAVLink.Messages.MAVLinkMessage;
import com.map.android.client.apis.CapabilityApi;
import com.map.android.lib.drone.action.ControlActions;
import com.map.android.lib.drone.attribute.error.CommandExecutionError;
import com.map.android.lib.drone.property.Parameter;
import com.map.android.lib.model.ICommandListener;

import com.map.android.service.communication.model.DataLink;
import com.map.android.service.core.MAVLink.MavLinkCommands;
import com.map.android.service.core.drone.DroneInterfaces;
import com.map.android.service.core.drone.DroneManager;
import com.map.android.service.core.drone.LogMessageListener;
import com.map.android.service.core.drone.profiles.ParameterManager;
import com.map.android.service.core.drone.variables.ApmModes;
import com.map.android.service.core.drone.variables.State;
import com.map.android.service.core.firmware.FirmwareType;
import com.map.android.service.core.model.AutopilotWarningParser;
import com.map.android.service.utils.CommonApiUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Fredia Huya-Kouadio on 7/27/15.
 */
public class ArduCopter extends ArduPilot {
    private static final Version BRAKE_FEATURE_FIRMWARE_VERSION = Version.forIntegers(3, 3, 0);

    private final ConcurrentHashMap<String, ICommandListener> manualControlStateListeners = new ConcurrentHashMap<>();

    public ArduCopter(String droneId, Context context, DataLink.DataLinkProvider<MAVLinkMessage> mavClient, Handler handler, AutopilotWarningParser warningParser, LogMessageListener logListener) {
        super(droneId, context, mavClient, handler, warningParser, logListener);
    }

    @Override
    public FirmwareType getFirmwareType() {
        return FirmwareType.ARDU_COPTER;
    }

    @Override
    protected boolean setVelocity(Bundle data, ICommandListener listener){
        //Retrieve the normalized values
        float normalizedXVel = data.getFloat(ControlActions.EXTRA_VELOCITY_X);
        float normalizedYVel = data.getFloat(ControlActions.EXTRA_VELOCITY_Y);
        float normalizedZVel = data.getFloat(ControlActions.EXTRA_VELOCITY_Z);

        double attitudeInRad = Math.toRadians(attitude.getYaw());

        final double cosAttitude = Math.cos(attitudeInRad);
        final double sinAttitude = Math.sin(attitudeInRad);

        float projectedX = (float) (normalizedXVel * cosAttitude) - (float) (normalizedYVel * sinAttitude);
        float projectedY = (float) (normalizedXVel * sinAttitude) + (float) (normalizedYVel * cosAttitude);

        //Retrieve the speed parameters.
        float defaultSpeed = 5; //m/s

        ParameterManager parameterManager = getParameterManager();

        //Retrieve the horizontal speed value
        Parameter horizSpeedParam = parameterManager.getParameter("WPNAV_SPEED");
        double horizontalSpeed = horizSpeedParam == null ? defaultSpeed : horizSpeedParam.getValue() / 100;

        //Retrieve the vertical speed value.
        String vertSpeedParamName = normalizedZVel >= 0 ? "WPNAV_SPEED_UP" : "WPNAV_SPEED_DN";
        Parameter vertSpeedParam = parameterManager.getParameter(vertSpeedParamName);
        double verticalSpeed = vertSpeedParam == null ? defaultSpeed : vertSpeedParam.getValue() / 100;

        MavLinkCommands.setVelocityInLocalFrame(this, (float) (projectedX * horizontalSpeed),
                (float) (projectedY * horizontalSpeed),
                (float) (normalizedZVel * verticalSpeed),
                listener);
        return true;
    }

    @Override
    public void destroy(){
        super.destroy();
        manualControlStateListeners.clear();
    }

    @Override
    protected boolean enableManualControl(Bundle data, ICommandListener listener){
        boolean enable = data.getBoolean(ControlActions.EXTRA_DO_ENABLE);
        String appId = data.getString(DroneManager.EXTRA_CLIENT_APP_ID);

        State state = getState();
        ApmModes vehicleMode = state.getMode();
        if(enable){
            if(vehicleMode == ApmModes.ROTOR_GUIDED){
                CommonApiUtils.postSuccessEvent(listener);
            }
            else{
                state.changeFlightMode(ApmModes.ROTOR_GUIDED, listener);
            }

            if(listener != null) {
                manualControlStateListeners.put(appId, listener);
            }
        }
        else{
            manualControlStateListeners.remove(appId);

            if(vehicleMode != ApmModes.ROTOR_GUIDED){
                CommonApiUtils.postSuccessEvent(listener);
            }
            else{
                state.changeFlightMode(ApmModes.ROTOR_LOITER, listener);
            }
        }

        return true;
    }

    @Override
    public void notifyDroneEvent(DroneInterfaces.DroneEventsType event){
        switch(event){
            case MODE:
                //Listen for vehicle mode updates, and update the manual control state listeners appropriately
                ApmModes currentMode = getState().getMode();
                for(ICommandListener listener: manualControlStateListeners.values()) {
                    if (currentMode == ApmModes.ROTOR_GUIDED) {
                        CommonApiUtils.postSuccessEvent(listener);
                    } else {
                        CommonApiUtils.postErrorEvent(CommandExecutionError.COMMAND_FAILED, listener);
                    }
                }
                break;
        }

        super.notifyDroneEvent(event);
    }

    @Override
    protected boolean isFeatureSupported(String featureId){
        switch(featureId){

            case CapabilityApi.FeatureIds.KILL_SWITCH:
                return CommonApiUtils.isKillSwitchSupported(this);

            default:
                return super.isFeatureSupported(featureId);
        }
    }

    @Override
    protected boolean brakeVehicle(ICommandListener listener) {
        if (getFirmwareVersionNumber().greaterThanOrEqualTo(BRAKE_FEATURE_FIRMWARE_VERSION)) {
            getState().changeFlightMode(ApmModes.ROTOR_BRAKE, listener);
        } else {
            super.brakeVehicle(listener);
        }

        return true;
    }
}
