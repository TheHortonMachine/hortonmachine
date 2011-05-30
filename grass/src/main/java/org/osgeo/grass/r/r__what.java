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

@Description("Queries raster map layers on their category values and category labels.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__what")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__what {

	@UI("infile")
	@Description("Name of existing raster map(s) to query")
	@In
	public String $$inputPARAMETER;

	@Description("Size of point cache (optional)")
	@In
	public String $$cachePARAMETER = "500";

	@Description("Char string to represent no data cell (optional)")
	@In
	public String $$nullPARAMETER = "*";

	@Description("Field separator (optional)")
	@In
	public String $$fsPARAMETER = "|";

	@Description("Coordinates for query (optional)")
	@In
	public String $$east_northPARAMETER;

	@Description("Show the category label in the grid cell(s)")
	@In
	public boolean $$fFLAG = false;

	@Description("Turn on cache reporting")
	@In
	public boolean $$cFLAG = false;

	@Description("Output integer category values, not cell values")
	@In
	public boolean $$iFLAG = false;

	@Description("Output color values as RRR:GGG:BBB")
	@In
	public boolean $$rFLAG = false;

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
