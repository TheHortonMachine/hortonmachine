package org.osgeo.grass.d;

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

@Description("Interactively edit cell values in a raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, raster")
@Name("d__rast__edit")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__rast__edit {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of aspect raster map (optional)")
	@In
	public String $$aspectPARAMETER;

	@Description("Width of display canvas (optional)")
	@In
	public String $$widthPARAMETER = "640";

	@Description("Height of display canvas (optional)")
	@In
	public String $$heightPARAMETER = "480";

	@Description("Minimum size of each cell (optional)")
	@In
	public String $$sizePARAMETER = "12";

	@Description("Maximum number of rows to load (optional)")
	@In
	public String $$rowsPARAMETER = "100";

	@Description("Maximum number of columns to load (optional)")
	@In
	public String $$colsPARAMETER = "100";

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
