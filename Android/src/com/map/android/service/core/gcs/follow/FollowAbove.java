package com.map.android.service.core.gcs.follow;

import android.os.Handler;

import com.map.android.lib.coordinate.LatLong;

import com.map.android.service.core.drone.manager.MavLinkDroneManager;
import com.map.android.service.core.gcs.location.Location;
import com.map.android.service.core.drone.autopilot.MavLinkDrone;

public class FollowAbove extends FollowAlgorithm {

    protected final MavLinkDrone drone;

    public FollowAbove(MavLinkDroneManager droneMgr, Handler handler) {
        super(droneMgr, handler);
        this.drone = droneMgr.getDrone();
    }

    @Override
    public FollowModes getType() {
        return FollowModes.ABOVE;
    }

    @Override
    protected void processNewLocation(Location location) {
        final LatLong gcsCoord = new LatLong(location.getCoord());
        drone.getGuidedPoint().newGuidedCoord(gcsCoord);
    }

}
