package com.map.android.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import com.map.android.client.Drone
import com.map.android.client.apis.CapabilityApi
import com.map.android.client.apis.VehicleApi
import com.map.android.client.apis.solo.SoloCameraApi
import com.map.android.lib.coordinate.LatLong
import com.map.android.lib.drone.attribute.AttributeEvent
import com.map.android.lib.drone.attribute.AttributeType
import com.map.android.lib.drone.companion.solo.SoloAttributes
import com.map.android.lib.drone.companion.solo.SoloEvents
import com.map.android.lib.drone.companion.solo.tlv.SoloGoproState
import com.map.android.R
import com.map.android.activities.helpers.SuperUI
import com.map.android.fragments.FlightDataFragment
import com.map.android.fragments.FlightMapFragment
import com.map.android.fragments.actionbar.ActionBarTelemFragment
import com.map.android.fragments.widget.TowerWidget
import com.map.android.fragments.widget.TowerWidgets
import com.map.android.fragments.widget.video.FullWidgetSoloLinkVideo
import com.map.android.utils.prefs.AutoPanMode
import kotlin.properties.Delegates

/**
 * Created by Fredia Huya-Kouadio on 7/19/15.
 */
public class WidgetActivity : SuperUI() {

    companion object {
        const val EXTRA_WIDGET_ID = "extra_widget_id"
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget)

        val fm = supportFragmentManager
        var flightDataFragment = fm.findFragmentById(R.id.map_view) as FlightDataFragment?
        if(flightDataFragment == null){
            flightDataFragment = FlightDataFragment()
            fm.beginTransaction().add(R.id.map_view, flightDataFragment).commit()
        }

        handleIntent(intent)
    }

    override fun addToolbarFragment() {
        val toolbarId = toolbarId
        val fm = supportFragmentManager
        var actionBarTelem: Fragment? = fm.findFragmentById(toolbarId)
        if (actionBarTelem == null) {
            actionBarTelem = ActionBarTelemFragment()
            fm.beginTransaction().add(toolbarId, actionBarTelem).commit()
        }
    }

    override fun onNewIntent(intent: Intent?){
        super.onNewIntent(intent)
        if(intent != null)
            handleIntent(intent)
    }

    private fun handleIntent(intent: Intent){
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, 0)
        val fm = supportFragmentManager

        val widget = TowerWidgets.getWidgetById(widgetId)
        if(widget != null){
            setToolbarTitle(widget.labelResId)

            val currentWidgetType = (fm.findFragmentById(R.id.widget_view) as TowerWidget?)?.getWidgetType()

            if(widget == currentWidgetType)
                return

            val widgetFragment = widget.getMaximizedFragment()
            fm.beginTransaction().replace(R.id.widget_view, widgetFragment).commit()
        }
    }

    override fun getToolbarId() = R.id.actionbar_container

}