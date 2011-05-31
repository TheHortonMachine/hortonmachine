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

@Description("Erase the contents of the active display frame with user defined color")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass/Display Modules")
@Name("d__erase")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__erase {

	@Description("Color to erase with, either a standard GRASS color or R:G:B triplet (separated by colons) (optional)")
	@In
	public String $$colorPARAMETER = "white";

	@Description("Remove all frames and erase the screen")
	@In
	public boolean $$fFLAG = false;

	@Description("Don't add to list of commands in monitor")
	@In
	public boolean $$xFLAG = false;

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
