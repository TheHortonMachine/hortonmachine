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

@Description("Fast Fourier Transform (FFT) for image processing.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery, FFT")
@Label("Grass/Imagery Modules")
@Name("i__fft")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__fft {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$input_imagePARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output real part arrays stored as raster map")
	@In
	public String $$real_imagePARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output imaginary part arrays stored as raster map")
	@In
	public String $$imaginary_imagePARAMETER;

	@Description("Range of values in output display files (optional)")
	@In
	public String $$rangePARAMETER = "255";

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
