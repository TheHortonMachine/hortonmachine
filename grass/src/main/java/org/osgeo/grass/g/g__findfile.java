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

@Description("Searches for GRASS data base files and sets variables for the shell.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general")
@Label("Grass/General Modules")
@Name("g__findfile")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__findfile {

	@Description("Name of an element")
	@In
	public String $$elementPARAMETER;

	@Description("Name of an existing map")
	@In
	public String $$filePARAMETER;

	@Description("Name of a mapset (optional)")
	@In
	public String $$mapsetPARAMETER = "";

	@Description("Don't add quotes")
	@In
	public boolean $$nFLAG = false;

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
