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

@Description("Launches a GRASS graphical user interface (GUI) session.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, gui")
@Label("Grass/General Modules")
@Name("g__gui")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__gui {

	@Description("Default value: GRASS_GUI if defined otherwise tcltk (optional)")
	@In
	public String $$guiPARAMETER;

	@Description("Name of workspace file (optional)")
	@In
	public String $$workspacePARAMETER;

	@Description("Update default GUI setting")
	@In
	public boolean $$uFLAG = false;

	@Description("Do not launch GUI after updating the default GUI setting")
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
