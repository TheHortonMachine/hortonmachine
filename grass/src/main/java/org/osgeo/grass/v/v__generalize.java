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

@Description("Vector based generalization.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, generalization, simplification, smoothing, displacement, network generalization")
@Label("Grass Vector Modules")
@Name("v__generalize")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__generalize {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "line,boundary,area";

	@Description("Generalization algorithm")
	@In
	public String $$methodPARAMETER = "douglas";

	@Description("Maximal tolerance value")
	@In
	public String $$thresholdPARAMETER = "1.0";

	@Description("Look-ahead parameter")
	@In
	public String $$look_aheadPARAMETER = "7";

	@Description("Percentage of the points in the output of 'douglas_reduction' algorithm")
	@In
	public String $$reductionPARAMETER = "50";

	@Description("Slide of computed point toward the original point")
	@In
	public String $$slidePARAMETER = "0.5";

	@Description("Minimum angle between two consecutive segments in Hermite method")
	@In
	public String $$angle_threshPARAMETER = "3";

	@Description("Degree threshold in network generalization")
	@In
	public String $$degree_threshPARAMETER = "0";

	@Description("Closeness threshold in network generalization")
	@In
	public String $$closeness_threshPARAMETER = "0";

	@Description("Betweeness threshold in network generalization")
	@In
	public String $$betweeness_threshPARAMETER = "0";

	@Description("Snakes alpha parameter")
	@In
	public String $$alphaPARAMETER = "1.0";

	@Description("Snakes beta parameter")
	@In
	public String $$betaPARAMETER = "1.0";

	@Description("Number of iterations")
	@In
	public String $$iterationsPARAMETER = "1";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Example: 1,3,7-9,13 (optional)")
	@In
	public String $$catsPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Copy attributes")
	@In
	public boolean $$cFLAG = false;

	@Description("Remove lines and areas smaller than threshold")
	@In
	public boolean $$rFLAG = false;

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
