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

@Description("Correction of the v.lidar.growing output. It is the last of the three algorithms for LIDAR filtering.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, LIDAR")
@Label("Grass Vector Modules")
@Name("v__lidar__correction")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__lidar__correction {

	@UI("infile")
	@Description("Input observation vector map name (v.lidar.growing output)")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Output classified vector map name")
	@In
	public String $$outputPARAMETER;

	@UI("outfile")
	@Description("Only 'terrain' points output vector map")
	@In
	public String $$terrainPARAMETER;

	@Description("Interpolation spline step value in east direction (optional)")
	@In
	public String $$scePARAMETER = "25";

	@Description("Interpolation spline step value in north direction (optional)")
	@In
	public String $$scnPARAMETER = "25";

	@Description("Regularization weight in reclassification evaluation (optional)")
	@In
	public String $$lambda_cPARAMETER = "1";

	@Description("High threshold for object to terrain reclassification (optional)")
	@In
	public String $$tchPARAMETER = "2";

	@Description("Low threshold for terrain to object reclassification (optional)")
	@In
	public String $$tclPARAMETER = "1";

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
