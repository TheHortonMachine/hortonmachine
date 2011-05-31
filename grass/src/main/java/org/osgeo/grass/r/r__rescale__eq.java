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

@Description("Rescales histogram equalized the range of category values in a raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__rescale__eq")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__rescale__eq {

	@UI("infile,grassfile")
	@Description("The name of the raster map to be rescaled")
	@In
	public String $$inputPARAMETER;

	@Description("The input data range to be rescaled (default: full range of input map) (optional)")
	@In
	public String $$fromPARAMETER;

	@UI("outfile,grassfile")
	@Description("The resulting raster map name")
	@In
	public String $$outputPARAMETER;

	@Description("The output data range")
	@In
	public String $$toPARAMETER;

	@Description("Title for new raster map (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

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
