package com.map.android.dialogs.openfile;

import com.map.android.utils.file.IO.ParameterReader;
import com.map.android.lib.drone.property.Parameter;

import java.util.List;

public abstract class OpenParameterDialog extends OpenFileDialog {
	public abstract void parameterFileLoaded(List<Parameter> parameters);

	@Override
	protected FileReader createReader() {
		return new ParameterReader();
	}

	@Override
	protected void onDataLoaded(FileReader reader) {
		parameterFileLoaded(((ParameterReader) reader).getParameters());
	}
}