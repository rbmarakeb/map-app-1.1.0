package com.map.android.service.core.drone.variables;

import java.util.ArrayList;
import java.util.List;

import com.map.android.service.core.drone.DroneInterfaces.DroneEventsType;
import com.map.android.service.core.drone.DroneVariable;
import com.map.android.service.core.drone.autopilot.MavLinkDrone;
import com.map.android.service.core.survey.CameraInfo;
import com.map.android.service.core.survey.Footprint;

import com.MAVLink.ardupilotmega.msg_camera_feedback;
import com.MAVLink.ardupilotmega.msg_mount_status;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.Altitude;
import com.map.android.lib.drone.property.Attitude;
import com.map.android.lib.drone.property.Gps;

public class Camera extends DroneVariable {
	private CameraInfo camera = new CameraInfo();
	private List<Footprint> footprints = new ArrayList<Footprint>();
	private double gimbal_pitch;

	public Camera(MavLinkDrone myDrone) {
		super(myDrone);
	}

	public void newImageLocation(msg_camera_feedback msg) {
		footprints.add(new Footprint(camera, msg));
		myDrone.notifyDroneEvent(DroneEventsType.FOOTPRINT);
	}

    public List<Footprint> getFootprints(){
        return footprints;
    }

	public Footprint getLastFootprint() {
		return footprints.get(footprints.size() - 1);
	}

	public CameraInfo getCamera() {
		return camera;
	}

	public Footprint getCurrentFieldOfView() {
		final Altitude droneAltitude = (Altitude) myDrone.getAttribute(AttributeType.ALTITUDE);
		double altitude = droneAltitude.getAltitude();

		final Gps droneGps = (Gps) myDrone.getAttribute(AttributeType.GPS);
		LatLong position = droneGps.getPosition();
		//double pitch = myDrone.getOrientation().getPitch() - gimbal_pitch;

		final Attitude attitude = (Attitude) myDrone.getAttribute(AttributeType.ATTITUDE);
		double pitch = attitude.getPitch();
		double roll = attitude.getRoll();
		double yaw = attitude.getYaw();
		return new Footprint(camera, position, altitude, pitch, roll, yaw);
	}

	public void updateMountOrientation(msg_mount_status msg_mount_status) {
		gimbal_pitch = 90 - msg_mount_status.pointing_a / 100;
	}

}
