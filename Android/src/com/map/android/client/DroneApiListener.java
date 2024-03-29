package com.map.android.client;

import android.os.RemoteException;

import com.map.android.BuildConfig;
import com.map.android.lib.drone.connection.ConnectionResult;
import com.map.android.lib.model.IApiListener;
import com.map.android.lib.util.version.VersionUtils;

/**
 * Created by fhuya on 12/15/14.
 */
public class DroneApiListener extends IApiListener.Stub {

    private final Drone drone;

    public DroneApiListener(Drone drone){
        this.drone = drone;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) throws RemoteException {
        drone.notifyDroneConnectionFailed(connectionResult);
    }

    @Override
    public int getClientVersionCode() throws RemoteException {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public int getApiVersionCode(){
        return VersionUtils.getCoreLibVersion(drone.getContext());
    }
}
