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

@Description("Selects vector objects from an existing vector map and creates a new map containing only the selected objects.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, extract")
@Label("Grass Vector Modules")
@Name("v__extract")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__extract {

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
	public String $$typePARAMETER = "point,line,boundary,centroid,area,face";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Example: 1,3,7-9,13 (optional)")
	@In
	public String $$listPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("If '-' given reads from standard input (optional)")
	@In
	public String $$filePARAMETER;

	@Description("Number must be smaller than unique cat count in layer (optional)")
	@In
	public String $$randomPARAMETER;

	@Description("If new >= 0, table is not copied (optional)")
	@In
	public String $$newPARAMETER = "-1";

	@Description("Dissolve common boundaries (default is no)")
	@In
	public boolean $$dFLAG = false;

	@Description("Do not copy table (see also 'new' parameter)")
	@In
	public boolean $$tFLAG = false;

	@Description("Reverse selection")
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
