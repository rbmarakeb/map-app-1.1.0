package com.map.android.proxy.mission.item.fragments

import com.map.android.lib.drone.mission.MissionItemType
import com.map.android.R

/**
 * Created by Fredia Huya-Kouadio on 10/20/15.
 */
class MissionResetROIFragment : MissionDetailFragment() {

    override fun getResource() = R.layout.fragment_editor_detail_reset_roi

    override fun onApiConnected(){
        super.onApiConnected()
        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.RESET_ROI))
    }
}