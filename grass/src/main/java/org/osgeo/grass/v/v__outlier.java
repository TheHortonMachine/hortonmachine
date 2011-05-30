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

@Description("Removes outliers from vector point data.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, statistics")
@Label("Grass Vector Modules")
@Name("v__outlier")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__outlier {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@UI("outfile")
	@Description("Name of output outlier vector map")
	@In
	public String $$outlierPARAMETER;

	@UI("outfile")
	@Description("Name of vector map for visualization in QGIS (optional)")
	@In
	public String $$qgisPARAMETER;

	@Description("Interpolation spline step value in east direction (optional)")
	@In
	public String $$soePARAMETER = "10";

	@Description("Interpolation spline step value in north direction (optional)")
	@In
	public String $$sonPARAMETER = "10";

	@Description("Tykhonov regularization weight (optional)")
	@In
	public String $$lambda_iPARAMETER = "0.1";

	@Description("Threshold for the outliers (optional)")
	@In
	public String $$thres_oPARAMETER = "50";

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
