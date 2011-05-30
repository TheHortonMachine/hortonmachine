package org.osgeo.grass.v;

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

@Description("Calculates univariate statistics from a raster map based on vector polygons and uploads statistics to new attribute columns.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, raster, statistics")
@Label("Grass Vector Modules")
@Name("v__rast__stats")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__rast__stats {

	@UI("infile")
	@Description("Name of vector polygon map")
	@In
	public String $$vectorPARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@UI("infile")
	@Description("Name of raster map to calculate statistics from")
	@In
	public String $$rasterPARAMETER;

	@Description("Column prefix for new attribute columns")
	@In
	public String $$colprefixPARAMETER;

	@Description("Percentile to calculate (requires extended statistics flag) (optional)")
	@In
	public String $$percentilePARAMETER = "90";

	@Description("Continue if upload column(s) already exist")
	@In
	public boolean $$cFLAG = false;

	@Description("Calculate extended statistics")
	@In
	public boolean $$eFLAG = false;

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
