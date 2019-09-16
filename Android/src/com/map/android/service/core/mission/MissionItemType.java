package com.map.android.service.core.mission;

import com.map.android.lib.coordinate.LatLong;

import com.map.android.service.core.mission.commands.CameraTriggerImpl;
import com.map.android.service.core.mission.commands.ChangeSpeedImpl;
import com.map.android.service.core.mission.commands.ConditionYawImpl;
import com.map.android.service.core.mission.commands.DoJumpImpl;
import com.map.android.service.core.mission.commands.EpmGripperImpl;
import com.map.android.service.core.mission.commands.ReturnToHomeImpl;
import com.map.android.service.core.mission.commands.SetRelayImpl;
import com.map.android.service.core.mission.commands.SetServoImpl;
import com.map.android.service.core.mission.commands.TakeoffImpl;
import com.map.android.service.core.mission.survey.SplineSurveyImpl;
import com.map.android.service.core.mission.survey.SurveyImpl;
import com.map.android.service.core.mission.waypoints.CircleImpl;
import com.map.android.service.core.mission.waypoints.DoLandStartImpl;
import com.map.android.service.core.mission.waypoints.LandImpl;
import com.map.android.service.core.mission.waypoints.RegionOfInterestImpl;
import com.map.android.service.core.mission.waypoints.SplineWaypointImpl;
import com.map.android.service.core.mission.waypoints.StructureScannerImpl;
import com.map.android.service.core.mission.waypoints.WaypointImpl;

import java.util.Collections;

public enum MissionItemType {
    WAYPOINT("Waypoint"),
    SPLINE_WAYPOINT("Spline Waypoint"),
    TAKEOFF("Takeoff"),
    RTL("Return to Launch"),
    LAND("Land"),
    CIRCLE("Circle"),
    ROI("Region of Interest"),
    SURVEY("Survey"),
    SPLINE_SURVEY("Spline Survey"),
    CYLINDRICAL_SURVEY("Structure Scan"),
    CHANGE_SPEED("Change Speed"),
    CAMERA_TRIGGER("Camera Trigger"),
    EPM_GRIPPER("EPM"),
    SET_SERVO("Set Servo"),
    CONDITION_YAW("Set Yaw"),
    SET_RELAY("Set Relay"),
    DO_LAND_START("Do Land Start"),
    DO_JUMP("Do Jump");

    private final String name;

    private MissionItemType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public MissionItemImpl getNewItem(MissionItemImpl referenceItem) throws IllegalArgumentException {
        switch (this) {
            case WAYPOINT:
                return new WaypointImpl(referenceItem);
            case SPLINE_WAYPOINT:
                return new SplineWaypointImpl(referenceItem);
            case TAKEOFF:
                return new TakeoffImpl(referenceItem);
            case CHANGE_SPEED:
                return new ChangeSpeedImpl(referenceItem);
            case CAMERA_TRIGGER:
                return new CameraTriggerImpl(referenceItem);
            case EPM_GRIPPER:
                return new EpmGripperImpl(referenceItem);
            case RTL:
                return new ReturnToHomeImpl(referenceItem);
            case LAND:
                return new LandImpl(referenceItem);
            case CIRCLE:
                return new CircleImpl(referenceItem);
            case ROI:
                return new RegionOfInterestImpl(referenceItem);
            case SURVEY:
                return new SurveyImpl(referenceItem.getMission(), Collections.<LatLong>emptyList());
            case SPLINE_SURVEY:
                return new SplineSurveyImpl(referenceItem.getMission(), Collections.<LatLong>emptyList());
            case CYLINDRICAL_SURVEY:
                return new StructureScannerImpl(referenceItem);
            case SET_SERVO:
                return new SetServoImpl(referenceItem);
            case CONDITION_YAW:
                return new ConditionYawImpl(referenceItem);
            case SET_RELAY:
                return new SetRelayImpl(referenceItem);
            case DO_LAND_START:
                return new DoLandStartImpl(referenceItem);
            case DO_JUMP:
                return new DoJumpImpl(referenceItem);
            default:
                throw new IllegalArgumentException("Unrecognized mission item type (" + name + ")" + "");
        }
    }
}
