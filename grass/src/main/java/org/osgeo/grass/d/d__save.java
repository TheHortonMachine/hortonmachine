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

@Description("Creates a list of commands for recreating screen graphics.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass Display Modules")
@Name("d__save")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__save {

	@Description("Name of frame(s) to save (optional)")
	@In
	public String $$framePARAMETER;

	@Description("List of object numbers to remove which are displayed after \"#\". -1 for the last object. (optional)")
	@In
	public String $$removePARAMETER;

	@Description("List of object numbers to move (\"from\" to \"to\"). remove= option will be done first, if any. (optional)")
	@In
	public String $$movePARAMETER;

	@Description("Save current frame")
	@In
	public boolean $$cFLAG = false;

	@Description("Save all the frames")
	@In
	public boolean $$aFLAG = false;

	@Description("Only map objects without extra header and tailer")
	@In
	public boolean $$oFLAG = false;

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
