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

@Description("Toolset for cleaning topology of vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, topology")
@Label("Grass Vector Modules")
@Name("v__clean")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__clean {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid,area";

	@UI("outfile,grassfile")
	@Description("Name of output map where errors are written (optional)")
	@In
	public String $$errorPARAMETER;

	@Description("Cleaning tool")
	@In
	public String $$toolPARAMETER;

	@Description("Threshold in map units, one value for each tool (default: 0.0[,0.0,...]) (optional)")
	@In
	public String $$threshPARAMETER;

	@Description("Don't build topology for the output vector")
	@In
	public boolean $$bFLAG = false;

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
