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

@Description("Centre node must be opened (costs >= 0). Costs of centre node are used in calculation")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, networking")
@Label("Grass/Vector Modules")
@Name("v__net__alloc")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__net__alloc {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Arc type (optional)")
	@In
	public String $$typePARAMETER = "line,boundary";

	@Description("Arc layer (optional)")
	@In
	public String $$alayerPARAMETER = "1";

	@Description("Node layer (optional)")
	@In
	public String $$nlayerPARAMETER = "2";

	@Description("Arc forward/both direction(s) cost column (number) (optional)")
	@In
	public String $$afcolumnPARAMETER;

	@Description("Arc backward direction cost column (number) (optional)")
	@In
	public String $$abcolumnPARAMETER;

	@Description("Node cost column (number) (optional)")
	@In
	public String $$ncolumnPARAMETER;

	@Description("Categories of centres (points on nodes) to which net will be allocated, layer for this categories is given by nlayer option")
	@In
	public String $$ccatsPARAMETER;

	@Description("Use geodesic calculation for longitude-latitude locations")
	@In
	public boolean $$gFLAG = false;

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
