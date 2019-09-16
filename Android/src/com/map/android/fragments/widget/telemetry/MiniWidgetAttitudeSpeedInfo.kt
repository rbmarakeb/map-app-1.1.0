package com.map.android.fragments.widget.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.map.android.lib.drone.attribute.AttributeEvent
import com.map.android.lib.drone.attribute.AttributeType
import com.map.android.R
import com.map.android.activities.FlightActivity
import com.map.android.fragments.FlightDataFragment
import com.map.android.fragments.actionbar.ActionBarTelemFragment
import com.map.android.fragments.widget.TowerWidget
import com.map.android.fragments.widget.TowerWidgets
import com.map.android.lib.drone.mission.Mission
import com.map.android.lib.drone.property.*
import java.lang.String
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols.*
import java.util.*

/**
 * Created by Fredia Huya-Kouadio on 8/27/15.
 */
public class MiniWidgetAttitudeSpeedInfo : TowerWidget() {

    companion object {
        private val filter = initFilter()

        private fun initFilter(): IntentFilter {
            val temp = IntentFilter()
            temp.addAction(AttributeEvent.ATTITUDE_UPDATED)
            temp.addAction(AttributeEvent.SPEED_UPDATED)
            temp.addAction(AttributeEvent.GPS_POSITION)
            temp.addAction(AttributeEvent.HOME_UPDATED)
            return temp
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AttributeEvent.ATTITUDE_UPDATED -> onOrientationUpdate()
                AttributeEvent.SPEED_UPDATED -> onSpeedUpdate()
                AttributeEvent.GPS_POSITION, AttributeEvent.HOME_UPDATED -> onPositionUpdate()
            }
        }
    }

//    private var attitudeIndicator: AttitudeIndicator? = null
//    private var roll: TextView? = null
    private var yaw: TextView? = null
//    private var pitch: TextView? = null

    private var horizontalSpeed: TextView? = null
//    private var verticalSpeed: TextView? = null
    private var nextWP: TextView? = null
    private var wpDistance: TextView? = null

    private var headingModeFPV: Boolean = false

    private var speed_unit_linear: LinearLayout? = null
    private var kts_item: TextView? = null
    private var kph_item: TextView? = null
    private var mph_item: TextView? = null

    private var distance_unit_linear: LinearLayout? = null
    private var nm_item: TextView? = null
    private var km_item: TextView? = null
    private var m_item: TextView? = null
    private var ft_item: TextView? = null

    private val KEY_SELECTED_SPEED= "com.map.android.KEY_SELECTED_SPEED"
    private val SELECTED_SPEED = "com.map.android.SELECTED_SPEED"

    private val KEY_SELECTED_BIG_UNIT= "com.map.android.KEY_SELECTED_BIG_UNIT"
    private val SELECTED_BIG_UNIT = "com.map.android.SELECTED_BIG_UNIT"

    private val KEY_SELECTED_SMALL_UNIT= "com.map.android.KEY_SELECTED_SMALL_UNIT"
    private val SELECTED_SMALL_UNIT = "com.map.android.SELECTED_SMALL_UNIT"

    private var previousWPValue = 0

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_mini_widget_attitude_speed_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        attitudeIndicator = view.findViewById(R.id.aiView) as AttitudeIndicator?

//        roll = view.findViewById(R.id.rollValueText) as TextView?
//        yaw = view.findViewById(R.id.yawValueText) as TextView?
//        pitch = view.findViewById(R.id.pitchValueText) as TextView?

        horizontalSpeed = view.findViewById(R.id.horizontal_speed_telem) as TextView?
        yaw = view.findViewById(R.id.yawValueText) as TextView?
        nextWP = view.findViewById(R.id.nextWPText) as TextView?
        wpDistance = view.findViewById(R.id.wpDistanceText) as TextView?
        //Fix the Distance Data
        //the user click on Distance => Distance menu appears
        speed_unit_linear = view.findViewById(R.id.speed_unit_linear) as LinearLayout?

        kts_item = view.findViewById(R.id.kts_item) as TextView?
        kph_item = view.findViewById(R.id.kph_item) as TextView?
        mph_item = view.findViewById(R.id.mph_item) as TextView?

        distance_unit_linear = view.findViewById(R.id.distance_unit_linear) as LinearLayout?

        nm_item = view.findViewById(R.id.nm_item) as TextView?
        km_item = view.findViewById(R.id.km_item) as TextView?
        m_item = view.findViewById(R.id.m_item) as TextView?
        ft_item = view.findViewById(R.id.ft_item) as TextView?

        horizontalSpeed?.setOnClickListener{
            showHideSpeed()
        }

        wpDistance?.setOnClickListener{
            showHideDistance()
        }
        //kt => Knote
        //kph Kilometer per hour
        //mph miles per hour
        kts_item?.setOnClickListener{writeSharedPreference("kts",KEY_SELECTED_SPEED,SELECTED_SPEED)
            speed_unit_color()}
        kph_item?.setOnClickListener{writeSharedPreference("kph",KEY_SELECTED_SPEED,SELECTED_SPEED)
            speed_unit_color()}
        mph_item?.setOnClickListener{writeSharedPreference("mph",KEY_SELECTED_SPEED,SELECTED_SPEED)
            speed_unit_color()}

        nm_item?.setOnClickListener{writeSharedPreference("nm",KEY_SELECTED_BIG_UNIT,SELECTED_BIG_UNIT)
            distance_big_unit_color()}
        km_item?.setOnClickListener{writeSharedPreference("km",KEY_SELECTED_BIG_UNIT,SELECTED_BIG_UNIT)
            distance_big_unit_color()}
        m_item?.setOnClickListener{writeSharedPreference("m",KEY_SELECTED_SMALL_UNIT,SELECTED_SMALL_UNIT)
            distance_small_unit_color()}
        ft_item?.setOnClickListener{writeSharedPreference("ft",KEY_SELECTED_SMALL_UNIT,SELECTED_SMALL_UNIT)
            distance_small_unit_color()}

    }
    fun showHideSpeed() {
        if (speed_unit_linear?.visibility == View.VISIBLE){
            horizontalSpeed?.setBackgroundResource(0)
            speed_unit_linear?.visibility =  View.GONE

        }
        else{
            horizontalSpeed?.setBackgroundResource( R.drawable.customborder_widget_info)
            speed_unit_linear?.visibility = View.VISIBLE

            wpDistance?.setBackgroundResource(0)
            distance_unit_linear?.visibility =  View.GONE

            speed_unit_color()
        }
    }
    fun showHideDistance() {
        if (distance_unit_linear?.visibility == View.VISIBLE){
            wpDistance?.setBackgroundResource(0)
            distance_unit_linear?.visibility =  View.GONE

        }
        else{
            wpDistance?.setBackgroundResource( R.drawable.customborder_widget_info)
            distance_unit_linear?.visibility = View.VISIBLE

            horizontalSpeed?.setBackgroundResource(0)
            speed_unit_linear?.visibility =  View.GONE

            distance_small_unit_color()
            distance_big_unit_color()
        }
    }
    fun speed_unit_color() {
        var selected_speed_unit = readSharedPreference(KEY_SELECTED_SPEED, SELECTED_SPEED)
        if (selected_speed_unit.equals(""))
            selected_speed_unit = "kts"

        when (selected_speed_unit) {
            "kts" -> {
                kts_item?.setBackgroundColor(Color.rgb(171, 215, 255))
                kph_item?.setBackgroundColor(Color.WHITE)
                mph_item?.setBackgroundColor(Color.WHITE)
            }
            "kph" -> {
                kts_item?.setBackgroundColor(Color.WHITE)
                kph_item?.setBackgroundColor(Color.rgb(171, 215, 255))
                mph_item?.setBackgroundColor(Color.WHITE)
            }

            "mph" -> {
                kts_item?.setBackgroundColor(Color.WHITE)
                kph_item?.setBackgroundColor(Color.WHITE)
                mph_item?.setBackgroundColor(Color.rgb(171, 215, 255))
            }
        }
    }
    fun distance_big_unit_color()
    {
        var selected_distance_big_unit=readSharedPreference(KEY_SELECTED_BIG_UNIT,SELECTED_BIG_UNIT)
        if(selected_distance_big_unit.equals(""))
            selected_distance_big_unit="nm"

        when (selected_distance_big_unit) {
            "nm" -> {
                nm_item?.setBackgroundColor(Color.rgb(171, 215, 255))
                km_item?.setBackgroundColor(Color.WHITE)
            }
            "km" -> {
                nm_item?.setBackgroundColor(Color.WHITE)
                km_item?.setBackgroundColor(Color.rgb(171, 215, 255))
            }
        }


    }
    fun distance_small_unit_color()
    {
        var selected_distance_small_unit=readSharedPreference(KEY_SELECTED_SMALL_UNIT,SELECTED_SMALL_UNIT)
        if(selected_distance_small_unit.equals(""))
            selected_distance_small_unit="m"


        when (selected_distance_small_unit) {
            "m" -> {
                m_item?.setBackgroundColor(Color.rgb(171, 215, 255))
                ft_item?.setBackgroundColor(Color.WHITE)
            }
            "ft" -> {
                m_item?.setBackgroundColor(Color.WHITE)
                ft_item?.setBackgroundColor(Color.rgb(171, 215, 255))
            }
        }
    }
    override fun onStart() {
        super.onStart()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        headingModeFPV = prefs.getBoolean("pref_heading_mode", false)
    }

    override fun getWidgetType() = TowerWidgets.ATTITUDE_SPEED_INFO

    override fun onApiConnected() {
        updateAllTelem()
        broadcastManager.registerReceiver(receiver, filter)
    }

    override fun onApiDisconnected() {
        broadcastManager.unregisterReceiver(receiver)
    }

    private fun updateAllTelem() {
        onOrientationUpdate()
        onSpeedUpdate()
    }

    private fun onOrientationUpdate() {
        if (!isAdded)
            return
        if (drone.isConnected) {
            val drone = drone

            val attitude = drone.getAttribute<Attitude>(AttributeType.ATTITUDE) ?: return

//        val r = attitude.roll.toFloat()
//        val p = attitude.pitch.toFloat()
            var y = attitude.yaw.toFloat()

            if (!headingModeFPV and (y < 0)) {
                y += 360
            }

            val mission = drone.getAttribute<Mission>(AttributeType.MISSION)
            var nextWPValue = 0
            var wpDistanceValue = "0"
            if (mission != null) {
                var curMissionItem = mission.currentMissionItem
                var totalMissionItem = mission.missionItems.size;
                nextWPValue = curMissionItem;
//            if(nextWPValue > totalMissionItem) {
//                nextWPValue = 0
//            }

                // wpDistanceValue = mission.getDistance(curMissionItem)
            }
            //       nextWP?.text = "Next WP: " + String.valueOf(nextWPValue)
//        wpDistance?.text = "Distance: " + wpDistanceValue + " nm"

//        attitudeIndicator?.setAttitude(r, p, y)

//        roll?.text = String.format(Locale.US, "%3.0f\u00B0", r)
//        pitch?.text = String.format(Locale.US, "%3.0f\u00B0", p)
            yaw?.text = String.format(Locale.US, "Heading: %3.0f\u00B0", y)
        }

    }

    private fun onSpeedUpdate() {
        val symbolsEN_US = getInstance(Locale.US)
        val df_Dd = DecimalFormat("#.##", symbolsEN_US)
        df_Dd.roundingMode = RoundingMode.HALF_UP
        if (!isAdded)
            return
        if (drone.isConnected) {
            val drone = drone
            val speed = drone.getAttribute<Speed>(AttributeType.SPEED) ?: return
            val speedUnitProvider = speedUnitProvider

            val groundSpeedValue = speedUnitProvider.boxBaseValueToTarget(speed.groundSpeed * 1.94384).toString()
            val verticalSpeedValue = speed.verticalSpeed

            val mission = drone.getAttribute<Mission>(AttributeType.MISSION)
            var nextWPValue = 0
            var wpDistanceValue = "0"
            if (mission != null) {
                var curMissionItem = mission.currentMissionItem
                var totalMissionItem = mission.missionItems.size;
                nextWPValue = curMissionItem;
//            if(nextWPValue > totalMissionItem) {
//                nextWPValue = 0
//            }

                //wpDistanceValue = mission.getDistance(curMissionItem)
            }
            //       nextWP?.text = "Next WP: " + String.valueOf(nextWPValue)
//        wpDistance?.text = "Distance: " + wpDistanceValue + " nm"
            if (groundSpeedValue.contains("mph")
                    ||groundSpeedValue.contains("m/s")
                    ||groundSpeedValue.contains("m/kts")) {
                val speed_kts=speed.groundSpeed * 1.94384;
                var selected_speed_unit=readSharedPreference(KEY_SELECTED_SPEED,SELECTED_SPEED)
                if(selected_speed_unit.equals(""))
                    selected_speed_unit="kts"

                if(selected_speed_unit.equals("kts"))
                {
                    var f_speed_kts=df_Dd.format(speed_kts )
                    horizontalSpeed?.text ="Speed: " + f_speed_kts + " kts"
                }
                else if(selected_speed_unit.equals("kph"))
                {
                    // 1 kts = 1.852 kph
                    val speed_kph = speed_kts* 1.852;
                    var f_speed_kph=df_Dd.format(speed_kph )
                    horizontalSpeed?.text ="Speed: " + f_speed_kph + " kph"
                }
                else if(selected_speed_unit.equals("mph"))
                {
                    // 1 kts = 1.15 mph
                    val speed_mph = speed_kts * 1.15;
                    var f_speed_mph=df_Dd.format(speed_mph )
                    horizontalSpeed?.text ="Speed: " + f_speed_mph + " mph"
                }
                else
                {
                    if (groundSpeedValue.contains("mph")) {
                        //0.868976
                        groundSpeedValue.equals(speedUnitProvider.boxBaseValueToTarget(speed.groundSpeed * 0.868976).toString())
                        horizontalSpeed?.text = getString(R.string.horizontal_speed_telem, groundSpeedValue.replace("mph", "kts"))
                    } else if (groundSpeedValue.contains("m/s")) {
                        horizontalSpeed?.text = getString(R.string.horizontal_speed_telem, groundSpeedValue.replace("m/s", "kts"))
                    } else {
                        horizontalSpeed?.text = getString(R.string.horizontal_speed_telem, groundSpeedValue)
                    }
                }
                //if the current Activity is FlightActivity check speed to update mode
                if (activity.javaClass.simpleName == "FlightActivity")
                {
                var actionBarTelemFragment = FlightActivity.getInstance()
                changeToAutoModeBySpeed(speed_kts,  actionBarTelemFragment)
                }
            }
//        verticalSpeed?.text = getString(R.string.vertical_speed_telem, speedUnitProvider.boxBaseValueToTarget(verticalSpeedValue).toString())
        }
    }
    private fun onPositionUpdate() {
        val symbolsEN_US = getInstance(Locale.US)
        val df_Dd = DecimalFormat("#.#", symbolsEN_US)
        df_Dd.roundingMode = RoundingMode.HALF_UP
        if (!isAdded)
            return
        if (drone.isConnected) {
            val drone = drone
            val droneGps = drone.getAttribute<Gps>(AttributeType.GPS) ?: return
            val mission = drone.getAttribute<Mission>(AttributeType.MISSION)
            var nextWPValue = 0
            var wpDistanceValue = "0"
            if (droneGps.isValid) {

                val latitudeValue = droneGps.position.latitude
                val longitudeValue = droneGps.position.longitude
                if (mission != null) {
                    var curMissionItem = mission.currentMissionItem
                    var totalMissionItem = mission.missionItems.size;
                    nextWPValue = curMissionItem;
//            if(nextWPValue > totalMissionItem) {
//                nextWPValue = 0
//            }
                    wpDistanceValue = mission.getDistance(curMissionItem, latitudeValue, longitudeValue)
                }


            }
            var selected_distance_big_unit=readSharedPreference(KEY_SELECTED_BIG_UNIT,SELECTED_BIG_UNIT)
            if(selected_distance_big_unit.equals(""))
                selected_distance_big_unit="nm"

            var selected_distance_small_unit=readSharedPreference(KEY_SELECTED_SMALL_UNIT,SELECTED_SMALL_UNIT)
            if(selected_distance_small_unit.equals(""))
                selected_distance_small_unit="m"

            nextWP?.text = "Next WP: " + String.valueOf(nextWPValue)
            if(nextWPValue != 0)
            {
                if(previousWPValue != nextWPValue )
                //if the current Activity is FlightActivity check speed to update mode
                if (activity.javaClass.simpleName == "FlightActivity")
                {
                    val flightDataFragment = FlightActivity.getFlightDataInstance() as FlightDataFragment
                    val flightMapFragment = flightDataFragment.flightMapFragmentInstance
                    flightMapFragment.setNextMissionItemMarkerInfoGreenIcon(nextWPValue)
                    previousWPValue=nextWPValue
                }
            }
            if(selected_distance_small_unit.equals("m"))
            {
                var wpDistanceValue_m = wpDistanceValue.toDouble() * 1852
                var f_wpDistanceValue_m=df_Dd.format(wpDistanceValue_m )
                var f_wpDistanceValue=df_Dd.format(wpDistanceValue.toDouble() )
                if(wpDistanceValue_m < 500)
                wpDistance?.text = "Distance: " + f_wpDistanceValue_m + " m"
                else
                {
                    if(selected_distance_big_unit.equals("nm"))
                    {
                        wpDistance?.text = "Distance: " + f_wpDistanceValue + " NM"
                    }
                    else
                    {
                        var wpDistanceValue_km = wpDistanceValue.toDouble() * 1.852
                        var f_wpDistanceValue_km = df_Dd.format(wpDistanceValue_km )
                        wpDistance?.text = "Distance: " + f_wpDistanceValue_km + " km"
                    }
                }
            }
            else
            {
                var wpDistanceValue_ft = wpDistanceValue.toDouble() * 6076.12
                var f_wpDistanceValue_ft=df_Dd.format(wpDistanceValue_ft )
                var f_wpDistanceValue=df_Dd.format(wpDistanceValue.toDouble() )
                if(wpDistanceValue_ft < 500)
                wpDistance?.text = "Distance: " + f_wpDistanceValue_ft + " ft"
                else
                {
                    if(selected_distance_big_unit.equals("nm"))
                    {
                        wpDistance?.text = "Distance: " + f_wpDistanceValue + " NM"
                    }
                    else
                    {
                        var wpDistanceValue_km=wpDistanceValue.toDouble() * 1.852
                        var f_wpDistanceValue_km = df_Dd.format(wpDistanceValue_km )
                        wpDistance?.text = "Distance: " + f_wpDistanceValue_km + " km"
                    }
                }
            }
         //   wpDistance?.text = "Distance: " + wpDistanceValue + " nm"
        }
    }

    //save speed unit and distance unit type
    fun readSharedPreference(key: kotlin.String, s: kotlin.String): kotlin.String {
        val sharedPref = context.getSharedPreferences(key, MODE_PRIVATE)
        //-1 is default_value if no vaule
        return sharedPref.getString(s, "")
    }

    fun writeSharedPreference(savedSetting: kotlin.String, key: kotlin.String, s: kotlin.String) {
        val sharedPref = context.getSharedPreferences(key, MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(s, savedSetting)
        editor.commit()
    }
    //change the Auto mode to Hold mode when speed below 3 knots or above 15 knots, change mode from AUTO to HOLD.
    fun changeToAutoModeBySpeed(speed_kts : Double ,  actionarTelemFragment : ActionBarTelemFragment)
    {
//        if(speed_kts < 3 || speed_kts > 15)
//        {
//            actionarTelemFragment.updateModeBySpeed()
//        }

        if(speed_kts <15)
        {
//            val actionsBarFragment = FlightControlManagerFragment.getactionsBarFragmentInstance() as RoverFlightControlFragment
//            actionsBarFragment.changeFraqmentRoverControlBourder(true)
        }

    }


}