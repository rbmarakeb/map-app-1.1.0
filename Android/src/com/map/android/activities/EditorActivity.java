package com.map.android.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.map.android.activities.interfaces.OnEditorInteraction;
import com.map.android.dialogs.openfile.OpenPOIDialog;
import com.map.android.fragments.EditorListFragment;
import com.map.android.fragments.account.editor.tool.EditorToolsFragment;
import com.map.android.fragments.account.editor.tool.EditorToolsImpl;
import com.map.android.fragments.account.editor.tool.POIToolsImpl;
import com.map.android.graphic.map.GraphicWPHome;
import com.map.android.lib.drone.mission.item.POIItem;
import com.map.android.proxy.mission.item.MissionItemProxy;
import com.map.android.proxy.mission.item.fragments.MissionDetailFragment;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.mission.MissionItemType;

import org.beyene.sius.unit.length.LengthUnit;
import com.map.android.R;

import com.map.android.dialogs.SupportEditInputDialog;
import com.map.android.dialogs.openfile.OpenFileDialog;
import com.map.android.dialogs.openfile.OpenMissionDialog;
import com.map.android.fragments.EditorMapFragment;
import com.map.android.fragments.helpers.GestureMapFragment;
import com.map.android.fragments.helpers.GestureMapFragment.OnPathFinishedListener;
import com.map.android.proxy.mission.MissionProxy;
import com.map.android.proxy.mission.MissionSelection;
import com.map.android.proxy.mission.item.fragments.MissionPOIFragment;
import com.map.android.utils.analytics.GAUtils;
import com.map.android.utils.file.FileStream;
import com.map.android.utils.file.IO.MissionReader;
import com.map.android.utils.file.IO.POIReader;
import com.map.android.utils.prefs.AutoPanMode;

import java.util.ArrayList;
import java.util.List;

/**
 * This implements the map editor activity. The map editor activity allows the
 * user to create and/or modify autonomous missions for the drone.
 */
public class EditorActivity extends DrawerNavigationUI implements OnPathFinishedListener,
        EditorToolsFragment.EditorToolListener, MissionDetailFragment.OnMissionDetailListener,
        OnEditorInteraction, MissionSelection.OnSelectionUpdateListener, OnClickListener,
        OnLongClickListener, SupportEditInputDialog.Listener {

    private static final double DEFAULT_SPEED = 5; //meters per second.

    /**
     * Used to retrieve the item detail window when the activity is destroyed,
     * and recreated.
     */
    private static final String ITEM_DETAIL_TAG = "Item Detail Window";

    private static final String EXTRA_OPENED_MISSION_FILENAME = "extra_opened_mission_filename";
    private static final String EXTRA_OPENED_POI_FILENAME = "extra_opened_poi_filename";

    private static final IntentFilter eventFilter = new IntentFilter();
    private static final String MISSION_FILENAME_DIALOG_TAG = "Mission filename";
    private static final String POI_FILENAME_DIALOG_TAG = "POI filename";
    private static final String POI_ADD_FILENAME_DIALOG_TAG = "POI add filename";
    private static final String POI_ADD_DIALOG_TAG = "POI add";
    private static final String POI_EDIT_DIALOG_TAG = "POI edit";

    static {
        eventFilter.addAction(MissionProxy.ACTION_MISSION_PROXY_UPDATE);
        eventFilter.addAction(AttributeEvent.MISSION_RECEIVED);
        eventFilter.addAction(AttributeEvent.PARAMETERS_REFRESH_COMPLETED);
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case AttributeEvent.PARAMETERS_REFRESH_COMPLETED:
                case MissionProxy.ACTION_MISSION_PROXY_UPDATE:
                    updateMissionLength();
                    break;

                case AttributeEvent.MISSION_RECEIVED:
                    final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
                    if (planningMapFragment != null) {
                        planningMapFragment.zoomToFit();
                    }
                    break;
            }
        }
    };

    /**
     * Used to provide access and interact with the
     * {@link MissionProxy} object on the Android
     * layer.
     */
    private static MissionProxy missionProxy;

    /*
     * View widgets.
     */
    private GestureMapFragment gestureMapFragment;
    private EditorToolsFragment editorToolsFragment;
    private MissionDetailFragment itemDetailFragment;
    private FragmentManager fragmentManager;

//    private TextView infoView;

    /**
     * If the mission was loaded from a file, the filename is stored here.
     */
    private String openedMissionFilename, openedPOIFilename;

    private FloatingActionButton itemDetailToggle;
    private FloatingActionButton mPOIButton;
    private EditorListFragment editorListFragment;
    private LatLong curPosition;

    private LinearLayout location_button_container;
    private boolean isShow;//to check if WayPoint detail fragment show
    @Override
    public void onCreate(Bundle savedInstanceState) {
        fragmentManager = getSupportFragmentManager();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        gestureMapFragment = ((GestureMapFragment) fragmentManager.findFragmentById(R.id.editor_map_fragment));
        if (gestureMapFragment == null) {
            gestureMapFragment = new GestureMapFragment();
            fragmentManager.beginTransaction().add(R.id.editor_map_fragment, gestureMapFragment).commit();
        }

        editorListFragment = (EditorListFragment) fragmentManager.findFragmentById(R.id.mission_list_fragment);

//        infoView = (TextView) findViewById(R.id.editorInfoWindow);

        final FloatingActionButton zoomToFit = (FloatingActionButton) findViewById(R.id.zoom_to_fit_button);
        zoomToFit.setVisibility(View.VISIBLE);
        zoomToFit.setOnClickListener(this);

        final FloatingActionButton mGoToMyLocation = (FloatingActionButton) findViewById(R.id.my_location_button);
        mGoToMyLocation.setOnClickListener(this);
        mGoToMyLocation.setOnLongClickListener(this);

        final FloatingActionButton mGoToDroneLocation = (FloatingActionButton) findViewById(R.id.drone_location_button);
        mGoToDroneLocation.setOnClickListener(this);
        mGoToDroneLocation.setOnLongClickListener(this);

        mPOIButton = (FloatingActionButton) findViewById(R.id.poi_button);
        mPOIButton.setOnClickListener(this);

        //final ImageButton imgHome = (ImageButton) findViewById(R.id.imgHome);
       // imgHome.setOnClickListener(this);

        itemDetailToggle = (FloatingActionButton) findViewById(R.id.toggle_action_drawer);
        itemDetailToggle.setOnClickListener(this);

        //zoom in button
        final ImageButton zoomin = (ImageButton) findViewById(R.id.zoomin);
        zoomin.setOnClickListener(this);
        //zoom out button
        final ImageButton zoomout = (ImageButton) findViewById(R.id.zoomout);
        zoomout.setOnClickListener(this);

        location_button_container= (LinearLayout) findViewById(R.id.location_button_container);
        isShow=false;
        if (savedInstanceState != null) {
            openedMissionFilename = savedInstanceState.getString(EXTRA_OPENED_MISSION_FILENAME);
            openedPOIFilename = savedInstanceState.getString(EXTRA_OPENED_POI_FILENAME);
        }

        // Retrieve the item detail fragment using its tag
        itemDetailFragment = (MissionDetailFragment) fragmentManager.findFragmentByTag(ITEM_DETAIL_TAG);

        gestureMapFragment.setOnPathFinishedListener(this);
        openActionDrawer();
    }

    @Override
    protected float getActionDrawerTopMargin() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
    }

    /**
     * Account for the various ui elements and update the map padding so that it
     * remains 'visible'.
     */
    private void updateLocationButtonsMargin(boolean isOpened) {
        final View actionDrawer = getActionDrawer();
        if (actionDrawer == null)
            return;

        itemDetailToggle.setActivated(isOpened);
    }

    @Override
    public void onApiConnected() {
        super.onApiConnected();

        missionProxy = dpApp.getMissionProxy();
        if (missionProxy != null) {
            missionProxy.selection.addSelectionUpdateListener(this);
            itemDetailToggle.setVisibility(missionProxy.selection.getSelected().isEmpty() ? View.GONE : View.VISIBLE);
        }

        updateMissionLength();
        getBroadcastManager().registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onApiDisconnected() {
        super.onApiDisconnected();

        if (missionProxy != null)
            missionProxy.selection.removeSelectionUpdateListener(this);

        getBroadcastManager().unregisterReceiver(eventReceiver);
    }

    @Override
    public void onClick(View v) {
        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();

        switch (v.getId()) {
            case R.id.toggle_action_drawer:
                if (missionProxy == null)
                    return;

//                if (itemDetailFragment == null) {
//                    List<MissionItemProxy> selected = missionProxy.selection.getSelected();
//                    showItemDetail(selectMissionDetailType(selected));
//                } else {
//                    removeItemDetail();
//                }
                break;

            case R.id.zoom_to_fit_button:
                if (planningMapFragment != null) {
                    planningMapFragment.zoomToFit();
                }
                break;

            case R.id.drone_location_button:
                planningMapFragment.goToDroneLocation();
                break;
            case R.id.my_location_button:
                planningMapFragment.goToMyLocation();
                break;
            case R.id.poi_button:
                planningMapFragment.isShowPOI = !planningMapFragment.isShowPOI;
                if (planningMapFragment.isShowPOI) {
                    mPOIButton.setImageResource(R.drawable.ic_poi_on);
                } else {
                    mPOIButton.setImageResource(R.drawable.ic_poi_off);
                }
                planningMapFragment.postUpdatePOI();
                break;
            case R.id.zoomin:
                planningMapFragment.onZoom(v);
                break;
            case R.id.zoomout:
                planningMapFragment.onZoom(v);
                break;
            //case R.id.imgHome:
//                EditorToolsImpl toolImpl = getToolImpl();
//                toolImpl.setTypeIdx(1);
//                toolImpl.onMapClick(planningMapFragment.getMapFragment().getMyCoordinate());
                //if(!mAppPrefs.getHomeMarkerEnabled()) {
                   // mAppPrefs.setHomeMarkerEnabled(true);
              //      GraphicWPHome homeMarker = new GraphicWPHome(this, planningMapFragment.getMapFragment().getMyCoordinate());
                //    planningMapFragment.getMapFragment().updateMarker(homeMarker, false);
              //  }
              //  break;
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();

        switch (view.getId()) {
            case R.id.drone_location_button:
                planningMapFragment.setAutoPanMode(AutoPanMode.DRONE);
                return true;
            case R.id.my_location_button:
                planningMapFragment.setAutoPanMode(AutoPanMode.USER);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        editorToolsFragment.setToolAndUpdateView(getTool());
        setupTool();
        gestureMapFragment.getMapFragment().goToMyLocation();
    }

    @Override
    protected int getToolbarId() {
        return R.id.actionbar_container;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_OPENED_MISSION_FILENAME, openedMissionFilename);
        outState.putString(EXTRA_OPENED_POI_FILENAME, openedPOIFilename);
    }

    @Override
    protected int getNavigationDrawerMenuItemId() {
        return R.id.navigation_editor;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_mission, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_open_mission:
                openMissionFile();
                return true;

            case R.id.menu_save_mission:
                saveMissionFile();
                return true;

            case R.id.menu_edit_poi:
                editPOIs();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openMissionFile() {
        OpenFileDialog missionDialog = new OpenMissionDialog() {
            @Override
            public void waypointFileLoaded(MissionReader reader) {
                openedMissionFilename = getSelectedFilename();

                if(missionProxy != null) {
                    missionProxy.readMissionFromFile(reader);
                    gestureMapFragment.getMapFragment().zoomToFit();
                }
            }
        };
        missionDialog.openDialog(this);
    }

    @Override
    public void onOk(String dialogTag, CharSequence input) {
        final Context context = getApplicationContext();

        switch (dialogTag) {
            case MISSION_FILENAME_DIALOG_TAG:
                if (missionProxy.writeMissionToFile(input.toString())) {
                    Toast.makeText(context, R.string.file_saved_success, Toast.LENGTH_SHORT)
                            .show();

                    final HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                            .setCategory(GAUtils.Category.MISSION_PLANNING)
                            .setAction(getString(R.string.mission_saved_file))
                            .setLabel(getString(R.string.mission_item_count));
                    GAUtils.sendEvent(eventBuilder);

                    break;
                }

                Toast.makeText(context, R.string.file_saved_error, Toast.LENGTH_SHORT).show();
                break;
            case POI_FILENAME_DIALOG_TAG:
                List<MissionItemProxy> list = missionProxy.selection.getSelected();
                if (list != null && list.size() > 0) {
                    double lat = list.get(0).getMarkerInfos().get(0).getPosition().getLatitude();
                    double lon = list.get(0).getMarkerInfos().get(0).getPosition().getLongitude();
                    POIItem item = new POIItem(input.toString(), lat, lon);
                    if (missionProxy.writePOIToFile(item)) {
                        Toast.makeText(context, R.string.file_saved_success, Toast.LENGTH_SHORT)
                                .show();
                        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
                        if (planningMapFragment != null)
                            planningMapFragment.postUpdatePOI();
                        missionProxy.selection.clearSelection();

                        break;
                    }
                }

                Toast.makeText(context, R.string.file_saved_error, Toast.LENGTH_SHORT).show();
                break;
            case POI_ADD_FILENAME_DIALOG_TAG:
                if (input != null && input.length() > 0) {
                    POIItem item = new POIItem(input.toString(), curPosition.getLatitude(), curPosition.getLongitude());
                    if (missionProxy.writePOIToFile(item)) {
                        Toast.makeText(context, R.string.file_saved_success, Toast.LENGTH_SHORT)
                                .show();
                        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
                        if (planningMapFragment != null)
                            planningMapFragment.postUpdatePOI();
                        missionProxy.selection.clearSelection();

                        break;
                    }
                }

                Toast.makeText(context, R.string.file_saved_error, Toast.LENGTH_SHORT).show();
                break;

            case POI_ADD_DIALOG_TAG:
                if (input != null && input.length() > 0) {
                    for (POIItem item : missionProxy.getPoiList()) {
                        if (item.getName().equals(input.toString())) {
                            EditorToolsImpl toolImpl = getToolImpl();
                            toolImpl.onMapClick(item);
                            final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
                            planningMapFragment.postUpdatePOI();
                            break;
                        }
                    }
                    break;
                }
                Toast.makeText(context, R.string.poi_edit_error, Toast.LENGTH_SHORT).show();
                break;

            case POI_EDIT_DIALOG_TAG:
                if (input != null && input.length() > 0) {
                    for (POIItem item : missionProxy.getPoiList()) {
                        if (item.getName().equals(input.toString())) {
                            MissionPOIFragment fragment = new MissionPOIFragment();
                            fragment.setPOIItem(item);
                            showItemDetail(fragment);
                            break;
                        }
                    }
                    break;
                }
                Toast.makeText(context, R.string.poi_edit_error, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onCancel(String dialogTag) {
    }

    private void saveMissionFile() {
        final String defaultFilename = TextUtils.isEmpty(openedMissionFilename)
                ? FileStream.getWaypointFilename("waypoints")
                : openedMissionFilename;

        final SupportEditInputDialog dialog = SupportEditInputDialog.newInstance(MISSION_FILENAME_DIALOG_TAG,
                getString(R.string.label_enter_filename), defaultFilename, true);

        dialog.show(getSupportFragmentManager(), MISSION_FILENAME_DIALOG_TAG);
    }

    private void savePOIFile() {
        final SupportEditInputDialog dialog = SupportEditInputDialog.newInstance(POI_FILENAME_DIALOG_TAG,
                getString(R.string.label_enter_poi_name), "POI_Name", true);

        dialog.show(getSupportFragmentManager(), POI_FILENAME_DIALOG_TAG);
    }

    private void editPOIs() {
//        final String defaultFilename = TextUtils.isEmpty(openedPOIFilename)
//                ? FileStream.getPOIFilename("pois")
//                : openedPOIFilename;
//        missionProxy.openPOIFile();
        if (missionProxy.poiList.size() == 0) {
            Toast.makeText(this, R.string.poi_item_empty, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        String[] poiNames = new String[missionProxy.poiList.size()];
        for (int i = 0; i < missionProxy.poiList.size(); i++) {
            poiNames[i] = missionProxy.poiList.get(i).getName();
        }

        final SupportEditInputDialog dialog = SupportEditInputDialog.newInstance(POI_EDIT_DIALOG_TAG,
                getString(R.string.edit_pois), "Enter POI Name ...", true, poiNames);

        dialog.show(getSupportFragmentManager(), POI_EDIT_DIALOG_TAG);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        gestureMapFragment.getMapFragment().saveCameraPosition();
    }

    private void updateMissionLength() {
        if (missionProxy != null) {

            double missionLength = missionProxy.getMissionLength();
            LengthUnit convertedMissionLength = unitSystem.getLengthUnitProvider().boxBaseValueToTarget(missionLength);
            double speedParameter = dpApp.getDrone().getSpeedParameter() / 100; //cm/s to m/s conversion.
            if (speedParameter == 0)
                speedParameter = DEFAULT_SPEED;

            int time = (int) (missionLength / speedParameter);

            String infoString = getString(R.string.editor_info_window_distance, convertedMissionLength.toString())
                    + ", " + getString(R.string.editor_info_window_flight_time, time / 60, time % 60);

//            infoView.setText(infoString);

            // Remove detail window if item is removed
            if (missionProxy.selection.getSelected().isEmpty() && itemDetailFragment != null) {
                removeItemDetail();
            }
        }
    }

    @Override
    public void onMapClick(LatLong point) {
        EditorToolsImpl toolImpl = getToolImpl();
        if (toolImpl instanceof POIToolsImpl) {
            curPosition = point;
            final SupportEditInputDialog dialog = SupportEditInputDialog.newInstance(POI_ADD_FILENAME_DIALOG_TAG,
                    getString(R.string.label_enter_poi_name), "POI_Name", true);

            dialog.show(getSupportFragmentManager(), POI_ADD_FILENAME_DIALOG_TAG);
        } else {
            toolImpl.onMapClick(point);
        }
    }

    public EditorToolsFragment.EditorTools getTool() {
        return editorToolsFragment.getTool();
    }

    public EditorToolsImpl getToolImpl() {
        return editorToolsFragment.getToolImpl();
    }

    @Override
    public void editorToolChanged(EditorToolsFragment.EditorTools tools) {
        setupTool();
    }

    @Override
    public void enableGestureDetection(boolean enable) {
        if (gestureMapFragment == null)
            return;

        if (enable)
            gestureMapFragment.enableGestureDetection();
        else
            gestureMapFragment.disableGestureDetection();
    }

    @Override
    public void skipMarkerClickEvents(boolean skip) {
        if (gestureMapFragment == null)
            return;

        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
        if (planningMapFragment != null)
            planningMapFragment.skipMarkerClickEvents(skip);
    }

    private void setupTool() {
        final EditorToolsImpl toolImpl = getToolImpl();
        toolImpl.setup();
        editorListFragment.enableDeleteMode(toolImpl.getEditorTools() == EditorToolsFragment.EditorTools.TRASH);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        updateLocationButtonsMargin(itemDetailFragment != null);
    }

    @Override
    protected void addToolbarFragment(){
        final int toolbarId = getToolbarId();
        editorToolsFragment = (EditorToolsFragment) fragmentManager.findFragmentById(toolbarId);
        if (editorToolsFragment == null) {
            editorToolsFragment = new EditorToolsFragment();
            fragmentManager.beginTransaction().add(toolbarId, editorToolsFragment).commit();
        }
    }

    private void showItemDetail(MissionDetailFragment itemDetail) {
        if (itemDetailFragment == null) {
            addItemDetail(itemDetail);
        } else {
            switchItemDetail(itemDetail);
        }
        isShow=true;
        editorToolsFragment.setToolAndUpdateView(EditorToolsFragment.EditorTools.NONE);
    }

    private void addItemDetail(MissionDetailFragment itemDetail) {
        itemDetailFragment = itemDetail;
        if (itemDetailFragment == null)
            return;
        location_button_container.setVisibility(View.GONE);
        enableDisableViewGroup((ViewGroup) editorToolsFragment.getView(),false);
        DrawerNavigationUI.lockDrawer();
        fragmentManager.beginTransaction()
                .replace(getActionDrawerId(), itemDetailFragment, ITEM_DETAIL_TAG)
                .commit();
        updateLocationButtonsMargin(true);
    }

    public void switchItemDetail(MissionDetailFragment itemDetail) {
        removeItemDetail();
        addItemDetail(itemDetail);
    }
    /**
     * Enables/Disables all child views in a view group.
     *
     * @param viewGroup the view group
     * @param enabled <code>true</code> to enable, <code>false</code> to disable
     * the views.
     */
    public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            }
        }
    }

    private void removeItemDetail() {
        if (itemDetailFragment != null) {
            fragmentManager.beginTransaction().remove(itemDetailFragment).commit();
            itemDetailFragment = null;
            location_button_container.setVisibility(View.VISIBLE);
            enableDisableViewGroup((ViewGroup) editorToolsFragment.getView(),true);
            DrawerNavigationUI.unlockDrawer();
            updateLocationButtonsMargin(false);
            isShow=false;
        }
    }
    //this function used to check if path updated to appear message box
    //"The message box should not appear if the route was not changed by the user. It is redundant"
    public static List<LatLong> getCurrentWaypointsPositions(){
        if(missionProxy != null)
        {
            List<LatLong> latLongsWayPointPosition =new ArrayList<>();
            for (int i=0 ; i< missionProxy.getMarkersInfos().size() ; i++)
            {
                //waypoint is the "first item of getMarkerInfos list" in each "item MissionProxyof list"
                LatLong latLong=new LatLong( missionProxy.getMarkersInfos().get(i).getPosition());

                latLongsWayPointPosition.add(latLong);
            }

        return latLongsWayPointPosition;
        }
        else
        return null;
    }
    @Override
    public void onPathFinished(List<LatLong> path) {
        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
        List<LatLong> points = planningMapFragment.projectPathIntoMap(path);
        EditorToolsImpl toolImpl = getToolImpl();
        toolImpl.onPathFinished(points);
    }

    @Override
    public void onDetailDialogDismissed(List<MissionItemProxy> itemList) {
        if (missionProxy != null) missionProxy.selection.removeItemsFromSelection(itemList);
    }

    @Override
    public void onWaypointTypeChanged(MissionItemType newType, List<Pair<MissionItemProxy,
            List<MissionItemProxy>>> oldNewItemsList) {
        missionProxy.replaceAll(oldNewItemsList);
    }

    private MissionDetailFragment selectMissionDetailType(List<MissionItemProxy> proxies) {
        if (proxies == null || proxies.isEmpty())
            return null;

        MissionItemType referenceType = null;
        for (MissionItemProxy proxy : proxies) {
            final MissionItemType proxyType = proxy.getMissionItem().getType();
            if (referenceType == null) {
                referenceType = proxyType;
            } else if (referenceType != proxyType
                    || MissionDetailFragment.typeWithNoMultiEditSupport.contains(referenceType)) {
                //Return a generic mission detail.
                return new MissionDetailFragment();
            }
        }

        return MissionDetailFragment.newInstance(referenceType);
    }

    @Override
    public void onItemClick(MissionItemProxy item, boolean zoomToFit) {
        if (missionProxy == null || isShow) return;

        EditorToolsImpl toolImpl = getToolImpl();
        toolImpl.onListItemClick(item);

        if (zoomToFit) {
            zoomToFitSelected();
        }
    }

    @Override
    public void onItemClickToAddPOI() {
//        missionProxy.openPOIFile();
        //to prevent add POI when WayPoint details show
        if ( isShow) return;

        if (missionProxy.poiList.size() == 0) {
            Toast.makeText(this, R.string.poi_item_empty, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        String[] poiNames = new String[missionProxy.poiList.size()];
        for (int i = 0; i < missionProxy.poiList.size(); i++) {
            poiNames[i] = missionProxy.poiList.get(i).getName();
        }

        final SupportEditInputDialog dialog = SupportEditInputDialog.newInstance(POI_ADD_DIALOG_TAG,
                getString(R.string.edit_pois), "Enter POI Name ...", true, poiNames);

        dialog.show(getSupportFragmentManager(), POI_EDIT_DIALOG_TAG);
    }

    @Override
    public void zoomToFitSelected() {
        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
        List<MissionItemProxy> selected = missionProxy.selection.getSelected();
        if (selected.isEmpty()) {
            planningMapFragment.zoomToFit();
        } else {
           // planningMapFragment.zoomToFit(MissionProxy.getVisibleCoords(selected));
            planningMapFragment.updateCamera(MissionProxy.getVisibleCoords(selected));
        }
    }

    @Override
    public void onListVisibilityChanged() {
    }

    @Override
    protected boolean enableMissionMenus() {
        return true;
    }

    @Override
    public void onSelectionUpdate(List<MissionItemProxy> selected) {
        EditorToolsImpl toolImpl = getToolImpl();
        toolImpl.onSelectionUpdate(selected);

        final boolean isEmpty = selected.isEmpty();

        if (isEmpty) {
//            itemDetailToggle.setVisibility(View.GONE);
//            removeItemDetail();
        } else {
            itemDetailToggle.setVisibility(View.VISIBLE);
            if (getTool() == EditorToolsFragment.EditorTools.SELECTOR)
                removeItemDetail();
            else {
                if (getTool() == EditorToolsFragment.EditorTools.POI) {
                    List<MissionItemProxy> list = missionProxy.selection.getSelected();
                    if (list != null && list.size() > 0) {
                        double lat = list.get(0).getMarkerInfos().get(0).getPosition().getLatitude();
                        double lon = list.get(0).getMarkerInfos().get(0).getPosition().getLongitude();
                        boolean isExist = false;
                        for (POIItem item : missionProxy.poiList) {
                            if (item.getLatitude() == lat && item.getLongitude() == lon) {
                                isExist = true;
                                break;
                            }
                        }
                        if (isExist) {
                            Toast.makeText(this, R.string.poi_already_exist, Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            showItemDetail(selectMissionDetailType(selected));
                            savePOIFile();
                        }
                    }
                } else {
                    showItemDetail(selectMissionDetailType(selected));
                }
            }
        }

        final EditorMapFragment planningMapFragment = gestureMapFragment.getMapFragment();
        if (planningMapFragment != null)
            planningMapFragment.postUpdate();
    }

}
