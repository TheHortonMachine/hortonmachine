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

@Description("Converts 3 GRASS raster layers (R,G,B) to a PPM image file at the pixel resolution of the CURRENTLY DEFINED REGION.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__out__ppm3")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__ppm3 {

	@UI("infile,grassfile")
	@Description("Name of raster map to be used for <red>")
	@In
	public String $$redPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map to be used for <green>")
	@In
	public String $$greenPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map to be used for <blue>")
	@In
	public String $$bluePARAMETER;

	@Description("Name for new PPM file. (use out=- for stdout)")
	@In
	public String $$outputPARAMETER;

	@Description("Run quietly")
	@In
	public boolean $$qFLAG = false;

	@Description("Add comments to describe the region")
	@In
	public boolean $$cFLAG = false;

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
