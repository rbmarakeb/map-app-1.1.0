package com.map.android.service.core.drone.autopilot.px4;

import android.content.Context;
import android.os.Handler;

import com.MAVLink.Messages.MAVLinkMessage;

import com.map.android.service.communication.model.DataLink;
import com.map.android.service.core.drone.LogMessageListener;
import com.map.android.service.core.drone.autopilot.generic.GenericMavLinkDrone;
import com.map.android.service.core.firmware.FirmwareType;
import com.map.android.service.core.model.AutopilotWarningParser;

/**
 * Created by Fredia Huya-Kouadio on 9/10/15.
 */
public class Px4Native extends GenericMavLinkDrone {

    public Px4Native(String droneId, Context context, Handler handler, DataLink.DataLinkProvider<MAVLinkMessage> mavClient, AutopilotWarningParser warningParser, LogMessageListener logListener) {
        super(droneId, context, handler, mavClient, warningParser, logListener);
    }

    @Override
    public FirmwareType getFirmwareType() {
        return FirmwareType.PX4_NATIVE;
    }

}
