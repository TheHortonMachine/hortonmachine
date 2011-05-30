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

@Description("Note that TSP is NP-hard, heuristic algorithm is used by this module and created cycle may be sub optimal")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, networking")
@Label("Grass Vector Modules")
@Name("v__net__salesman")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__net__salesman {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Arc type (optional)")
	@In
	public String $$typePARAMETER = "line,boundary";

	@Description("Arc layer (optional)")
	@In
	public String $$alayerPARAMETER = "1";

	@Description("Node layer (used for cities) (optional)")
	@In
	public String $$nlayerPARAMETER = "2";

	@Description("Arcs' cost column (for both directions) (optional)")
	@In
	public String $$acolumnPARAMETER;

	@Description("Categories of points ('cities') on nodes (layer is specified by nlayer)")
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
