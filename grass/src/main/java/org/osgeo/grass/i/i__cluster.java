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

@Description("The resulting signature file is used as input for i.maxlik, to generate an unsupervised image classification.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery, classification, signatures")
@Label("Grass/Imagery Modules")
@Name("i__cluster")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__cluster {

	@UI("infile,grassfile")
	@Description("Name of input imagery group")
	@In
	public String $$groupPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input imagery subgroup")
	@In
	public String $$subgroupPARAMETER;

	@Description("Name for output file containing result signatures")
	@In
	public String $$sigfilePARAMETER;

	@Description("Initial number of classes")
	@In
	public String $$classesPARAMETER;

	@Description("Name of file containing initial signatures (optional)")
	@In
	public String $$seedPARAMETER;

	@Description("Sampling intervals (by row and col); default: ~10,000 pixels (optional)")
	@In
	public String $$samplePARAMETER;

	@Description("Maximum number of iterations (optional)")
	@In
	public String $$iterationsPARAMETER = "30";

	@Description("Percent convergence (optional)")
	@In
	public String $$convergencePARAMETER = "98.0";

	@Description("Cluster separation (optional)")
	@In
	public String $$separationPARAMETER = "0.0";

	@Description("Minimum number of pixels in a class (optional)")
	@In
	public String $$min_sizePARAMETER = "17";

	@Description("Name for output file containing final report (optional)")
	@In
	public String $$reportfilePARAMETER;

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

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
