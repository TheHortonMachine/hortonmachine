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

@Description("Queries a vector map layer at given locations.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, querying")
@Label("Grass Vector Modules")
@Name("v__what")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__what {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("If not given reads from standard input (optional)")
	@In
	public String $$east_northPARAMETER;

	@Description("Query threshold distance (optional)")
	@In
	public String $$distancePARAMETER = "0";

	@Description("Print topological information (debugging)")
	@In
	public boolean $$dFLAG = false;

	@Description("Print attribute information")
	@In
	public boolean $$aFLAG = false;

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
