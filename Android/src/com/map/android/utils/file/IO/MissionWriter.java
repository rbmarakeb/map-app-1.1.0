package com.map.android.utils.file.IO;

import android.util.Log;

import com.map.android.utils.file.FileList;
import com.map.android.utils.file.FileStream;
import com.map.android.lib.drone.mission.Mission;
import com.map.android.lib.util.ParcelableUtils;

import java.io.FileOutputStream;

/**
 * Write a mission to file.
 */
public class MissionWriter {
	private static final String TAG = MissionWriter.class.getSimpleName();

	public static boolean write(Mission mission) {
		return write(mission, FileStream.getWaypointFilename("waypoints"));
	}

	public static boolean write(Mission mission, String filename) {
		try {
			if (!FileStream.isExternalStorageAvailable())
				return false;

			if (!filename.endsWith(FileList.WAYPOINT_FILENAME_EXT)) {
				filename += FileList.WAYPOINT_FILENAME_EXT;
			}

			final FileOutputStream out = FileStream.getWaypointFileStream(filename);
            byte[] missionBytes = ParcelableUtils.marshall(mission);
            out.write(missionBytes);
			out.close();

		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		}
		return true;
	}
}
