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

@Description("Samples a raster map at vector point locations.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector")
@Label("Grass Vector Modules")
@Name("v__sample")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__sample {

	@UI("infile,grassfile")
	@Description("Vector map defining sample points")
	@In
	public String $$inputPARAMETER;

	@Description("Vector map attribute column to use for comparison")
	@In
	public String $$columnPARAMETER;

	@UI("outfile,grassfile")
	@Description("Vector map to store differences")
	@In
	public String $$outputPARAMETER;

	@UI("infile,grassfile")
	@Description("Raster map to be sampled")
	@In
	public String $$rasterPARAMETER;

	@Description("Option scaling factor for values read from raster map. Sampled values will be multiplied by this factor (optional)")
	@In
	public String $$zPARAMETER = "1.0";

	@Description("Bilinear interpolation (default is nearest neighbor)")
	@In
	public boolean $$bFLAG = false;

	@Description("Cubic convolution interpolation (default is nearest neighbor)")
	@In
	public boolean $$cFLAG = false;

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

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
