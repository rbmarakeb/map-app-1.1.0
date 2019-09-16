package com.map.android.service.core.drone.autopilot.apm;

import android.content.Context;
import android.os.Handler;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.enums.MAV_TYPE;

import com.map.android.service.communication.model.DataLink;
import com.map.android.service.core.drone.LogMessageListener;
import com.map.android.service.core.firmware.FirmwareType;
import com.map.android.service.core.model.AutopilotWarningParser;

/**
 * Created by Fredia Huya-Kouadio on 7/27/15.
 */
public class ArduRover extends ArduPilot {

    public ArduRover(String droneId, Context context, DataLink.DataLinkProvider<MAVLinkMessage> mavClient, Handler handler, AutopilotWarningParser warningParser, LogMessageListener logListener) {
        super(droneId, context, mavClient, handler, warningParser, logListener);
    }

    @Override
    public int getType(){
        return MAV_TYPE.MAV_TYPE_GROUND_ROVER;
    }

    @Override
    public void setType(int type){}

    @Override
    public FirmwareType getFirmwareType() {
        return FirmwareType.ARDU_ROVER;
    }
}
