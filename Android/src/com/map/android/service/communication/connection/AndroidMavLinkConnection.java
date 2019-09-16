package com.map.android.service.communication.connection;

import android.content.Context;

import com.map.android.service.core.MAVLink.connection.MavLinkConnection;
import com.map.android.service.core.model.Logger;
import com.map.android.service.utils.AndroidLogger;

public abstract class AndroidMavLinkConnection extends MavLinkConnection {

    protected final Context mContext;

    public AndroidMavLinkConnection(Context applicationContext) {
        this.mContext = applicationContext;
    }

    @Override
    protected final Logger initLogger() {
        return AndroidLogger.getLogger();
    }
}
