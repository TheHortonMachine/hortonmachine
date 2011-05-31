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

@Description("Set color rules for features in a vector using a numeric attribute column.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, color table")
@Label("Grass Vector Modules")
@Name("v__colors")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__colors {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Name of column containing numeric data")
	@In
	public String $$columnPARAMETER;

	@Description("Layer number of data column (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Name of color column to populate (optional)")
	@In
	public String $$rgb_columnPARAMETER = "GRASSRGB";

	@Description("Manually set range (min,max) (optional)")
	@In
	public String $$rangePARAMETER;

	@Description("Type of color table (optional)")
	@In
	public String $$colorPARAMETER;

	@UI("infile,grassfile")
	@Description("Raster map name from which to copy color table (optional)")
	@In
	public String $$rasterPARAMETER;

	@Description("Path to rules file (optional)")
	@In
	public String $$rulesPARAMETER;

	@Description("Save placeholder raster map for use with d.legend")
	@In
	public boolean $$sFLAG = false;

	@Description("Invert colors")
	@In
	public boolean $$nFLAG = false;

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
