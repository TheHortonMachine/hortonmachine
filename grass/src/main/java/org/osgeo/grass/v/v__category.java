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

@Description("Attach, delete or report vector categories to map geometry.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, category")
@Label("Grass Vector Modules")
@Name("v__category")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__category {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Action to be done (optional)")
	@In
	public String $$optionPARAMETER = "add";

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid,area";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Example: 1,3,7-9,13 (optional)")
	@In
	public String $$idsPARAMETER;

	@Description("Category value (optional)")
	@In
	public String $$catPARAMETER = "1";

	@Description("Category increment (optional)")
	@In
	public String $$stepPARAMETER = "1";

	@Description("Format: layer type count min max")
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
