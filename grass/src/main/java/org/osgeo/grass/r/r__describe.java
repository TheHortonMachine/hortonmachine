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

@Description("Prints terse list of category values found in a raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__describe")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__describe {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$mapPARAMETER;

	@Description("String representing no data cell value (optional)")
	@In
	public String $$nvPARAMETER = "*";

	@Description("Number of quantization steps (optional)")
	@In
	public String $$nstepsPARAMETER = "255";

	@Description("Print the output one value per line")
	@In
	public boolean $$1FLAG = false;

	@Description("Only print the range of the data")
	@In
	public boolean $$rFLAG = false;

	@Description("Suppress reporting of any NULLs")
	@In
	public boolean $$nFLAG = false;

	@Description("Use the current region")
	@In
	public boolean $$dFLAG = false;

	@Description("Read fp map as integer")
	@In
	public boolean $$iFLAG = false;

	@Description("Run quietly")
	@In
	public boolean $$qFLAG = false;

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
