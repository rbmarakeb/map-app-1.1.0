package com.map.android.service.core.drone.autopilot;

import com.MAVLink.Messages.MAVLinkMessage;

import com.map.android.service.communication.model.DataLink;
import com.map.android.service.core.MAVLink.WaypointManager;
import com.map.android.service.core.drone.profiles.ParameterManager;
import com.map.android.service.core.drone.variables.Camera;
import com.map.android.service.core.drone.variables.GuidedPoint;
import com.map.android.service.core.drone.variables.MissionStats;
import com.map.android.service.core.drone.variables.State;
import com.map.android.service.core.drone.variables.StreamRates;
import com.map.android.service.core.drone.variables.calibration.AccelCalibration;
import com.map.android.service.core.drone.variables.calibration.MagnetometerCalibrationImpl;
import com.map.android.service.core.firmware.FirmwareType;
import com.map.android.service.core.mission.Mission;

public interface MavLinkDrone extends Drone {

    String PACKAGE_NAME = "com.map.android.service.core.drone.autopilot";

    String ACTION_REQUEST_HOME_UPDATE = PACKAGE_NAME + ".action.REQUEST_HOME_UPDATE";

    boolean isConnectionAlive();

    int getMavlinkVersion();

    void onMavLinkMessageReceived(MAVLinkMessage message);

    public byte getSysid();

    public byte getCompid();

    public State getState();

    public ParameterManager getParameterManager();

    public int getType();

    public FirmwareType getFirmwareType();

    public DataLink.DataLinkProvider<MAVLinkMessage> getMavClient();

    public WaypointManager getWaypointManager();

    public Mission getMission();

    public StreamRates getStreamRates();

    public MissionStats getMissionStats();

    public GuidedPoint getGuidedPoint();

    public AccelCalibration getCalibrationSetup();

    public MagnetometerCalibrationImpl getMagnetometerCalibration();

    public String getFirmwareVersion();

    public Camera getCamera();

}
