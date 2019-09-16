package com.map.android.client.interfaces;

import android.os.Bundle;

import com.map.android.lib.drone.connection.ConnectionResult;
import com.map.android.lib.gcs.link.LinkConnectionStatus;

/**
 * Created by fhuya on 11/18/14.
 */
public interface DroneListener {

    /**
     * @deprecated Use {@link LinkListener#onLinkStateUpdated(LinkConnectionStatus)} instead.
     *
     * @param result
     */
    void onDroneConnectionFailed(ConnectionResult result);

    void onDroneEvent(String event, Bundle extras);

    void onDroneServiceInterrupted(String errorMsg);
}
