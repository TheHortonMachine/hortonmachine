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

@Description("Blends color components of two raster maps by a given ratio.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__blend")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__blend {

	@UI("infile,grassfile")
	@Description("Name of first raster map for blending")
	@In
	public String $$firstPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of second raster map for blending")
	@In
	public String $$secondPARAMETER;

	@Description("Base name for red, green and blue output maps containing the blend")
	@In
	public String $$outputPARAMETER;

	@Description("Percentage weight of first map for color blending (optional)")
	@In
	public String $$percentPARAMETER = "2";

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
