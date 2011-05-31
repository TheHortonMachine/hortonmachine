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

@Description("Display information about the active display monitor")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass/Display Modules")
@Name("d__info")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__info {

	@Description("Display screen rectangle (left, right, top, bottom)")
	@In
	public boolean $$rFLAG = false;

	@Description("Display screen dimensions (width, height)")
	@In
	public boolean $$dFLAG = false;

	@Description("Display active frame rectangle")
	@In
	public boolean $$fFLAG = false;

	@Description("Display screen rectangle of current region")
	@In
	public boolean $$bFLAG = false;

	@Description("Display geographic coordinates and resolution of entire screen")
	@In
	public boolean $$gFLAG = false;

	@Description("Display number of colors")
	@In
	public boolean $$cFLAG = false;

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
