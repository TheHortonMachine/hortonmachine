package org.osgeo.grass.d;

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

@Description("Creates and displays a menu within the active frame on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass/Display Modules")
@Name("d__menu")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__menu {

	@Description("Sets the color of the menu background (optional)")
	@In
	public String $$bcolorPARAMETER = "white";

	@Description("Sets the color of the menu text (optional)")
	@In
	public String $$tcolorPARAMETER = "black";

	@Description("Sets the color dividing lines of text (optional)")
	@In
	public String $$dcolorPARAMETER = "black";

	@Description("Sets the menu text size (in percent) (optional)")
	@In
	public String $$sizePARAMETER = "3";

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
