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

@Description("Exports GRASS raster map to GRIDATB.FOR map file (TOPMODEL)")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__out__gridatb")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__gridatb {

	@UI("infile")
	@Description("Input map")
	@In
	public String $$inputPARAMETER;

	@Description("GRIDATB i/o map file")
	@In
	public String $$outputPARAMETER;

	@Description("Overwrite output map file")
	@In
	public boolean $$oFLAG = false;

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
