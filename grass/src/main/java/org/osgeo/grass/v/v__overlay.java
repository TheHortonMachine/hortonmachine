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

@Description("Overlays two vector maps.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, geometry")
@Label("Grass Vector Modules")
@Name("v__overlay")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__overlay {

	@UI("infile,grassfile")
	@Description("Name of input vector map (A)")
	@In
	public String $$ainputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$atypePARAMETER = "area";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$alayerPARAMETER = "1";

	@UI("infile,grassfile")
	@Description("Name of input vector map (B)")
	@In
	public String $$binputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$btypePARAMETER = "area";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$blayerPARAMETER = "1";

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Feature is written to output if the result of operation 'ainput operator binput' is true. Input feature is considered to be true, if category of given layer is defined. (optional)")
	@In
	public String $$operatorPARAMETER = "or";

	@Description("If 0 or not given, the category is not written (optional)")
	@In
	public String $$olayerPARAMETER = "1,0,0";

	@Description("Do not create attribute table")
	@In
	public boolean $$tFLAG = false;

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
