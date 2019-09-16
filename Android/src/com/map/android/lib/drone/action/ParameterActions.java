package com.map.android.lib.drone.action;

import com.map.android.lib.util.Utils;

/**
 * Created by Fredia Huya-Kouadio on 1/19/15.
 */
public class ParameterActions {

    //Private to prevent instantiation
    private ParameterActions(){}

    public static final String ACTION_REFRESH_PARAMETERS = Utils.PACKAGE_NAME + ".action.REFRESH_PARAMETERS";

    public static final String ACTION_WRITE_PARAMETERS = Utils.PACKAGE_NAME + ".action.WRITE_PARAMETERS";
    public static final String EXTRA_PARAMETERS = "extra_parameters";
}
