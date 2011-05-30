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

@Description("Displays the color table associated with a raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, raster")
@Name("d__colortable")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__colortable {

	@UI("infile")
	@Description("Name of raster map whose color table is to be displayed")
	@In
	public String $$mapPARAMETER;

	@Description("Color of lines separating the colors of the color table (optional)")
	@In
	public String $$colorPARAMETER = "white";

	@Description("Number of lines to appear in the color table (optional)")
	@In
	public String $$linesPARAMETER;

	@Description("Number of columns to appear in the color table (optional)")
	@In
	public String $$colsPARAMETER;

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
