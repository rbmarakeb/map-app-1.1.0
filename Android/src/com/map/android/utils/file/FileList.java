package com.map.android.utils.file;

import java.io.File;
import java.io.FilenameFilter;

public class FileList {

    public static final String WAYPOINT_FILENAME_EXT = ".dpwp";

    public static final String PARAM_FILENAME_EXT = ".param";

	public static final String POI_FILENAME_EXT = ".txt";

	static public String[] getWaypointFileList() {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.contains(WAYPOINT_FILENAME_EXT);
			}
		};
		return getFileList(DirectoryPath.getWaypointsPath(), filter);
	}

	public static String[] getParametersFileList() {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.contains(PARAM_FILENAME_EXT);
			}
		};
		return getFileList(DirectoryPath.getParametersPath(), filter);
	}

	public static String[] getPOIFileList() {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.contains(POI_FILENAME_EXT);
			}
		};
		return getFileList(DirectoryPath.getParametersPath(), filter);
	}

	static public String[] getFileList(String path, FilenameFilter filter) {
		File mPath = new File(path);
		try {
			mPath.mkdirs();
			if (mPath.exists()) {
				return mPath.list(filter);
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return new String[0];
	}

}
