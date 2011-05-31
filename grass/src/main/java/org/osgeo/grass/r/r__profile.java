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

@Description("Outputs the raster map layer values lying on user-defined line(s).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__profile")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__profile {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@Description("Name of file for output (use output=- for stdout) (optional)")
	@In
	public String $$outputPARAMETER = "-";

	@Description("Profile coordinate pairs (optional)")
	@In
	public String $$profilePARAMETER;

	@Description("Resolution along profile (default = current region resolution) (optional)")
	@In
	public String $$resPARAMETER;

	@Description("Character to represent no data cell (optional)")
	@In
	public String $$nullPARAMETER = "*";

	@Description("Interactively select End-Points")
	@In
	public boolean $$iFLAG = false;

	@Description("Output easting and northing in first two columns of four column output")
	@In
	public boolean $$gFLAG = false;

	@Description("Output RRR:GGG:BBB color values for each profile point")
	@In
	public boolean $$cFLAG = false;

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
