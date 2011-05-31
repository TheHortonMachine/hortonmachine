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

@Description("Spatial approximation and topographic analysis from given point or isoline data in vector format to floating point raster format using regularized spline with tension.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector")
@Label("Grass Vector Modules")
@Name("v__surf__rst")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__surf__rst {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@Description("If set to 0, z coordinates are used. (3D vector only) (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@UI("outfile,grassfile")
	@Description("Output surface raster map (elevation) (optional)")
	@In
	public String $$elevPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output slope raster map (optional)")
	@In
	public String $$slopePARAMETER;

	@UI("outfile,grassfile")
	@Description("Output aspect raster map (optional)")
	@In
	public String $$aspectPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output profile curvature raster map (optional)")
	@In
	public String $$pcurvPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output tangential curvature raster map (optional)")
	@In
	public String $$tcurvPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output mean curvature raster map (optional)")
	@In
	public String $$mcurvPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output deviations vector point file (optional)")
	@In
	public String $$deviPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output cross-validation errors vector point file (optional)")
	@In
	public String $$cvdevPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output vector map showing quadtree segmentation (optional)")
	@In
	public String $$treefilePARAMETER;

	@UI("outfile,grassfile")
	@Description("Output vector map showing overlapping windows (optional)")
	@In
	public String $$overfilePARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the raster map used as mask (optional)")
	@In
	public String $$maskmapPARAMETER;

	@Description("Name of the attribute column with values to be used for approximation (if layer>0) (optional)")
	@In
	public String $$zcolumnPARAMETER;

	@Description("Tension parameter (optional)")
	@In
	public String $$tensionPARAMETER = "40.";

	@Description("Smoothing parameter (optional)")
	@In
	public String $$smoothPARAMETER;

	@Description("Name of the attribute column with smoothing parameters (optional)")
	@In
	public String $$scolumnPARAMETER;

	@Description("Maximum number of points in a segment (optional)")
	@In
	public String $$segmaxPARAMETER = "40";

	@Description("Minimum number of points for approximation in a segment (>segmax) (optional)")
	@In
	public String $$npminPARAMETER = "300";

	@Description("Minimum distance between points (to remove almost identical points) (optional)")
	@In
	public String $$dminPARAMETER = "0.500000";

	@Description("Maximum distance between points on isoline (to insert additional points) (optional)")
	@In
	public String $$dmaxPARAMETER = "2.500000";

	@Description("Conversion factor for values used for approximation (optional)")
	@In
	public String $$zmultPARAMETER = "1.0";

	@Description("Anisotropy angle (in degrees counterclockwise from East) (optional)")
	@In
	public String $$thetaPARAMETER;

	@Description("Anisotropy scaling factor (optional)")
	@In
	public String $$scalexPARAMETER;

	@Description("Perform cross-validation procedure without raster approximation")
	@In
	public boolean $$cFLAG = false;

	@Description("Use scale dependent tension")
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
