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

@Description("Converts a raster map layer into a height-field file for POVRAY.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__out__pov")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__pov {

	@UI("infile,grassfile")
	@Description("Name of an existing raster map")
	@In
	public String $$mapPARAMETER;

	@Description("Name of output povray file (TGA height field file)")
	@In
	public String $$tgaPARAMETER;

	@Description("Height-field type (0=actual heights 1=normalized) (optional)")
	@In
	public String $$hftypePARAMETER;

	@Description("Elevation bias (optional)")
	@In
	public String $$biasPARAMETER;

	@Description("Vertical scaling factor (optional)")
	@In
	public String $$scalePARAMETER;

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
