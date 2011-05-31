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

@Description("To establish and control use of a graphics display monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass/Display Modules")
@Name("d__mon")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__mon {

	@Description("Name of graphics monitor to start (optional)")
	@In
	public String $$startPARAMETER;

	@Description("Name of graphics monitor to stop (optional)")
	@In
	public String $$stopPARAMETER;

	@Description("Name of graphics monitor to select (optional)")
	@In
	public String $$selectPARAMETER;

	@Description("Name of graphics monitor to unlock (optional)")
	@In
	public String $$unlockPARAMETER;

	@Description("List all monitors")
	@In
	public boolean $$lFLAG = false;

	@Description("List all monitors (with current status)")
	@In
	public boolean $$LFLAG = false;

	@Description("Print name of currently selected monitor")
	@In
	public boolean $$pFLAG = false;

	@Description("Release currently selected monitor")
	@In
	public boolean $$rFLAG = false;

	@Description("Do not automatically select when starting")
	@In
	public boolean $$sFLAG = false;

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
