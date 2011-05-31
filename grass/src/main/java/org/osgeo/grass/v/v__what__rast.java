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

@Description("Uploads raster values at positions of vector points to the table.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, raster, attribute table")
@Label("Grass Vector Modules")
@Name("v__what__rast")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__what__rast {

	@UI("infile,grassfile")
	@Description("Name of input vector points map for which to edit attribute table")
	@In
	public String $$vectorPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of existing raster map to be queried")
	@In
	public String $$rasterPARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Column name (will be updated by raster values)")
	@In
	public String $$columnPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

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
