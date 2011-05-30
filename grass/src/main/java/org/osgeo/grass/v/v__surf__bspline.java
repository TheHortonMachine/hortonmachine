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

@Description("Bicubic or bilinear spline interpolation with Tykhonov regularization.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, interpolation")
@Label("Grass Vector Modules")
@Name("v__surf__bspline")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__surf__bspline {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("infile")
	@Description("Name of input vector map of sparse points (optional)")
	@In
	public String $$sparsePARAMETER;

	@UI("outfile")
	@Description("Name for output vector map (optional)")
	@In
	public String $$outputPARAMETER;

	@UI("outfile")
	@Description("Name for output raster map (optional)")
	@In
	public String $$rasterPARAMETER;

	@Description("Length of each spline step in the east-west direction (optional)")
	@In
	public String $$siePARAMETER = "4";

	@Description("Length of each spline step in the north-south direction (optional)")
	@In
	public String $$sinPARAMETER = "4";

	@Description("Spline interpolation algorithm (optional)")
	@In
	public String $$methodPARAMETER = "bilinear";

	@Description("Tykhonov regularization parameter (affects smoothing) (optional)")
	@In
	public String $$lambda_iPARAMETER = "1";

	@Description("If set to 0, z coordinates are used. (3D vector only) (optional)")
	@In
	public String $$layerPARAMETER = "0";

	@Description("Attribute table column with values to interpolate (if layer>0) (optional)")
	@In
	public String $$columnPARAMETER;

	@Description("Find the best Tykhonov regularizing parameter using a \"leave-one-out\" cross validation method")
	@In
	public boolean $$cFLAG = false;

	@Description("Estimate point density and distance for the input vector points within the current region extends and quit")
	@In
	public boolean $$eFLAG = false;

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
