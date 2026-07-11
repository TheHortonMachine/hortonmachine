package org.hortonmachine.hmachine.geoframe.ermworkflow;

import java.io.File;

public class Paths {
	final String outputsDir;

	final String dtm;
	final String pit;
	final String flow;
	final String drain;
	final String tca;
	final String net;
	final String basin;
	final String basinResized;
	final String basinPit;
	final String basinDrain;
	final String basinTca;
	final String basinNet;
	final String skyview;
	final String basinSkyview;
	final String basinNetnum;
	final String basinNetbasins;
	final String basinNetbasinsDesired;

	final boolean overwrite;
	final String ext = ".tif";

	Paths(String inDtm, boolean overwrite) {
		this.overwrite = overwrite;
		dtm = inDtm;
		if (!new File(dtm).exists()) {
			throw new IllegalArgumentException("Input DTM not found at: " + dtm);
		}
		File folder = new File(dtm).getParentFile();
		outputsDir = new File(folder, "outputs").getAbsolutePath() + File.separator;
		if (!new File(outputsDir).exists()) {
			new File(outputsDir).mkdirs();
		}
		pit = outputsDir + "pit" + ext;
		flow = outputsDir + "flow" + ext;
		drain = outputsDir + "drain" + ext;
		tca = outputsDir + "tca" + ext;
		net = outputsDir + "net" + ext;
		skyview = outputsDir + "skyview" + ext;
		basin = outputsDir + "basin" + ext;
		basinResized = outputsDir + "basin_resized" + ext;
		basinPit = outputsDir + "basin_pit" + ext;
		basinDrain = outputsDir + "basin_drain" + ext;
		basinTca = outputsDir + "basin_tca" + ext;
		basinNet = outputsDir + "basin_net" + ext;
		basinSkyview = outputsDir + "basin_skyview" + ext;
		basinNetnum = outputsDir + "basin_netnum" + ext;
		basinNetbasins = outputsDir + "basin_netnumbasins" + ext;
		basinNetbasinsDesired = outputsDir + "basin_netnumbasins_desired" + ext;

	}

	public String getOutputsDir() {
		return outputsDir;
	}

	boolean shouldRun(String path) {
		return overwrite || !new File(path).exists();
	}
}