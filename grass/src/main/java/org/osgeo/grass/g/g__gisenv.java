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

@Description("Outputs and modifies the user's current GRASS variable settings.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general")
@Label("Grass/General Modules")
@Name("g__gisenv")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__gisenv {

	@Description("GRASS variable to get (optional)")
	@In
	public String $$getPARAMETER;

	@Description("GRASS variable to set (optional)")
	@In
	public String $$setPARAMETER;

	@Description("Where GRASS variable is stored (optional)")
	@In
	public String $$storePARAMETER = "gisrc";

	@Description("Use shell syntax (for \"eval\")")
	@In
	public boolean $$sFLAG = false;

	@Description("Don't use shell syntax")
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
