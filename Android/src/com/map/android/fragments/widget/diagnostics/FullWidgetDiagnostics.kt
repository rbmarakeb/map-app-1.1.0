package com.map.android.fragments.widget.diagnostics

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.map.android.lib.drone.property.EkfStatus
import com.map.android.lib.drone.property.EkfStatus.EkfFlags
import com.map.android.lib.util.SpannableUtils
import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter
import lecho.lib.hellocharts.formatter.SimpleColumnChartValueFormatter
import lecho.lib.hellocharts.model.Axis
import lecho.lib.hellocharts.model.Column
import lecho.lib.hellocharts.model.ColumnChartData
import lecho.lib.hellocharts.model.SubcolumnValue
import lecho.lib.hellocharts.view.ColumnChartView
import com.map.android.R
import com.map.android.activities.WidgetActivity
import com.map.android.fragments.widget.diagnostics.BaseWidgetDiagnostic
import com.map.android.fragments.widget.TowerWidget
import com.map.android.fragments.widget.TowerWidgets
import com.map.android.view.viewPager.TabPageIndicator
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Fredia Huya-Kouadio on 8/30/15.
 */
public class FullWidgetDiagnostics : TowerWidget(){

    private val viewAdapter by lazy(LazyThreadSafetyMode.NONE) {
        DiagnosticViewAdapter(context, childFragmentManager)
    }

    private var tabPageIndicator: TabPageIndicator? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_full_widget_diagnostics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById(R.id.diagnostics_view_pager) as ViewPager?
        viewPager?.adapter = viewAdapter
        viewPager?.offscreenPageLimit = 2

        tabPageIndicator = view.findViewById(R.id.pager_title_strip) as TabPageIndicator?
        tabPageIndicator?.setViewPager(viewPager)
    }

    override fun getWidgetType() = TowerWidgets.ATTITUDE_SPEED_INFO

    override fun onApiConnected() {}

    override fun onApiDisconnected() {}

    public fun setAdapterViewTitle(position: Int, title: CharSequence){
        viewAdapter.setViewTitles(position, title)
        tabPageIndicator?.notifyDataSetChanged()
    }
}