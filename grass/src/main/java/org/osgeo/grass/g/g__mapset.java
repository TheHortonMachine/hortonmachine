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

@Description("Change current mapset.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, settings")
@Label("Grass/General Modules")
@Name("g__mapset")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__mapset {

	@Description("New MAPSET name (optional)")
	@In
	public String $$mapsetPARAMETER;

	@Description("New LOCATION name (not location path) (optional)")
	@In
	public String $$locationPARAMETER;

	@Description("New GISDBASE (full path to the directory where the new location is) (optional)")
	@In
	public String $$gisdbasePARAMETER;

	@Description("Create mapset if it doesn't exist")
	@In
	public boolean $$cFLAG = false;

	@Description("List available mapsets")
	@In
	public boolean $$lFLAG = false;

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
