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

@Description("Detects the object's edges from a LIDAR data set.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, LIDAR, edges")
@Label("Grass/Vector Modules")
@Name("v__lidar__edgedetection")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__lidar__edgedetection {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Interpolation spline step value in east direction (optional)")
	@In
	public String $$seePARAMETER = "4";

	@Description("Interpolation spline step value in north direction (optional)")
	@In
	public String $$senPARAMETER = "4";

	@Description("Regularization weight in gradient evaluation (optional)")
	@In
	public String $$lambda_gPARAMETER = "0.01";

	@Description("High gradient threshold for edge classification (optional)")
	@In
	public String $$tghPARAMETER = "6";

	@Description("Low gradient threshold for edge classification (optional)")
	@In
	public String $$tglPARAMETER = "3";

	@Description("Angle range for same direction detection (optional)")
	@In
	public String $$theta_gPARAMETER = "0.26";

	@Description("Regularization weight in residual evaluation (optional)")
	@In
	public String $$lambda_rPARAMETER = "2";

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
