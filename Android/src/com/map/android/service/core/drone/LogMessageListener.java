package com.map.android.service.core.drone;

/**
 * Created by Fredia Huya-Kouadio on 3/23/15.
 */
public interface LogMessageListener {

    void onMessageLogged(int logLevel, String message);
}
