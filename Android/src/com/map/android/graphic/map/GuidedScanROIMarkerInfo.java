package com.map.android.graphic.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.map.android.maps.MarkerInfo;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.coordinate.LatLongAlt;

import com.map.android.R;

/**
 * Created by Fredia Huya-Kouadio on 1/27/15.
 */
public class GuidedScanROIMarkerInfo extends MarkerInfo.SimpleMarkerInfo {

    public static final double DEFAULT_FOLLOW_ROI_ALTITUDE = 10; //meters
    private LatLongAlt roiCoord;

    @Override
    public void setPosition(LatLong coord){
        if(coord == null || coord instanceof LatLongAlt){
            roiCoord = (LatLongAlt) coord;
        }
        else {
            double defaultHeight = DEFAULT_FOLLOW_ROI_ALTITUDE;
            if(roiCoord != null)
                defaultHeight = roiCoord.getAltitude();

            this.roiCoord = new LatLongAlt(coord.getLatitude(), coord.getLongitude(), defaultHeight);
        }
    }

    @Override
    public LatLongAlt getPosition(){
        return roiCoord;
    }

    @Override
    public Bitmap getIcon(Resources res){
        return BitmapFactory.decodeResource(res, R.drawable.ic_roi);
    }

    @Override
    public boolean isVisible(){
        return roiCoord != null;
    }

    @Override
    public float getAnchorU() {
        return 0.5f;
    }

    @Override
    public float getAnchorV() {
        return 0.5f;
    }
}
