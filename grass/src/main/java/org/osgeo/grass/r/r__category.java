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

@Description("Manages category values and labels associated with user-specified raster map layers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__category")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__category {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$mapPARAMETER;

	@Description("Example: 1,3,7-9,13 (optional)")
	@In
	public String $$catsPARAMETER;

	@Description("Example: 1.4,3.8,13 (optional)")
	@In
	public String $$valsPARAMETER;

	@Description("Output field separator (optional)")
	@In
	public String $$fsPARAMETER = "tab";

	@UI("infile,grassfile")
	@Description("Raster map from which to copy category table (optional)")
	@In
	public String $$rasterPARAMETER;

	@Description("File containing category label rules (or \"-\" to read from stdin) (optional)")
	@In
	public String $$rulesPARAMETER;

	@Description("Used when no explicit label exists for the category (optional)")
	@In
	public String $$formatPARAMETER;

	@Description("Two pairs of category multiplier and offsets, for $1 and $2 (optional)")
	@In
	public String $$coefficientsPARAMETER;

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
