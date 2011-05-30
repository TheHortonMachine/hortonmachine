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

@Description("Creates/modifies the color table associated with a raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, color table")
@Label("Grass Raster Modules")
@Name("r__colors")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__colors {

	@UI("infile")
	@Description("Name of input raster map (optional)")
	@In
	public String $$mapPARAMETER;

	@Description("Type of color table (optional)")
	@In
	public String $$colorPARAMETER;

	@UI("infile")
	@Description("Raster map name from which to copy color table (optional)")
	@In
	public String $$rasterPARAMETER;

	@Description("Path to rules file (\"-\" to read rules from stdin) (optional)")
	@In
	public String $$rulesPARAMETER;

	@Description("Remove existing color table")
	@In
	public boolean $$rFLAG = false;

	@Description("Only write new color table if one doesn't already exist")
	@In
	public boolean $$wFLAG = false;

	@Description("List available rules then exit")
	@In
	public boolean $$lFLAG = false;

	@Description("Invert colors")
	@In
	public boolean $$nFLAG = false;

	@Description("Logarithmic scaling")
	@In
	public boolean $$gFLAG = false;

	@Description("Logarithmic-absolute scaling")
	@In
	public boolean $$aFLAG = false;

	@Description("Histogram equalization")
	@In
	public boolean $$eFLAG = false;

	@Description("Enter rules interactively")
	@In
	public boolean $$iFLAG = false;

	@Description("Run quietly")
	@In
	public boolean $$qFLAG = false;

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
