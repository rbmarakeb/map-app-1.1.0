package com.map.android.service.core.mission.commands;

import com.MAVLink.common.msg_mission_item;
import com.MAVLink.enums.MAV_CMD;

import com.map.android.service.core.mission.Mission;
import com.map.android.service.core.mission.MissionItemImpl;
import com.map.android.service.core.mission.MissionItemType;

import java.util.List;

/**
 * Mavlink message builder for the 'SetRelay' mission item.
 * Set a Relay pin’s voltage high or low.
 */
public class SetRelayImpl extends MissionCMD {

    private int relayNumber;
    private boolean enabled;

    public SetRelayImpl(MissionItemImpl item){
        super(item);
    }

    public SetRelayImpl(msg_mission_item msg, Mission mission){
        super(mission);
        unpackMAVMessage(msg);
    }

    public SetRelayImpl(Mission mission, int relayNumber, boolean enabled){
        super(mission);
        this.relayNumber = relayNumber;
        this.enabled = enabled;
    }

    @Override
    public MissionItemType getType(){
        return MissionItemType.SET_RELAY;
    }

    @Override
    public void unpackMAVMessage(msg_mission_item mavMsg){
        relayNumber = (int) mavMsg.param1;
        enabled = mavMsg.param2 != 0;
    }

    @Override
    public List<msg_mission_item> packMissionItem(){
        List<msg_mission_item> list = super.packMissionItem();
        msg_mission_item mavMsg = list.get(0);
        mavMsg.command = MAV_CMD.MAV_CMD_DO_SET_RELAY;
        mavMsg.param1 = relayNumber;
        mavMsg.param2 = enabled ? 1 : 0;
        return list;
    }

    public int getRelayNumber() {
        return relayNumber;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
