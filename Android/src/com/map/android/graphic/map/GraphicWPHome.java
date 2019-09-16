package com.map.android.graphic.map;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.map.android.R;
import com.map.android.client.Drone;
import com.map.android.client.apis.VehicleApi;
import com.map.android.lib.coordinate.LatLong;
import com.map.android.lib.coordinate.LatLongAlt;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.property.Home;
import com.map.android.lib.model.AbstractCommandListener;
import com.map.android.maps.MarkerInfo;

import timber.log.Timber;

public class GraphicWPHome extends MarkerInfo.SimpleMarkerInfo {

//	private final Drone drone;
	private final Context context;
	private LatLong mCoordinate;

	public GraphicWPHome(Context context, LatLong coordinate) {
//		this.drone = drone;
		this.context = context;
		this.mCoordinate = coordinate;
	}

	@Override
	public float getAnchorU() {
		return 0.5f;
	}

//	public boolean isValid() {
//        Home droneHome = drone.getAttribute(AttributeType.HOME);
//		return droneHome != null && droneHome.isValid();
//	}

	@Override
	public float getAnchorV() {
		return 0.5f;
	}

	@Override
	public Bitmap getIcon(Resources res) {
		return BitmapFactory.decodeResource(res, R.drawable.ic_home);
	}

	@Override
	public LatLong getPosition() {
//        Home droneHome = drone.getAttribute(AttributeType.HOME);
//        if(droneHome == null) return null;
//
//		return droneHome.getCoordinate();
		return mCoordinate;
	}

	@Override
	public void setPosition(LatLong position){
		//Move the home location
//		final Home currentHome = drone.getAttribute(AttributeType.HOME);
//		final LatLongAlt homeCoord = currentHome.getCoordinate();
//		final double homeAlt = homeCoord == null ? 0 : homeCoord.getAltitude();

//		final LatLongAlt newHome = new LatLongAlt(position, homeAlt);
//		VehicleApi.getApi(drone).setVehicleHome(newHome, new AbstractCommandListener() {
//			@Override
//			public void onSuccess() {
//				Timber.i("Updated home location to %s", newHome);
//				Toast.makeText(context, "Updated home location.", Toast.LENGTH_SHORT).show();
//			}
//
//			@Override
//			public void onError(int i) {
//				Timber.e("Unable to update home location: %d", i);
//			}
//
//			@Override
//			public void onTimeout() {
//				Timber.w("Home location update timed out.");
//			}
//		});
	}

	@Override
	public String getSnippet() {
//        Home droneHome = drone.getAttribute(AttributeType.HOME);
		return "Home";
	}

	@Override
	public String getTitle() {
		return "Home";
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public boolean isDraggable() {
		return true;
	}
}
