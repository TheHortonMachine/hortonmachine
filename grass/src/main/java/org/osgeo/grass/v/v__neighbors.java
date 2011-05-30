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

@Description("Makes each cell value a function of the attribute values assigned to the vector points or centroids around it, and stores new cell values in an output raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, raster, aggregation")
@Label("Grass Vector Modules")
@Name("v__neighbors")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__neighbors {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Neighborhood operation")
	@In
	public String $$methodPARAMETER = "count";

	@Description("Neighborhood diameter in map units")
	@In
	public String $$sizePARAMETER;

	@Description("Allow output files to overwrite existing files")
	@In
	public boolean $$overwriteFLAG = false;

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
