package com.map.android.dialogs.openfile;

import com.map.android.utils.file.IO.POIReader;

public abstract class OpenPOIDialog extends OpenFileDialog {
	public abstract void poiFileLoaded(POIReader reader);

	@Override
	protected FileReader createReader() {
		return new POIReader();
	}

	@Override
	protected void onDataLoaded(FileReader reader) {
		poiFileLoaded((POIReader) reader);
	}
}
