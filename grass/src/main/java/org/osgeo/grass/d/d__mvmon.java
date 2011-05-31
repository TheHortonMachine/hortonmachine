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

@Description("Moves displayed maps to another monitor")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, setup")
@Label("Grass/Display Modules")
@Name("d__mvmon")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__mvmon {

	@Description("Target monitor")
	@In
	public String $$toPARAMETER;

	@Description("Source monitor (optional)")
	@In
	public String $$fromPARAMETER;

	@Description("clear target monitor before moving")
	@In
	public boolean $$cFLAG = false;

	@Description("stay with source monitor")
	@In
	public boolean $$sFLAG = false;

	@Description("kill source monitor after moving")
	@In
	public boolean $$kFLAG = false;

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
