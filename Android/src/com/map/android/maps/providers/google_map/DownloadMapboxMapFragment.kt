package com.map.android.maps.providers.google_map

import android.widget.Toast
import com.map.android.fragments.DroneMap
import com.map.android.utils.prefs.AutoPanMode

/**
 * Created by Fredia Huya-Kouadio on 6/17/15.
 */
public class DownloadMapboxMapFragment : DroneMap() {
    override fun isMissionDraggable() = false

    override fun setAutoPanMode(target: AutoPanMode?): Boolean {
        return when(target){
            AutoPanMode.DISABLED -> true
            else -> {
                Toast.makeText(activity, "Auto pan is not supported on this map.", Toast.LENGTH_LONG).show()
                false
            }
        }
    }
}