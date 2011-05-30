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

@Description("Produces tilings of the source projection for use in the destination region and projection.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, tiling")
@Label("Grass Raster Modules")
@Name("r__tileset")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__tileset {

	@Description("Name of region to use instead of current region for bounds and resolution (optional)")
	@In
	public String $$regionPARAMETER;

	@Description("Source projection")
	@In
	public String $$sourceprojPARAMETER;

	@Description("Conversion factor from units to meters in source projection (optional)")
	@In
	public String $$sourcescalePARAMETER = "1";

	@Description("Destination projection, defaults to this location's projection (optional)")
	@In
	public String $$destprojPARAMETER;

	@Description("Conversion factor from units to meters in source projection (optional)")
	@In
	public String $$destscalePARAMETER;

	@Description("Maximum number of columns for a tile in the source projection (optional)")
	@In
	public String $$maxcolsPARAMETER = "1024";

	@Description("Maximum number of rows for a tile in the source projection (optional)")
	@In
	public String $$maxrowsPARAMETER = "1024";

	@Description("Number of cells tiles should overlap in each direction (optional)")
	@In
	public String $$overlapPARAMETER = "0";

	@Description("Output field separator (optional)")
	@In
	public String $$fsPARAMETER = "|";

	@Description("Verbosity level (optional)")
	@In
	public String $$vPARAMETER = "0";

	@Description("Produces shell script output")
	@In
	public boolean $$gFLAG = false;

	@Description("Produces web map server query string output")
	@In
	public boolean $$wFLAG = false;

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
