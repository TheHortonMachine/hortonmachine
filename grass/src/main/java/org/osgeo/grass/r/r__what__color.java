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

@Description("Queries colors for a raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__what__color")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__what__color {

	@UI("infile,grassfile")
	@Description("Name of existing raster map to query colors")
	@In
	public String $$inputPARAMETER;

	@Description("Values to query colors for (optional)")
	@In
	public String $$valuePARAMETER;

	@Description("Output format (printf-style) (optional)")
	@In
	public String $$formatPARAMETER = "%d:%d:%d";

	@Description("Read values from stdin")
	@In
	public boolean $$iFLAG = false;

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
