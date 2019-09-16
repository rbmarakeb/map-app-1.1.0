package com.map.android.service.core.drone;

import com.map.android.service.core.drone.profiles.VehicleProfile;
import com.map.android.service.core.drone.variables.StreamRates;
import com.map.android.service.core.firmware.FirmwareType;

public interface Preferences {

	public abstract VehicleProfile loadVehicleProfile(FirmwareType firmwareType);

    public StreamRates.Rates getRates();
}
