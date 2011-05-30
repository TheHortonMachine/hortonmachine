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

@Description("Reinterpolates and optionally computes topographic analysis from input raster map to a new raster map (possibly with different resolution) using regularized spline with tension and smoothing.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__resamp__rst")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__resamp__rst {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@Description("Desired east-west resolution")
	@In
	public String $$ew_resPARAMETER;

	@Description("Desired north-south resolution")
	@In
	public String $$ns_resPARAMETER;

	@UI("outfile")
	@Description("Output z-file (elevation) map (optional)")
	@In
	public String $$elevPARAMETER;

	@UI("outfile")
	@Description("Output slope map (or fx) (optional)")
	@In
	public String $$slopePARAMETER;

	@UI("outfile")
	@Description("Output aspect map (or fy) (optional)")
	@In
	public String $$aspectPARAMETER;

	@UI("outfile")
	@Description("Output profile curvature map (or fxx) (optional)")
	@In
	public String $$pcurvPARAMETER;

	@UI("outfile")
	@Description("Output tangential curvature map (or fyy) (optional)")
	@In
	public String $$tcurvPARAMETER;

	@UI("outfile")
	@Description("Output mean curvature map (or fxy) (optional)")
	@In
	public String $$mcurvPARAMETER;

	@UI("infile")
	@Description("Name of raster map containing smoothing (optional)")
	@In
	public String $$smoothPARAMETER;

	@UI("infile")
	@Description("Name of raster map to be used as mask (optional)")
	@In
	public String $$maskmapPARAMETER;

	@Description("Rows/columns overlap for segmentation (optional)")
	@In
	public String $$overlapPARAMETER = "3";

	@Description("Multiplier for z-values (optional)")
	@In
	public String $$zmultPARAMETER = "1.0";

	@Description("Spline tension value (optional)")
	@In
	public String $$tensionPARAMETER = "40.";

	@Description("Anisotropy angle (in degrees) (optional)")
	@In
	public String $$thetaPARAMETER;

	@Description("Anisotropy scaling factor (optional)")
	@In
	public String $$scalexPARAMETER;

	@Description("Use dnorm independent tension")
	@In
	public boolean $$tFLAG = false;

	@Description("Output partial derivatives instead of topographic parameters")
	@In
	public boolean $$dFLAG = false;

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
