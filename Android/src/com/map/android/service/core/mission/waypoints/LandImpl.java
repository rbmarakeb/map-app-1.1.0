package com.map.android.service.core.mission.waypoints;

import com.MAVLink.common.msg_mission_item;
import com.MAVLink.enums.MAV_CMD;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.coordinate.LatLongAlt;

import com.map.android.service.core.mission.Mission;
import com.map.android.service.core.mission.MissionItemImpl;
import com.map.android.service.core.mission.MissionItemType;

import java.util.List;

public class LandImpl extends SpatialCoordItem {

    public LandImpl(MissionItemImpl item) {
        super(item);
        setAltitude((0.0));
    }

    public LandImpl(Mission mission) {
        this(mission, new LatLong(0, 0));
    }

    public LandImpl(Mission mMission, LatLong coord) {
        super(mMission, new LatLongAlt(coord, 0));
    }

    public LandImpl(msg_mission_item msg, Mission mission) {
        super(mission, null);
        unpackMAVMessage(msg);
    }


    @Override
    public List<msg_mission_item> packMissionItem() {
        List<msg_mission_item> list = super.packMissionItem();
        msg_mission_item mavMsg = list.get(0);
        mavMsg.command = MAV_CMD.MAV_CMD_NAV_LAND;
        return list;
    }

    @Override
    public void unpackMAVMessage(msg_mission_item mavMsg) {
        super.unpackMAVMessage(mavMsg);
    }

    @Override
    public MissionItemType getType() {
        return MissionItemType.LAND;
    }

}