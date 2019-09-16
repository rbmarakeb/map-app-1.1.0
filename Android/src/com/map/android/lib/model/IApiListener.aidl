package com.map.android.lib.model;

import com.map.android.lib.drone.connection.ConnectionResult;

/**
* DroneAPI event listener. A valid instance must be provided at api registration.
*/
interface IApiListener {

    /**
    * Retrieve the version code for the api.
    */
    int getApiVersionCode();

    /**
    * @deprecated
    * Called when the connection attempt fails.
    * @param result Describe why the connection failed.
    */
    oneway void onConnectionFailed(in ConnectionResult result);

    /**
    * Retrieve the version code for the connected client.
    */
    int getClientVersionCode();
}