package com.map.android.lib.drone.mission;

import android.location.Location;
import android.os.Bundle;
import android.os.Parcel;

import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.mission.item.MissionItem;
import com.map.android.lib.drone.mission.item.spatial.BaseSpatialItem;
import com.map.android.lib.drone.property.DroneAttribute;
import com.map.android.lib.drone.property.Gps;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Holds a set of mission items.
 */
public class Mission implements DroneAttribute {

    private int currentMissionItem;
    private final List<MissionItem> missionItemsList = new ArrayList<MissionItem>();

    public Mission() {}

    public void addMissionItem(MissionItem missionItem){
        missionItemsList.add(missionItem);
    }

    public void addMissionItem(int index, MissionItem missionItem){
        missionItemsList.add(index, missionItem);
    }

    public void removeMissionItem(MissionItem missionItem){
        missionItemsList.remove(missionItem);
    }

    public void removeMissionItem(int index){
        missionItemsList.remove(index);
    }

    public void clear(){
        missionItemsList.clear();
    }

    public MissionItem getMissionItem(int index){
        return missionItemsList.get(index);
    }

    public List<MissionItem> getMissionItems(){
        return missionItemsList;
    }

    public int getCurrentMissionItem() {
        return currentMissionItem;
    }

    public void setCurrentMissionItem(int currentMissionItem) {
        this.currentMissionItem = currentMissionItem;
    }

    public String getDistance(int curIdx, double dlat, double dlong) {

        try{
        int nextIdx = curIdx;
        int testid=curIdx;
        if(curIdx < 0 || (nextIdx-1) > missionItemsList.size()) return "0";
        if(nextIdx == missionItemsList.size()) nextIdx = 0;
        if(curIdx >0) testid=curIdx - 1;
        Object obj = missionItemsList.get(testid);
        if(obj instanceof BaseSpatialItem) {
            BaseSpatialItem item1 = (BaseSpatialItem) obj;
            BaseSpatialItem item2 = (BaseSpatialItem) missionItemsList.get(nextIdx);

            Location location1 = new Location("1");
            location1.setLatitude(item1.getCoordinate().getLatitude());
            location1.setLongitude(item1.getCoordinate().getLongitude());

            Location location2 = new Location("2");
            location2.setLatitude(dlat);
            location2.setLongitude(dlong);

            return String.format(Locale.getDefault(), "%.2f", location1.distanceTo(location2) * 0.000539957);
        }

        return "0";}
        catch (Exception e) {
            // This will catch any exception, because they are all descended from Exception
            //System.out.println("Error " + e.getMessage());
            return "0";
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.currentMissionItem);

        List<Bundle> missionItemsBundles = new ArrayList<Bundle>(missionItemsList.size());
        if(!missionItemsList.isEmpty()){
            for(MissionItem missionItem : missionItemsList){
                missionItemsBundles.add(missionItem.getType().storeMissionItem(missionItem));
            }
        }

        dest.writeTypedList(missionItemsBundles);
    }

    private Mission(Parcel in) {
        this.currentMissionItem = in.readInt();

        List<Bundle> missionItemsBundles = new ArrayList<>();
        in.readTypedList(missionItemsBundles, Bundle.CREATOR);
        if(!missionItemsBundles.isEmpty()){
            for(Bundle bundle : missionItemsBundles){
                missionItemsList.add(MissionItemType.restoreMissionItemFromBundle(bundle));
            }
        }
    }

    public static final Creator<Mission> CREATOR = new Creator<Mission>() {
        public Mission createFromParcel(Parcel source) {
            return new Mission(source);
        }

        public Mission[] newArray(int size) {
            return new Mission[size];
        }
    };
}
