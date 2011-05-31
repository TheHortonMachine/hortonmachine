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

@Description("Exports a GRASS raster map to a 8/24bit TIFF image file at the pixel resolution of the currently defined region.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, export")
@Label("Grass Raster Modules")
@Name("r__out__tiff")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__tiff {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@Description("Name for new TIFF file")
	@In
	public String $$outputPARAMETER;

	@Description("TIFF file compression (optional)")
	@In
	public String $$compressionPARAMETER = "none";

	@Description("TIFF Palette output (8bit instead of 24bit).")
	@In
	public boolean $$pFLAG = false;

	@Description("Output TIFF world file")
	@In
	public boolean $$tFLAG = false;

	@Description("Output Tiled TIFF")
	@In
	public boolean $$lFLAG = false;

	@Description("Verbose mode")
	@In
	public boolean $$vFLAG = false;

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
