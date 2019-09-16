package com.map.android.client;

import com.map.android.lib.mavlink.MavlinkMessageWrapper;
import com.map.android.lib.model.IMavlinkObserver;

/**
 * Allows to register for mavlink message updates.
 */
public abstract class MavlinkObserver extends IMavlinkObserver.Stub {

    @Override
    public abstract void onMavlinkMessageReceived(MavlinkMessageWrapper mavlinkMessageWrapper);
}
