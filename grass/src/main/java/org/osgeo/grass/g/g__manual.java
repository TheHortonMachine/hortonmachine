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

@Description("Display the HTML man pages of GRASS")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, manual, help")
@Label("Grass/General Modules")
@Name("g__manual")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__manual {

	@Description("Manual entry to be displayed (optional)")
	@In
	public String $$entryPARAMETER;

	@Description("Display index")
	@In
	public boolean $$iFLAG = false;

	@Description("Display as MAN text page instead of HTML page in browser")
	@In
	public boolean $$mFLAG = false;

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
