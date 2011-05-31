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

@Description("Modifies the user's current mapset search path, affecting the user's access to data existing under the other GRASS mapsets in the current location.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, settings")
@Label("Grass/General Modules")
@Name("g__mapsets")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__mapsets {

	@Description("Name(s) of existing mapset(s) (optional)")
	@In
	public String $$mapsetPARAMETER;

	@Description("Name(s) of existing mapset(s) to add to search list (optional)")
	@In
	public String $$addmapsetPARAMETER;

	@Description("Name(s) of existing mapset(s) to remove from search list (optional)")
	@In
	public String $$removemapsetPARAMETER;

	@Description("Field separator (optional)")
	@In
	public String $$fsPARAMETER = "";

	@Description("List all available mapsets")
	@In
	public boolean $$lFLAG = false;

	@Description("Print current mapset search path")
	@In
	public boolean $$pFLAG = false;

	@Description("Show mapset selection dialog")
	@In
	public boolean $$sFLAG = false;

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
