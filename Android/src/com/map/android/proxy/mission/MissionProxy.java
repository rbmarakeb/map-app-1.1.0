package com.map.android.proxy.mission;

import android.support.v7.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.CircularArray;
import android.util.Pair;

import com.google.android.gms.analytics.HitBuilders;
import com.map.android.lib.drone.mission.item.POIItem;
import com.map.android.proxy.mission.item.MissionItemProxy;
import com.map.android.proxy.mission.item.markers.MissionItemMarkerInfo;
import com.map.android.proxy.mission.item.markers.PolygonMarkerInfo;
import com.map.android.proxy.mission.item.markers.SurveyMarkerInfoProvider;
import com.map.android.utils.Utils;
import com.map.android.utils.analytics.GAUtils;
import com.map.android.utils.file.FileStream;
import com.map.android.utils.file.IO.MissionReader;
import com.map.android.utils.file.IO.POIReader;
import com.map.android.utils.file.IO.POIWriter;
import com.map.android.utils.prefs.DroidPlannerPrefs;
import com.map.android.client.Drone;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.coordinate.LatLongAlt;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.mission.Mission;
import com.map.android.lib.drone.mission.MissionItemType;
import com.map.android.lib.drone.mission.item.MissionItem;
import com.map.android.lib.drone.mission.item.MissionItem.SpatialItem;
import com.map.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.map.android.lib.drone.mission.item.command.Takeoff;
import com.map.android.lib.drone.mission.item.complex.SplineSurvey;
import com.map.android.lib.drone.mission.item.complex.StructureScanner;
import com.map.android.lib.drone.mission.item.complex.Survey;
import com.map.android.lib.drone.mission.item.complex.SurveyDetail;
import com.map.android.lib.drone.mission.item.spatial.BaseSpatialItem;
import com.map.android.lib.drone.mission.item.spatial.RegionOfInterest;
import com.map.android.lib.drone.mission.item.spatial.SplineWaypoint;
import com.map.android.lib.drone.mission.item.spatial.Waypoint;
import com.map.android.lib.util.MathUtils;

import com.map.android.maps.DPMap;
import com.map.android.maps.MarkerInfo;
import com.map.android.utils.file.IO.MissionWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is used as a wrapper to {@link com.map.android.lib.drone.mission.Mission}
 * object on the Android side.
 */
public class MissionProxy implements DPMap.PathSource {

    public static final String ACTION_MISSION_PROXY_UPDATE = Utils.PACKAGE_NAME + ".ACTION_MISSION_PROXY_UPDATE";
    public static final String ACTION_POI_UPDATE = Utils.PACKAGE_NAME + ".ACTION_POI_UPDATE";

    private static final int UNDO_BUFFER_SIZE = 30;

    private static final IntentFilter eventFilter = new IntentFilter();

    static {
        eventFilter.addAction(AttributeEvent.MISSION_DRONIE_CREATED);
        eventFilter.addAction(AttributeEvent.MISSION_UPDATED);
        eventFilter.addAction(AttributeEvent.MISSION_RECEIVED);
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AttributeEvent.MISSION_DRONIE_CREATED.equals(action)
                    || AttributeEvent.MISSION_UPDATED.equals(action)
                    || AttributeEvent.MISSION_RECEIVED.equals(action)) {
                Mission droneMission = drone.getAttribute(AttributeType.MISSION);
                load(droneMission);
            }
        }
    };

    private final Drone.OnMissionItemsBuiltCallback missionItemsBuiltListener = new Drone.OnMissionItemsBuiltCallback() {
        @Override
        public void onMissionItemsBuilt(MissionItem.ComplexItem[] complexItems) {
            notifyMissionUpdate(false);
        }
    };

    /**
     * Stores all the mission item renders for this mission render.
     */
    private final List<MissionItemProxy> missionItemProxies = new ArrayList<MissionItemProxy>();

    private final LocalBroadcastManager lbm;
    private final DroidPlannerPrefs dpPrefs;
    private Drone drone;

    private final CircularArray<Mission> undoBuffer = new CircularArray<>(UNDO_BUFFER_SIZE);

    private Mission currentMission;
    public MissionSelection selection = new MissionSelection();

    public List<POIItem> poiList = new ArrayList<>();

    public MissionProxy(Context context, Drone drone) {
        this.drone = drone;
        this.currentMission = generateMission(true);
        lbm = LocalBroadcastManager.getInstance(context);
        lbm.registerReceiver(eventReceiver, eventFilter);

        dpPrefs = DroidPlannerPrefs.getInstance(context);
    }

    public void setDrone(Drone drone){
        this.drone = drone;
    }

    public void notifyMissionUpdate() {
        notifyMissionUpdate(true);
    }

    public boolean canUndoMission() {
        return !undoBuffer.isEmpty();
    }

    public void undoMission() {
        if (!canUndoMission())
            throw new IllegalStateException("Invalid state for mission undoing.");

        Mission previousMission = undoBuffer.popLast();
        load(previousMission, false);
    }

    public void notifyMissionUpdate(boolean saveMission) {
        if (saveMission && currentMission != null) {
            //Store the current state of the mission.
            undoBuffer.addLast(currentMission);
        }

        currentMission = generateMission(true);
        lbm.sendBroadcast(new Intent(ACTION_MISSION_PROXY_UPDATE));
    }

    public void notifyPOIUpdate(POIItem old, POIItem newItem) {
//        if (saveMission && currentMission != null) {
//            //Store the current state of the mission.
//            undoBuffer.addLast(currentMission);
//        }

//        currentMission = generateMission(true);
        poiList.remove(old);
        poiList.add(newItem);

        POIWriter.write(poiList, false);
        lbm.sendBroadcast(new Intent(ACTION_POI_UPDATE));
    }

    public List<MissionItemProxy> getItems() {
        return missionItemProxies;
    }

    private MissionItem[] getMissionItems() {
        List<MissionItem> missionItems = new ArrayList<MissionItem>(missionItemProxies.size());
        for (MissionItemProxy mip : missionItemProxies)
            missionItems.add(mip.getMissionItem());

        return missionItems.toArray(new MissionItem[missionItems.size()]);
    }

    public Drone getDrone() {
        return this.drone;
    }

    /**
     * @return the map markers corresponding to this mission's command set.
     */
    public List<MarkerInfo> getMarkersInfos() {
        List<MarkerInfo> markerInfos = new ArrayList<MarkerInfo>();

        for (MissionItemProxy itemProxy : missionItemProxies) {
            List<MarkerInfo> itemMarkerInfos = itemProxy.getMarkerInfos();
            if (itemMarkerInfos != null && !itemMarkerInfos.isEmpty()) {
                markerInfos.addAll(itemMarkerInfos);
            }
        }
        return markerInfos;
    }

    public List<POIItem> getPoiList() {
        return poiList;
    }

    public void openPOIFile() {
        POIReader reader = new POIReader();
        reader.openPOI(FileStream.getPOIFilename("pois"));
        readPOIFromFile(reader);
    }

    /**
     * Update the state for this object based on the state of the Mission
     * object.
     */
    public void load(Mission mission) {
        load(mission, true);
    }

    private void load(Mission mission, boolean isNew) {
        if (mission == null)
            return;

        if (isNew) {
            currentMission = null;
            clearUndoBuffer();
        }

        selection.mSelectedItems.clear();
        missionItemProxies.clear();

        for (MissionItem item : mission.getMissionItems()) {
            missionItemProxies.add(new MissionItemProxy(this, item));
        }

        selection.notifySelectionUpdate();

        notifyMissionUpdate(isNew);
    }

    private void clearUndoBuffer(){
        while(!undoBuffer.isEmpty())
            undoBuffer.popLast();
    }

    /**
     * Checks if this mission render contains the passed argument.
     *
     * @param item mission item render object
     * @return true if this mission render contains the passed argument
     */
    public boolean contains(MissionItemProxy item) {
        return missionItemProxies.contains(item);
    }

    /**
     * Removes a waypoint mission item from the set of mission items commands.
     *
     * @param item item to remove
     */
    public void removeItem(MissionItemProxy item) {
        missionItemProxies.remove(item);
        selection.mSelectedItems.remove(item);

        selection.notifySelectionUpdate();
        notifyMissionUpdate();
    }

    public void removePOIItem(POIItem one) {
        for (POIItem item : poiList) {
            if (one.getName().equals(item.getName())) {
                poiList.remove(item);
                break;
            }
        }
        POIWriter.write(poiList, false);
        lbm.sendBroadcast(new Intent(ACTION_POI_UPDATE));
    }
    /**
     * Adds a survey mission item to the set.
     *
     * @param points 2D points making up the survey
     */
    public void addSurveyPolygon(List<LatLong> points, boolean spline) {
        Survey survey;
        if(spline){
            survey = new SplineSurvey();
        }else {
            survey = new Survey();
        }
        survey.setPolygonPoints(points);
        addMissionItem(survey);
    }

    /**
     * Add a set of waypoints generated around the passed 2D points.
     *
     * @param points list of points used to generate the mission waypoints
     */
    public void addWaypoints(List<LatLong> points) {
        double alt = getLastAltitude();
        List<MissionItem> missionItemsToAdd = new ArrayList<MissionItem>(points.size());
        for (LatLong point : points) {
            Waypoint waypoint = new Waypoint();
            waypoint.setCoordinate(new LatLongAlt(point.getLatitude(), point.getLongitude(),
                    (float) alt));
            missionItemsToAdd.add(waypoint);

        }

        addMissionItems(missionItemsToAdd);
    }

    public double getLastAltitude() {
        if (!missionItemProxies.isEmpty()) {
            MissionItem lastItem = missionItemProxies.get(missionItemProxies.size() - 1).getMissionItem();
            if (lastItem instanceof MissionItem.SpatialItem
                    && !(lastItem instanceof RegionOfInterest)) {
                return ((MissionItem.SpatialItem) lastItem).getCoordinate().getAltitude();
            }
        }

        return dpPrefs.getDefaultAltitude();
    }

    /**
     * Add a set of spline waypoints generated around the passed 2D points.
     *
     * @param points list of points used as location for the spline waypoints
     */
    public void addSplineWaypoints(List<LatLong> points) {
        double alt = getLastAltitude();
        List<MissionItem> missionItemsToAdd = new ArrayList<MissionItem>(points.size());
        for (LatLong point : points) {
            SplineWaypoint splineWaypoint = new SplineWaypoint();
            splineWaypoint.setCoordinate(new LatLongAlt(point.getLatitude(), point.getLongitude(),
                    (float) alt));
            missionItemsToAdd.add(splineWaypoint);
        }

        addMissionItems(missionItemsToAdd);
    }

    private void addMissionItems(List<MissionItem> missionItems) {
        for (MissionItem missionItem : missionItems) {
            missionItemProxies.add(new MissionItemProxy(this, missionItem));
        }

        notifyMissionUpdate();
    }

    public void addSpatialWaypoint(BaseSpatialItem spatialItem, LatLong point) {
        double alt = getLastAltitude();
        spatialItem.setCoordinate(new LatLongAlt(point.getLatitude(), point.getLongitude(), alt));
        addMissionItem(spatialItem);
    }

    /**
     * Add a waypoint generated around the passed 2D point.
     *
     * @param point point used to generate the mission waypoint
     */
    public void addWaypoint(LatLong point) {
        double alt = getLastAltitude();
        Waypoint waypoint = new Waypoint();
        waypoint.setCoordinate(new LatLongAlt(point.getLatitude(), point.getLongitude(), alt));
        addMissionItem(waypoint);
    }

    /**
     * Add a spline waypoint generated around the passed 2D point.
     *
     * @param point point used as location for the spline waypoint.
     */
    public void addSplineWaypoint(LatLong point) {
        double alt = getLastAltitude();
        SplineWaypoint splineWaypoint = new SplineWaypoint();
        splineWaypoint.setCoordinate(new LatLongAlt(point.getLatitude(), point.getLongitude(), alt));
        addMissionItem(splineWaypoint);
    }

    private void addMissionItem(MissionItem missionItem) {
        missionItemProxies.add(new MissionItemProxy(this, missionItem));
        notifyMissionUpdate();
    }

    private void addMissionItem(int index, MissionItem missionItem) {
        missionItemProxies.add(index, new MissionItemProxy(this, missionItem));
        notifyMissionUpdate();
    }

    public void addTakeoff() {
        Takeoff takeoff = new Takeoff();
        takeoff.setTakeoffAltitude(dpPrefs.getDefaultAltitude());
        addMissionItem(takeoff);
    }

    public boolean hasTakeoffAndLandOrRTL() {
        if (missionItemProxies.size() >= 2) {
            if (isFirstItemTakeoff() && isLastItemLandOrRTL()) {
                return true;
            }
        }
        return false;
    }

    public boolean isFirstItemTakeoff() {
        return !missionItemProxies.isEmpty() && missionItemProxies.get(0).getMissionItem().getType() ==
                MissionItemType.TAKEOFF;
    }

    public boolean isLastItemLandOrRTL() {
        int itemsCount = missionItemProxies.size();
        if (itemsCount == 0) return false;

        MissionItemType itemType = missionItemProxies.get(itemsCount - 1).getMissionItem()
                .getType();
        return itemType == MissionItemType.RETURN_TO_LAUNCH || itemType == MissionItemType.LAND;
    }

    public void addTakeOffAndRTL() {
        if (!isFirstItemTakeoff()) {
            double defaultAlt = dpPrefs.getDefaultAltitude();
            if (!missionItemProxies.isEmpty()) {
                MissionItem firstItem = missionItemProxies.get(0).getMissionItem();
                if (firstItem instanceof MissionItem.SpatialItem)
                    defaultAlt = ((MissionItem.SpatialItem) firstItem).getCoordinate().getAltitude();
                else if (firstItem instanceof Survey) {
                    SurveyDetail surveyDetail = ((Survey) firstItem).getSurveyDetail();
                    if (surveyDetail != null)
                        defaultAlt = surveyDetail.getAltitude();
                }
            }

            Takeoff takeOff = new Takeoff();
            takeOff.setTakeoffAltitude(defaultAlt);
            addMissionItem(0, takeOff);
        }

        if (!isLastItemLandOrRTL()) {
            ReturnToLaunch rtl = new ReturnToLaunch();
            addMissionItem(rtl);
        }
    }

    /**
     * Returns the order for the given argument in the mission set.
     *
     * @param item
     * @return order of the given argument
     */
    public int getOrder(MissionItemProxy item) {
        return missionItemProxies.indexOf(item) + 1;
    }

    /**
     * @return The order of the first waypoint.
     */
    public int getFirstWaypoint(){
        List<MarkerInfo> markerInfos = getMarkersInfos();

        if(!markerInfos.isEmpty()) {
            MarkerInfo markerInfo = markerInfos.get(0);
            if(markerInfo instanceof MissionItemMarkerInfo){
                return getOrder(((MissionItemMarkerInfo)markerInfo).getMarkerOrigin());
            }
            else if(markerInfo instanceof SurveyMarkerInfoProvider){
                return getOrder(((SurveyMarkerInfoProvider)markerInfo).getMarkerOrigin());
            }
            else if(markerInfo instanceof PolygonMarkerInfo){
                return getOrder(((PolygonMarkerInfo)markerInfo).getMarkerOrigin());
            }
        }

        return 0;
    }

    /**
     * @return The order for the last waypoint.
     */
    public int getLastWaypoint(){
        List<MarkerInfo> markerInfos = getMarkersInfos();

        if(!markerInfos.isEmpty()) {
            MarkerInfo markerInfo = markerInfos.get(markerInfos.size() - 1);
            if(markerInfo instanceof MissionItemMarkerInfo){
                return getOrder(((MissionItemMarkerInfo)markerInfo).getMarkerOrigin());
            }
            else if(markerInfo instanceof SurveyMarkerInfoProvider){
                return getOrder(((SurveyMarkerInfoProvider)markerInfo).getMarkerOrigin());
            }
            else if(markerInfo instanceof PolygonMarkerInfo){
                return getOrder(((PolygonMarkerInfo)markerInfo).getMarkerOrigin());
            }
        }
        return 0;
    }

    /**
     * Updates a mission item render
     *
     * @param oldItem mission item render to update
     * @param newItem new mission item render
     */
    public void replace(MissionItemProxy oldItem, MissionItemProxy newItem) {
        int index = missionItemProxies.indexOf(oldItem);
        if (index == -1)
            return;

        missionItemProxies.remove(index);
        missionItemProxies.add(index, newItem);

        if (selection.selectionContains(oldItem)) {
            selection.removeItemFromSelection(oldItem);
            selection.addToSelection(newItem);
        }

        notifyMissionUpdate();
    }

    public void replaceAll(List<Pair<MissionItemProxy, List<MissionItemProxy>>> oldNewList) {
        if (oldNewList == null) {
            return;
        }

        int pairSize = oldNewList.size();
        if (pairSize == 0) {
            return;
        }

        List<MissionItemProxy> selectionsToRemove = new ArrayList<>(pairSize);
        List<MissionItemProxy> itemsToSelect = new ArrayList<>(pairSize);

        for (int i = 0; i < pairSize; i++) {
            MissionItemProxy oldItem = oldNewList.get(i).first;
            int index = missionItemProxies.indexOf(oldItem);
            if (index == -1) {
                continue;
            }

            missionItemProxies.remove(index);

            List<MissionItemProxy> newItems = oldNewList.get(i).second;
            missionItemProxies.addAll(index, newItems);

            if (selection.selectionContains(oldItem)) {
                selectionsToRemove.add(oldItem);
                itemsToSelect.addAll(newItems);
            }
        }

        //Update the selection list.
        selection.removeItemsFromSelection(selectionsToRemove);
        selection.addToSelection(itemsToSelect);

        notifyMissionUpdate();
    }

    /**
     * Reverse the order of the mission items renders.
     */
    public void reverse() {
        Collections.reverse(missionItemProxies);
    }

    public void swap(int fromIndex, int toIndex) {
        MissionItemProxy from = missionItemProxies.get(fromIndex);
        MissionItemProxy to = missionItemProxies.get(toIndex);

        missionItemProxies.set(toIndex, from);
        missionItemProxies.set(fromIndex, to);
        notifyMissionUpdate();
    }

    public void clear() {
        selection.clearSelection();
        missionItemProxies.clear();
        notifyMissionUpdate();
    }

    public double getAltitudeDiffFromPreviousItem(MissionItemProxy waypointRender) {
        int itemsCount = missionItemProxies.size();
        if (itemsCount < 2)
            return 0;

        MissionItem waypoint = waypointRender.getMissionItem();
        if (!(waypoint instanceof MissionItem.SpatialItem))
            return 0;

        int index = missionItemProxies.indexOf(waypointRender);
        if (index == -1 || index == 0)
            return 0;

        MissionItem previous = missionItemProxies.get(index - 1).getMissionItem();
        if (previous instanceof MissionItem.SpatialItem) {
            return ((MissionItem.SpatialItem) waypoint).getCoordinate().getAltitude()
                    - ((MissionItem.SpatialItem) previous).getCoordinate().getAltitude();
        }

        return 0;
    }

    public double getDistanceFromLastWaypoint(MissionItemProxy waypointRender) {
        if (missionItemProxies.size() < 2)
            return 0;

        MissionItem waypoint = waypointRender.getMissionItem();
        if (!(waypoint instanceof MissionItem.SpatialItem))
            return 0;

        int index = missionItemProxies.indexOf(waypointRender);
        if (index == -1 || index == 0)
            return 0;

        MissionItem previous = missionItemProxies.get(index - 1).getMissionItem();
        if (previous instanceof MissionItem.SpatialItem) {
            return MathUtils.getDistance3D(((MissionItem.SpatialItem) waypoint).getCoordinate(),
                    ((MissionItem.SpatialItem) previous).getCoordinate());
        }

        return 0;
    }

    @Override
    public List<LatLong> getPathPoints() {
        if (missionItemProxies.isEmpty()) {
            return Collections.emptyList();
        }

        // Partition the mission items into spline/non-spline buckets.
        List<Pair<Boolean, List<MissionItemProxy>>> bucketsList = new ArrayList<>();

        boolean isSpline = false;
        List<MissionItemProxy> currentBucket = new ArrayList<>();
        for (MissionItemProxy missionItemProxy : missionItemProxies) {

            MissionItem missionItem = missionItemProxy.getMissionItem();
            if (missionItem instanceof MissionItem.Command) {
                //Skip commands
                continue;
            }

            if (missionItem instanceof SplineWaypoint || missionItem instanceof SplineSurvey) {
                if (!isSpline) {
                    if (!currentBucket.isEmpty()) {
                        // Get the last item from the current bucket. It will become the first
                        // anchor point for the spline path.
                        MissionItemProxy lastItem = currentBucket.get(currentBucket.size() - 1);

                        // Store the previous item bucket.
                        bucketsList.add(new Pair<>(Boolean.FALSE, currentBucket));

                        // Create a new bucket for this category and update 'isSpline'
                        currentBucket = new ArrayList<>();
                        currentBucket.add(lastItem);
                    }

                    isSpline = true;
                }

                // Add the current element into the bucket
                currentBucket.add(missionItemProxy);
            } else {
                if (isSpline) {

                    // Add the current item to the spline bucket. It will act as the end anchor
                    // point for the spline path.
                    if (!currentBucket.isEmpty()) {
                        currentBucket.add(missionItemProxy);

                        // Store the previous item bucket.
                        bucketsList.add(new Pair<>(Boolean.TRUE, currentBucket));

                        currentBucket = new ArrayList<>();
                    }

                    isSpline = false;
                }

                // Add the current element into the bucket
                currentBucket.add(missionItemProxy);
            }
        }

        bucketsList.add(new Pair<>(isSpline, currentBucket));

        List<LatLong> pathPoints = new ArrayList<>();
        LatLong lastPoint = null;

        for (Pair<Boolean, List<MissionItemProxy>> bucketEntry : bucketsList) {

            List<MissionItemProxy> bucket = bucketEntry.second;
            if (bucketEntry.first) {
                List<LatLong> splinePoints = new ArrayList<>();
                int bucketSize = bucket.size();
                for(int i = 0; i < bucketSize; i++){
                    MissionItemProxy missionItemProxy = bucket.get(i);
                    MissionItemType missionItemType = missionItemProxy.getMissionItem().getType();
                    List<LatLong> missionItemPath = missionItemProxy.getPath(lastPoint);

                    switch(missionItemType){
                        case SURVEY:
                            if(!missionItemPath.isEmpty()) {
                                if (i == 0)
                                    splinePoints.add(missionItemPath.get(0));
                                else {
                                    splinePoints.add(missionItemPath.get(missionItemPath.size() - 1));
                                }
                            }
                            break;

                        default:
                            splinePoints.addAll(missionItemPath);
                            break;
                    }

                    if (!splinePoints.isEmpty()) {
                        lastPoint = splinePoints.get(splinePoints.size() - 1);
                    }
                }

                pathPoints.addAll(MathUtils.SplinePath.process(splinePoints));
            }
            else {
                for (MissionItemProxy missionItemProxy : bucket) {
                    pathPoints.addAll(missionItemProxy.getPath(lastPoint));

                    if (!pathPoints.isEmpty()) {
                        lastPoint = pathPoints.get(pathPoints.size() - 1);
                    }
                }
            }
        }

        return pathPoints;
    }

    public void removeSelection(MissionSelection missionSelection) {
        missionItemProxies.removeAll(missionSelection.mSelectedItems);
        missionSelection.clearSelection();
        notifyMissionUpdate();
    }

    public void move(MissionItemProxy item, LatLong position) {
        MissionItem missionItem = item.getMissionItem();
        if (missionItem instanceof SpatialItem) {
            SpatialItem spatialItem = (SpatialItem) missionItem;
            spatialItem.setCoordinate(new LatLongAlt(position.getLatitude(),
                    position.getLongitude(), spatialItem.getCoordinate().getAltitude()));

            if (spatialItem instanceof StructureScanner) {
                this.drone.buildMissionItemsAsync(new StructureScanner[]{(StructureScanner) spatialItem},
                        missionItemsBuiltListener);
            }

            notifyMissionUpdate();
        }
    }

    public List<LatLong> getVisibleCoords() {
        return getVisibleCoords(missionItemProxies);
    }

    public void movePolygonPoint(Survey survey, int index, LatLong position) {
        survey.getPolygonPoints().get(index).set(position);
        this.drone.buildMissionItemsAsync(new Survey[]{survey}, missionItemsBuiltListener);
        notifyMissionUpdate();
    }

    public static List<LatLong> getVisibleCoords(List<MissionItemProxy> mipList) {
        List<LatLong> coords = new ArrayList<LatLong>();

        if (mipList == null || mipList.isEmpty()) {
            return coords;
        }

        for (MissionItemProxy itemProxy : mipList) {
            MissionItem item = itemProxy.getMissionItem();
            if (!(item instanceof SpatialItem))
                continue;

            LatLong coordinate = ((SpatialItem) item).getCoordinate();
            if (coordinate.getLatitude() == 0 || coordinate.getLongitude() == 0)
                continue;

            coords.add(coordinate);
        }

        return coords;
    }

    private Mission generateMission() {
        return generateMission(false);
    }

    private Mission generateMission(boolean isDeepCopy) {
        Mission mission = new Mission();

        if (!missionItemProxies.isEmpty()) {
            for (MissionItemProxy itemProxy : missionItemProxies) {
                MissionItem sourceItem = itemProxy.getMissionItem();
                MissionItem destItem = isDeepCopy ? sourceItem.clone() : sourceItem;
                mission.addMissionItem(destItem);
            }
        }

        return mission;
    }

    public void sendMissionToAPM(Drone drone) {
        drone.setMission(generateMission(), true);

        int missionItemsCount = missionItemProxies.size();

        String missionItemsList = "[";
        if (missionItemsCount > 0) {
            boolean isFirst = true;
            for (MissionItemProxy itemProxy : missionItemProxies) {
                if (isFirst)
                    isFirst = false;
                else
                    missionItemsList += ", ";

                missionItemsList += itemProxy.getMissionItem().getType().getLabel();
            }
        }

        missionItemsList += "]";

        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(GAUtils.Category.MISSION_PLANNING)
                .setAction("Mission sent to drone")
                .setLabel("Mission items: " + missionItemsList);
        GAUtils.sendEvent(eventBuilder);

        //Send an event for the created mission
        eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(GAUtils.Category.MISSION_PLANNING)
                .setAction("Mission sent to drone")
                .setLabel("Mission items count")
                .setValue(missionItemsCount);
        GAUtils.sendEvent(eventBuilder);
    }

    public double getMissionLength() {
        List<LatLong> points = getPathPoints();
        double length = 0;
        if (points.size() > 1) {
            for (int i = 1; i < points.size(); i++) {
                length += MathUtils.getDistance2D(points.get(i - 1), points.get(i));
            }
        }

        return length;
    }

    public void makeAndUploadDronie(Drone drone) {
        drone.generateDronie();
    }

    public List<List<LatLong>> getPolygonsPath() {
        ArrayList<List<LatLong>> polygonPaths = new ArrayList<List<LatLong>>();
        for (MissionItemProxy itemProxy : missionItemProxies) {
            MissionItem item = itemProxy.getMissionItem();
            if (item instanceof Survey) {
                polygonPaths.add(((Survey) item).getPolygonPoints());
            }
        }
        return polygonPaths;
    }

    public boolean writeMissionToFile(String filename) {
        return MissionWriter.write(generateMission(), filename);
    }

    public boolean readMissionFromFile(MissionReader reader) {
        if (reader == null)
            return false;

        Mission mission = reader.getMission();
        drone.setMission(mission, false);

        load(mission);
        return true;
    }

    public boolean writePOIToFile(POIItem item) {
        poiList.add(item);
        return POIWriter.write(item, true);
    }

    public boolean readPOIFromFile(POIReader reader) {
        if (reader == null)
            return false;

        this.poiList = reader.getPOIList();
        return true;
    }
}
