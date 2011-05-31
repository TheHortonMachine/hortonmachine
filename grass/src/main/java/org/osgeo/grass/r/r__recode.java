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

@Description("Recodes categorical raster maps.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, recode category")
@Label("Grass/Raster Modules")
@Name("r__recode")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__recode {

	@UI("infile,grassfile")
	@Description("Raster map to be recoded")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Name of input file (optional)")
	@In
	public String $$rulesPARAMETER;

	@Description("Title for the resulting raster map (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("Align the current region to the input map")
	@In
	public boolean $$aFLAG = false;

	@Description("Force output to double map type (DCELL)")
	@In
	public boolean $$dFLAG = false;

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
