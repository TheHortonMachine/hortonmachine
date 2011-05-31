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

@Description("Random location perturbations of GRASS vector points")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector")
@Label("Grass/Vector Modules")
@Name("v__perturb")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__perturb {

	@UI("infile,grassfile")
	@Description("Vector points to be spatially perturbed")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Distribution of perturbation (optional)")
	@In
	public String $$distributionPARAMETER = "uniform";

	@Description("Parameter(s) of distribution. If the distribution is uniform, only one parameter, the maximum, is needed. For a normal distribution, two parameters, the mean and standard deviation, are required.")
	@In
	public String $$parametersPARAMETER;

	@Description("Minimum deviation in map units (optional)")
	@In
	public String $$minimumPARAMETER = "0.0";

	@Description("Seed for random number generation (optional)")
	@In
	public String $$seedPARAMETER = "0";

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

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
