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

@Description("Finds the mode of values in a cover map within areas assigned the same category value in a user-specified base map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__mode")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__mode {

	@UI("infile,grassfile")
	@Description("Base map to be reclassified")
	@In
	public String $$basePARAMETER;

	@UI("infile,grassfile")
	@Description("Coverage map")
	@In
	public String $$coverPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output map")
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
