package com.map.android.service.core.gcs.follow;

import android.os.Handler;

import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.Gps;

import com.map.android.service.core.drone.manager.MavLinkDroneManager;
import com.map.android.service.core.gcs.location.Location;
import com.map.android.service.core.helpers.geoTools.GeoTools;

public class FollowLeash extends FollowWithRadiusAlgorithm {

    public FollowLeash(MavLinkDroneManager droneMgr, Handler handler, double radius) {
        super(droneMgr, handler, radius);
    }

    @Override
    public FollowModes getType() {
        return FollowModes.LEASH;
    }

    @Override
    protected void processNewLocation(Location location) {
        final LatLong locationCoord = location.getCoord();

        final Gps droneGps = (Gps) drone.getAttribute(AttributeType.GPS);
        final LatLong dronePosition = droneGps.getPosition();

        if (locationCoord == null || dronePosition == null) {
            return;
        }

        if (GeoTools.getDistance(locationCoord, dronePosition) > radius) {
            double headingGCStoDrone = GeoTools.getHeadingFromCoordinates(locationCoord, dronePosition);
            LatLong goCoord = GeoTools.newCoordFromBearingAndDistance(locationCoord, headingGCStoDrone, radius);
            drone.getGuidedPoint().newGuidedCoord(goCoord);
        }
    }

}
