package org.osgeo.grass.g;

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

@Description("Lists available GRASS data base files of the user-specified data type to standard output.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, map management")
@Label("Grass/General Modules")
@Name("g__list")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__list {

	@Description("Data type")
	@In
	public String $$typePARAMETER;

	@Description("Mapset to list (default: current search path) (optional)")
	@In
	public String $$mapsetPARAMETER;

	@Description("Verbose listing (also list map titles)")
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
