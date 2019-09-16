package com.map.android.service.utils.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.map.android.service.core.drone.profiles.VehicleProfile;
import com.map.android.service.core.drone.variables.StreamRates.Rates;
import com.map.android.service.core.firmware.FirmwareType;
import com.map.android.service.utils.file.IO.VehicleProfileReader;

/**
 * Provides structured access to 3DR Services preferences
 * <p/>
 * Over time it might be good to move the various places that are doing
 * prefs.getFoo(blah, default) here - to collect prefs in one place and avoid
 * duplicating string constants (which tend to become stale as code evolves).
 * This is called the DRY (don't repeat yourself) principle of software
 * development.
 */
public class DroidPlannerPrefs implements com.map.android.service.core.drone.Preferences {

    public static final int DEFAULT_STREAM_RATE = 2; //Hz

    private final SharedPreferences prefs;
    private final Context context;

    public DroidPlannerPrefs(Context context) {
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public VehicleProfile loadVehicleProfile(FirmwareType firmwareType) {
        return VehicleProfileReader.load(context, firmwareType);
    }

    @Override
    public Rates getRates() {
        return new Rates(DEFAULT_STREAM_RATE);
    }

    /**
     * @return true if google analytics reporting is enabled.
     */
    public boolean isUsageStatisticsEnabled() {
        return true;//return prefs.getBoolean("pref_usage_statistics", true);
    }

}
