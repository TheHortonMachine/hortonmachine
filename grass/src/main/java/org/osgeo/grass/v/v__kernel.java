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

@Description("Generates a raster density map from vector points data using a moving 2D isotropic Gaussian kernel or optionally generates a vector density map on vector network with a 1D kernel.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, kernel density")
@Label("Grass/Vector Modules")
@Name("v__kernel")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__kernel {

	@UI("infile,grassfile")
	@Description("Input vector with training points")
	@In
	public String $$inputPARAMETER;

	@UI("infile,grassfile")
	@Description("Input network vector map (optional)")
	@In
	public String $$netPARAMETER;

	@Description("Output raster/vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Standard deviation in map units")
	@In
	public String $$stddeviationPARAMETER;

	@Description("Discretization error in map units (optional)")
	@In
	public String $$dsizePARAMETER = "0.";

	@Description("Maximum length of segment on network (optional)")
	@In
	public String $$segmaxPARAMETER = "100.";

	@Description("Maximum distance from point to network (optional)")
	@In
	public String $$distmaxPARAMETER = "100.";

	@Description("Multiply the density result by this number (optional)")
	@In
	public String $$multPARAMETER = "1.";

	@Description("Try to calculate an optimal standard deviation with 'stddeviation' taken as maximum (experimental)")
	@In
	public boolean $$oFLAG = false;

	@Description("Only calculate optimal standard deviation and exit (no map is written)")
	@In
	public boolean $$qFLAG = false;

	@Description("Run verbosely")
	@In
	public boolean $$vFLAG = false;

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
