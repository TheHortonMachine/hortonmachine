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

@Description("Calculates contrast weighted edge density index on a raster map")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, landscape structure analysis, patch index")
@Label("Grass Raster Modules")
@Name("r__li__cwed")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__li__cwed {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$mapPARAMETER;

	@Description("Configuration file")
	@In
	public String $$confPARAMETER;

	@Description("input file that contains the weight to calculate the index")
	@In
	public String $$pathPARAMETER;

	@UI("outfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

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
