package com.map.android.service.core.mission.survey;

import com.MAVLink.common.msg_mission_item;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_FRAME;
import com.map.android.lib.coordinate.LatLong;

import com.map.android.service.core.mission.Mission;
import com.map.android.service.core.mission.MissionItemImpl;
import com.map.android.service.core.mission.MissionItemType;
import com.map.android.service.core.mission.commands.CameraTriggerImpl;
import com.map.android.service.core.polygon.Polygon;
import com.map.android.service.core.survey.CameraInfo;
import com.map.android.service.core.survey.SurveyData;
import com.map.android.service.core.survey.grid.Grid;
import com.map.android.service.core.survey.grid.GridBuilder;

import java.util.ArrayList;
import java.util.List;

public class SurveyImpl extends MissionItemImpl {

    public Polygon polygon = new Polygon();
    public SurveyData surveyData = new SurveyData();
    public Grid grid;

    private boolean startCameraBeforeFirstWaypoint;

    public SurveyImpl(Mission mission, List<LatLong> points) {
        super(mission);
        polygon.addPoints(points);
    }

    public void update(double angle, double altitude, double overlap, double sidelap) {
        surveyData.update(angle, altitude, overlap, sidelap);
    }

    public boolean isStartCameraBeforeFirstWaypoint() {
        return startCameraBeforeFirstWaypoint;
    }

    public void setStartCameraBeforeFirstWaypoint(boolean startCameraBeforeFirstWaypoint) {
        this.startCameraBeforeFirstWaypoint = startCameraBeforeFirstWaypoint;
    }

    public void setCameraInfo(CameraInfo camera) {
        surveyData.setCameraInfo(camera);
    }

    public void build() throws Exception {
        // TODO find better point than (0,0) to reference the grid
        grid = null;
        GridBuilder gridBuilder = new GridBuilder(polygon, surveyData, new LatLong(0, 0));
        polygon.checkIfValid();
        grid = gridBuilder.generate(true);
    }

    @Override
    public List<msg_mission_item> packMissionItem() {
        try {
            List<msg_mission_item> list = new ArrayList<msg_mission_item>();
            build();

            packSurveyPoints(list);

            return list;
        } catch (Exception e) {
            return new ArrayList<msg_mission_item>();
        }
    }

    private void packSurveyPoints(List<msg_mission_item> list) {
        //Generate the camera trigger
        CameraTriggerImpl camTrigger = new CameraTriggerImpl(mission, surveyData.getLongitudinalPictureDistance());

        //Add it if the user wants it to start before the first waypoint.
        if(startCameraBeforeFirstWaypoint){
            list.addAll(camTrigger.packMissionItem());
        }

        final double altitude = surveyData.getAltitude();

        //Add the camera trigger after the first waypoint if it wasn't added before.
        boolean addToFirst = !startCameraBeforeFirstWaypoint;

        for (LatLong point : grid.gridPoints) {
            msg_mission_item mavMsg = getSurveyPoint(point, altitude);
            list.add(mavMsg);

            if(addToFirst){
                list.addAll(camTrigger.packMissionItem());
                addToFirst = false;
            }
        }

        list.addAll((new CameraTriggerImpl(mission, (0.0)).packMissionItem()));
    }

    protected msg_mission_item getSurveyPoint(LatLong point, double altitude){
        return packSurveyPoint(point, altitude);
    }

    public static msg_mission_item packSurveyPoint(LatLong point, double altitude) {
        msg_mission_item mavMsg = new msg_mission_item();
        mavMsg.autocontinue = 1;
        mavMsg.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
        mavMsg.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT;
        mavMsg.x = (float) point.getLatitude();
        mavMsg.y = (float) point.getLongitude();
        mavMsg.z = (float) altitude;
        mavMsg.param1 = 0f;
        mavMsg.param2 = 0f;
        mavMsg.param3 = 0f;
        mavMsg.param4 = 0f;
        return mavMsg;
    }

    @Override
    public void unpackMAVMessage(msg_mission_item mavMsg) {
        // TODO Auto-generated method stub

    }

    @Override
    public MissionItemType getType() {
        return MissionItemType.SURVEY;
    }

}
