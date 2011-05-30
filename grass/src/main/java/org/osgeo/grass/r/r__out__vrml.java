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

@Description("Export a raster map to the Virtual Reality Modeling Language (VRML)")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, export, VRML")
@Label("Grass Raster Modules")
@Name("r__out__vrml")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__vrml {

	@UI("infile")
	@Description("Name of elevation map")
	@In
	public String $$elevPARAMETER;

	@UI("infile")
	@Description("Name of color file (optional)")
	@In
	public String $$colorPARAMETER;

	@Description("Vertical exaggeration (optional)")
	@In
	public String $$exagPARAMETER = "1.0";

	@Description("Name for new VRML file")
	@In
	public String $$outputPARAMETER;

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
