package com.map.android.service.core.gcs.follow;

import android.os.Handler;

import com.map.android.service.core.drone.manager.MavLinkDroneManager;

public class FollowLead extends FollowHeadingAngle {

    public FollowLead(MavLinkDroneManager droneMgr, Handler handler, double radius) {
        super(droneMgr, handler, radius, 0.0);
    }

    @Override
    public FollowModes getType() {
        return FollowModes.LEAD;
    }

}
