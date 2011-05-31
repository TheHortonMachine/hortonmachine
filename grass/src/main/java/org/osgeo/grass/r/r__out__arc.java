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

@Description("Converts a raster map layer into an ESRI ARCGRID file.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__out__arc")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__arc {

	@UI("infile,grassfile")
	@Description("Name of an existing raster map layer")
	@In
	public String $$inputPARAMETER;

	@Description("Name of an output ARC-GID map (use out=- for stdout)")
	@In
	public String $$outputPARAMETER;

	@Description("Number of decimal places (optional)")
	@In
	public String $$dpPARAMETER = "8";

	@Description("Suppress printing of header information")
	@In
	public boolean $$hFLAG = false;

	@Description("List one entry per line instead of full row")
	@In
	public boolean $$1FLAG = false;

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
