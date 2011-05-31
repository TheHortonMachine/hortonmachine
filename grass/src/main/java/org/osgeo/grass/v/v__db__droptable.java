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

@Description("Removes existing attribute table of a vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, database, attribute table")
@Label("Grass Vector Modules")
@Name("v__db__droptable")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__db__droptable {

	@UI("infile,grassfile")
	@Description("Vector map from which to remove attribute table")
	@In
	public String $$mapPARAMETER;

	@Description("Name of existing attribute table (default: vector map name) (optional)")
	@In
	public String $$tablePARAMETER;

	@Description("Layer from which to drop linked attribute table (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Force removal (required for actual deletion of table)")
	@In
	public boolean $$fFLAG = false;

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
