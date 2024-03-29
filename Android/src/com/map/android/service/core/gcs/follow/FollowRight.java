package com.map.android.service.core.gcs.follow;

import android.os.Handler;

import com.map.android.service.core.drone.manager.MavLinkDroneManager;

public class FollowRight extends FollowHeadingAngle {

    public FollowRight(MavLinkDroneManager droneMgr, Handler handler, double radius) {
        super(droneMgr, handler, radius, 90.0);
    }

    @Override
    public FollowModes getType() {
        return FollowModes.RIGHT;
    }

}
