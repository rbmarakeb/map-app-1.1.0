package com.map.android.fragments.account.editor.tool;

import android.os.Bundle;

import com.map.android.lib.drone.mission.MissionItemType;
import com.map.android.lib.drone.mission.item.POIItem;
import com.map.android.lib.drone.mission.item.spatial.BaseSpatialItem;
import com.map.android.proxy.mission.item.MissionItemProxy;
import com.map.android.lib.coordinate.LatLong;

import com.map.android.dialogs.SupportYesNoDialog;
import com.map.android.proxy.mission.MissionProxy;
import com.map.android.proxy.mission.MissionSelection;

import java.util.List;

/**
 * Created by Fredia Huya-Kouadio on 8/25/15.
 */
public abstract class EditorToolsImpl implements MissionSelection.OnSelectionUpdateListener, SupportYesNoDialog.Listener {

    protected MissionProxy missionProxy;
    protected final EditorToolsFragment editorToolsFragment;
    protected MissionItemType selectedType;
    protected int selectedTypeIdx;

    EditorToolsImpl(EditorToolsFragment fragment) {
        this.editorToolsFragment = fragment;
        selectedTypeIdx = 0;
    }

    void setMissionProxy(MissionProxy missionProxy) {
        this.missionProxy = missionProxy;
    }

    void onSaveInstanceState(Bundle outState) {
    }

    void onRestoreInstanceState(Bundle savedState) {
    }

    public void setTypeIdx(int idx) {
        selectedTypeIdx = idx;
    }

    public void onMapClick(LatLong point) {
        if (missionProxy == null) return;

        // If an mission item is selected, unselect it.
      //  missionProxy.selection.clearSelection();
    }

    public void onMapClick(POIItem item) {
        if (missionProxy == null) return;

        // If an mission item is selected, unselect it.
        missionProxy.selection.clearSelection();

        if(selectedTypeIdx == 1) {
            selectedType = MarkerToolsImpl.MARKER_ITEMS_TYPE[selectedTypeIdx];
        }
        if (selectedType == null)
            return;

        BaseSpatialItem spatialItem = (BaseSpatialItem) selectedType.getNewItem();
        missionProxy.addSpatialWaypoint(spatialItem, new LatLong(item.getLatitude(), item.getLongitude()));
    }

    public void onListItemClick(MissionItemProxy item) {
        if (missionProxy == null)
            return;

        if (missionProxy.selection.selectionContains(item)) {
            missionProxy.selection.clearSelection();
        } else {
            editorToolsFragment.setTool(EditorToolsFragment.EditorTools.NONE);
            missionProxy.selection.setSelectionTo(item);
        }
    }

    public void onPathFinished(List<LatLong> path) {
    }

    @Override
    public void onSelectionUpdate(List<MissionItemProxy> selected) {

    }

    public abstract EditorToolsFragment.EditorTools getEditorTools();

    public abstract void setup();

    @Override
    public void onDialogYes(String dialogTag){

    }

    @Override
    public void onDialogNo(String dialogTag){

    }

}
