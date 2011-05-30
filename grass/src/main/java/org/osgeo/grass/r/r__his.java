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

@Description("Generates red, green and blue raster map layers combining hue, intensity and saturation (HIS) values from user-specified input raster map layers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__his")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__his {

	@UI("infile")
	@Description("Name of layer to be used for HUE")
	@In
	public String $$h_mapPARAMETER;

	@UI("infile")
	@Description("Name of layer to be used for INTENSITY (optional)")
	@In
	public String $$i_mapPARAMETER;

	@UI("infile")
	@Description("Name of layer to be used for SATURATION (optional)")
	@In
	public String $$s_mapPARAMETER;

	@UI("outfile")
	@Description("Name of output layer to be used for RED")
	@In
	public String $$r_mapPARAMETER;

	@UI("outfile")
	@Description("Name of output layer to be used for GREEN")
	@In
	public String $$g_mapPARAMETER;

	@UI("outfile")
	@Description("Name of output layer to be used for BLUE")
	@In
	public String $$b_mapPARAMETER;

	@Description("Respect NULL values while drawing")
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
