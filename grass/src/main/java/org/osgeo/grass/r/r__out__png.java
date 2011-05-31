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

@Description("Export GRASS raster as non-georeferenced PNG image format.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__out__png")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__png {

	@UI("infile,grassfile")
	@Description("Raster file to be converted.")
	@In
	public String $$inputPARAMETER;

	@Description("Name for new PNG file. (use out=- for stdout) (optional)")
	@In
	public String $$outputPARAMETER = "<rasterfilename>.png";

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
