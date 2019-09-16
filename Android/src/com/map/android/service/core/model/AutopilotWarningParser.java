package com.map.android.service.core.model;

import com.map.android.service.core.drone.autopilot.MavLinkDrone;

/**
 * Parse received autopilot warning messages.
 */
public interface AutopilotWarningParser {

    String getDefaultWarning();

    String parseWarning(MavLinkDrone drone, String warning);
}
