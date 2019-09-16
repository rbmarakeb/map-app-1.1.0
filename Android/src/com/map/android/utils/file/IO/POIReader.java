package com.map.android.utils.file.IO;

import android.util.Log;

import com.map.android.dialogs.openfile.OpenFileDialog;
import com.map.android.lib.drone.mission.Mission;
import com.map.android.lib.drone.mission.item.POIItem;
import com.map.android.lib.util.ParcelableUtils;
import com.map.android.utils.file.DirectoryPath;
import com.map.android.utils.file.FileList;
import com.map.android.utils.file.FileStream;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read a mission from a file.
 */
public class POIReader implements OpenFileDialog.FileReader {

    private static final String TAG = POIReader.class.getSimpleName();

	private List<POIItem> poiList = new ArrayList<>();

	public boolean openPOI(String file) {
		if (!FileStream.isExternalStorageAvailable()) {
			return false;
		}
		poiList.clear();
		try {
			final FileInputStream in = new FileInputStream(DirectoryPath.getPOIsPath() + file);
            Map<byte[], Integer> bytesList = new LinkedHashMap<byte[], Integer>();
            int length = 0;
            StringBuffer buf = new StringBuffer();
            while(in.available() > 0){
                byte[] missionBytes = new byte[2048];
                int bufferSize = in.read(missionBytes);
                buf.append(new String(missionBytes, "UTF-8"));
                length += bufferSize;
            }
			String[] lines = buf.toString().trim().split("\n");
            for (String line : lines) {
				String[] tokens = line.trim().split(",");
				if (tokens.length != 3) continue;
            	POIItem item = new POIItem(tokens);
            	poiList.add(item);
			}
			in.close();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		}

		return true;
	}

	public List<POIItem> getPOIList() {
		return poiList;
	}

	@Override
	public String getPath() {
		return DirectoryPath.getWaypointsPath();
	}

	@Override
	public String[] getFileList() {
		return FileList.getWaypointFileList();
	}

	@Override
	public boolean openFile(String file) {
		return openPOI(file);
	}
}
