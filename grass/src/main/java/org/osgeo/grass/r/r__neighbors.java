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

@Description("Makes each cell category value a function of the category values assigned to the cells around it, and stores new cell values in an output raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__neighbors")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__neighbors {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Neighborhood operation (optional)")
	@In
	public String $$methodPARAMETER = "average";

	@Description("Neighborhood size (optional)")
	@In
	public String $$sizePARAMETER = "3";

	@Description("Title of the output raster map (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("File containing weights (optional)")
	@In
	public String $$weightPARAMETER;

	@Description("Do not align output with the input")
	@In
	public boolean $$aFLAG = false;

	@Description("Run quietly")
	@In
	public boolean $$qFLAG = false;

	@Description("Use circular neighborhood")
	@In
	public boolean $$cFLAG = false;

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
