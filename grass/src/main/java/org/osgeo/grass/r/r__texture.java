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

@Description("Generate images with textural features from a raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__texture")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__texture {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Prefix for output raster map(s)")
	@In
	public String $$prefixPARAMETER;

	@Description("The size of sliding window (odd and >= 3) (optional)")
	@In
	public String $$sizePARAMETER = "3";

	@Description("The distance between two samples (>= 1) (optional)")
	@In
	public String $$distancePARAMETER = "1";

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

	@Description("Angular Second Moment")
	@In
	public boolean $$aFLAG = false;

	@Description("Contrast")
	@In
	public boolean $$cFLAG = false;

	@Description("Correlation")
	@In
	public boolean $$kFLAG = false;

	@Description("Variance")
	@In
	public boolean $$vFLAG = false;

	@Description("Inverse Diff Moment")
	@In
	public boolean $$iFLAG = false;

	@Description("Sum Average")
	@In
	public boolean $$sFLAG = false;

	@Description("Sum Variance")
	@In
	public boolean $$wFLAG = false;

	@Description("Sum Entropy")
	@In
	public boolean $$xFLAG = false;

	@Description("Entropy")
	@In
	public boolean $$eFLAG = false;

	@Description("Difference Variance")
	@In
	public boolean $$dFLAG = false;

	@Description("Difference Entropy")
	@In
	public boolean $$pFLAG = false;

	@Description("Measure of Correlation-1")
	@In
	public boolean $$mFLAG = false;

	@Description("Measure of Correlation-2")
	@In
	public boolean $$nFLAG = false;

	@Description("Max Correlation Coeff")
	@In
	public boolean $$oFLAG = false;

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
