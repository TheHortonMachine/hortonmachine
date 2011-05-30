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

@Description("Creates a composite raster map layer by using known category values from one (or more) map layer(s) to fill in areas of \"no data\" in another map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__patch")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__patch {

	@UI("infile")
	@Description("Name of raster maps to be patched together")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for resultant raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

	@Description("Use zero (0) for transparency instead of NULL")
	@In
	public boolean $$zFLAG = false;

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
