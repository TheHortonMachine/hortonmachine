package org.osgeo.grass.i;

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

@Description("Zero-crossing \"edge detection\" raster function for image processing.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery")
@Label("Grass Imagery Modules")
@Name("i__zc")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__zc {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Zero crossing raster map")
	@In
	public String $$outputPARAMETER;

	@Description("x-y extent of the Gaussian filter (optional)")
	@In
	public String $$widthPARAMETER = "9";

	@Description("Sensitivity of Gaussian filter (optional)")
	@In
	public String $$thresholdPARAMETER = "10";

	@Description("Number of azimuth directions categorized (optional)")
	@In
	public String $$orientationsPARAMETER = "1";

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
