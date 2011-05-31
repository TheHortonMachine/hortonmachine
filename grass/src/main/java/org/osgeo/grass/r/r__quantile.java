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

@Description("Compute quantiles using two passes.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, statistics")
@Label("Grass/Raster Modules")
@Name("r__quantile")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__quantile {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@Description("Number of quantiles (optional)")
	@In
	public String $$quantilesPARAMETER = "4";

	@Description("List of percentiles (optional)")
	@In
	public String $$percentilesPARAMETER;

	@Description("Number of bins to use (optional)")
	@In
	public String $$binsPARAMETER = "1000000";

	@Description("Generate recode rules based on quantile-defined intervals.")
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
