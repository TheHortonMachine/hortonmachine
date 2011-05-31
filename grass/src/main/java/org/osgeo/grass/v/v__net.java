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

@Description("Performs network maintenance.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, networking")
@Label("Grass/Vector Modules")
@Name("v__net")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__net {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("infile,grassfile")
	@Description("Required for operation 'connect' (optional)")
	@In
	public String $$pointsPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Operation to be performed (optional)")
	@In
	public String $$operationPARAMETER = "nodes";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$alayerPARAMETER = "1";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$nlayerPARAMETER = "2";

	@Description("Required for operation 'connect'. Connect points in given threshold. (optional)")
	@In
	public String $$threshPARAMETER;

	@Description("For operation 'nodes'")
	@In
	public boolean $$cFLAG = false;

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
