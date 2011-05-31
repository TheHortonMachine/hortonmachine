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

@Description("Interpolates point data to a G3D grid volume using regularized spline with tension (RST) algorithm.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector")
@Label("Grass Vector Modules")
@Name("v__vol__rst")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__vol__rst {

	@UI("infile,grassfile")
	@Description("Name of the vector map with input x,y,z,w")
	@In
	public String $$inputPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the surface raster map for cross-section (optional)")
	@In
	public String $$cellinpPARAMETER;

	@Description("Name of the column containing w attribute to interpolate (optional)")
	@In
	public String $$wcolumnPARAMETER = "flt1";

	@Description("Tension parameter (optional)")
	@In
	public String $$tensionPARAMETER = "40.";

	@Description("Smoothing parameter (optional)")
	@In
	public String $$smoothPARAMETER = "0.1";

	@Description("Name of the column with smoothing parameters (optional)")
	@In
	public String $$scolumnPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@UI("outfile,grassfile")
	@Description("Output deviations vector point file (optional)")
	@In
	public String $$deviPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output cross-validation vector map (optional)")
	@In
	public String $$cvdevPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the raster map used as mask (optional)")
	@In
	public String $$maskmapPARAMETER;

	@Description("Maximum number of points in a segment (optional)")
	@In
	public String $$segmaxPARAMETER = "50";

	@Description("Minimum number of points for approximation in a segment (>segmax) (optional)")
	@In
	public String $$npminPARAMETER = "200";

	@Description("Minimum distance between points (to remove almost identical points) (optional)")
	@In
	public String $$dminPARAMETER = "0.500000";

	@Description("Conversion factor for w-values used for interpolation (optional)")
	@In
	public String $$wmultPARAMETER = "1.0";

	@Description("Conversion factor for z-values (optional)")
	@In
	public String $$zmultPARAMETER = "1.0";

	@UI("outfile,grassfile")
	@Description("Output cross-section raster map (optional)")
	@In
	public String $$celloutPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output elevation g3d-file (optional)")
	@In
	public String $$elevPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output gradient magnitude g3d-file (optional)")
	@In
	public String $$gradientPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output gradient horizontal angle g3d-file (optional)")
	@In
	public String $$aspect1PARAMETER;

	@UI("outfile,grassfile")
	@Description("Output gradient vertical angle g3d-file (optional)")
	@In
	public String $$aspect2PARAMETER;

	@UI("outfile,grassfile")
	@Description("Output change of gradient g3d-file (optional)")
	@In
	public String $$ncurvPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output gaussian curvature g3d-file (optional)")
	@In
	public String $$gcurvPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output mean curvature g3d-file (optional)")
	@In
	public String $$mcurvPARAMETER;

	@Description("Perform a cross-validation procedure without volume interpolation")
	@In
	public boolean $$cFLAG = false;

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
