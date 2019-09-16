package com.map.android.service.core.drone.variables;

import com.map.android.service.core.drone.DroneInterfaces.DroneEventsType;
import com.map.android.service.core.drone.DroneVariable;
import com.map.android.service.core.drone.autopilot.MavLinkDrone;

import com.MAVLink.common.msg_raw_imu;
import com.map.android.lib.drone.property.Parameter;

public class Magnetometer extends DroneVariable {

	private int x;
	private int y;
	private int z;

	public Magnetometer(MavLinkDrone myDrone) {
		super(myDrone);
	}

	public void newData(msg_raw_imu msg_imu) {
		x = msg_imu.xmag;
		y = msg_imu.ymag;
		z = msg_imu.zmag;
		myDrone.notifyDroneEvent(DroneEventsType.MAGNETOMETER);
	}

	public int[] getVector() {
		return new int[] { x, y, z };
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public int[] getOffsets() {
		Parameter paramX = myDrone.getParameterManager().getParameter("COMPASS_OFS_X");
		Parameter paramY = myDrone.getParameterManager().getParameter("COMPASS_OFS_Y");
		Parameter paramZ = myDrone.getParameterManager().getParameter("COMPASS_OFS_Z");
		if (paramX == null || paramY == null || paramZ == null) {
			return null;
		}
		return new int[]{(int) paramX.getValue(),(int) paramY.getValue(),(int) paramZ.getValue()};

	}
}
