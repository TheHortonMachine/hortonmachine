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

@Description("Controls access to the current mapset for other users on the system.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general")
@Label("Grass/General Modules")
@Name("g__access")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__access {

	@Description("Access for group (optional)")
	@In
	public String $$groupPARAMETER;

	@Description("Access for others (optional)")
	@In
	public String $$otherPARAMETER;

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
