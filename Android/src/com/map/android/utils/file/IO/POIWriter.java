package com.map.android.utils.file.IO;

import android.util.Log;

import com.map.android.lib.drone.mission.Mission;
import com.map.android.lib.drone.mission.item.POIItem;
import com.map.android.lib.util.ParcelableUtils;
import com.map.android.utils.file.FileList;
import com.map.android.utils.file.FileStream;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Write a mission to file.
 */
public class POIWriter {
	private static final String TAG = POIWriter.class.getSimpleName();

	public static boolean write(POIItem item, boolean isAppend) {
		return write(item, isAppend, FileStream.getPOIFilename("pois"));
	}

	public static boolean write(List<POIItem> list, boolean isAppend) {
		return write(list, isAppend, FileStream.getPOIFilename("pois"));
	}

	public static boolean write(List<POIItem> list, boolean isAppend, String filename) {
		try {
			if (!FileStream.isExternalStorageAvailable())
				return false;

			if (!filename.endsWith(FileList.POI_FILENAME_EXT)) {
				filename += FileList.POI_FILENAME_EXT;
			}

			final FileOutputStream out = FileStream.getPOIFileStream(filename, isAppend);
			for (POIItem item : list) {
				byte[] poiBytes = item.toString().getBytes();
				out.write(poiBytes);
			}
			out.close();

		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		}
		return true;
	}

	public static boolean write(POIItem item, boolean isAppend, String filename) {
		try {
			if (!FileStream.isExternalStorageAvailable())
				return false;

			if (!filename.endsWith(FileList.POI_FILENAME_EXT)) {
				filename += FileList.POI_FILENAME_EXT;
			}

			final FileOutputStream out = FileStream.getPOIFileStream(filename, isAppend);
            byte[] poiBytes = item.toString().getBytes();
            out.write(poiBytes);
			out.close();

		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		}
		return true;
	}
}
