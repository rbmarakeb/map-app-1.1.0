package com.map.android.service.core.gcs.follow;

import android.os.Handler;

import com.map.android.lib.coordinate.LatLong;

import com.map.android.service.core.drone.manager.MavLinkDroneManager;
import com.map.android.service.core.gcs.location.Location;
import com.map.android.service.core.drone.autopilot.MavLinkDrone;

/**
 * Created by fhuya on 1/5/15.
 */
public class FollowSplineAbove extends FollowAlgorithm {

    private final MavLinkDrone drone;

    @Override
    public void processNewLocation(Location location) {
        LatLong gcsLoc = new LatLong(location.getCoord());

        //TODO: some device (nexus 6) do not report the speed (always 0).. figure out workaround.
        double speed = location.getSpeed();
        double bearing = location.getBearing();
        double bearingInRad = Math.toRadians(bearing);
        double xVel = speed * Math.cos(bearingInRad);
        double yVel = speed * Math.sin(bearingInRad);
        drone.getGuidedPoint().newGuidedCoordAndVelocity(gcsLoc, xVel, yVel, 0);
    }

    @Override
    public FollowModes getType() {
        return FollowModes.SPLINE_ABOVE;
    }

    public FollowSplineAbove(MavLinkDroneManager droneManager, Handler handler) {
        super(droneManager, handler);
        drone = droneManager.getDrone();
    }
}
