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

@Description("Makes each output cell value a function of the values assigned to the corresponding cells in the input raster map layers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, series")
@Label("Grass/Raster Modules")
@Name("r__series")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__series {

	@UI("infile,grassfile")
	@Description("Name of input raster map(s)")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Aggregate operation")
	@In
	public String $$methodPARAMETER;

	@Description("Ignore values outside this range (optional)")
	@In
	public String $$rangePARAMETER;

	@Description("Run quietly")
	@In
	public boolean $$qFLAG = false;

	@Description("Propagate NULLs")
	@In
	public boolean $$nFLAG = false;

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
