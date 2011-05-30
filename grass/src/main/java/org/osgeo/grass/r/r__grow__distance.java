package org.osgeo.grass.r;

import org.jgrasstools.grass.utils.ModuleSupporter;

import oms3.annotations.Author;
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.UI;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description("Generates a raster map layer of distance to features in input layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__grow__distance")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__grow__distance {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for distance output map (optional)")
	@In
	public String $$distancePARAMETER;

	@UI("outfile")
	@Description("Name for value output map (optional)")
	@In
	public String $$valuePARAMETER;

	@Description("Metric (optional)")
	@In
	public String $$metricPARAMETER = "euclidean";

	@Description("Allow output files to overwrite existing files")
	@In
	public boolean $$overwriteFLAG = false;

	@Description("Verbose module output")
	@In
	public boolean $$verboseFLAG = false;

	@Description("Quiet module output")
	@In
	public boolean $$quietFLAG = false;


	@Execute
	public void process() throws Exception {
		ModuleSupporter.processModule(this);
	}

}
