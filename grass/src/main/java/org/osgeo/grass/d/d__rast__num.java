package org.osgeo.grass.d;

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

@Description("Overlays cell category values on a raster map layer displayed to the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass/Display Modules")
@Name("d__rast__num")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__rast__num {

	@UI("infile,grassfile")
	@Description("Name of input raster map (optional)")
	@In
	public String $$mapPARAMETER;

	@Description("Color for drawing grid, or \"none\" (optional)")
	@In
	public String $$grid_colorPARAMETER = "gray";

	@Description("Color for drawing text (optional)")
	@In
	public String $$text_colorPARAMETER = "black";

	@Description("Number of significant digits (floating point only) (optional)")
	@In
	public String $$dpPARAMETER = "1";

	@Description("Get text color from cell color value")
	@In
	public boolean $$fFLAG = false;

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
