package org.osgeo.grass.i;

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

@Description("Transforms raster maps from HIS (Hue-Intensity-Saturation) color space to RGB (Red-Green-Blue) color space.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery, color transformation, RGB, HIS")
@Label("Grass/Imagery Modules")
@Name("i__his__rgb")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__his__rgb {

	@UI("infile,grassfile")
	@Description("Name of input raster map (hue)")
	@In
	public String $$hue_inputPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input raster map (intensity)")
	@In
	public String $$intensity_inputPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input raster map (saturation)")
	@In
	public String $$saturation_inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map (red)")
	@In
	public String $$red_outputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map (green)")
	@In
	public String $$green_outputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map (blue)")
	@In
	public String $$blue_outputPARAMETER;

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
