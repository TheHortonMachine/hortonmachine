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

@Description("Selects/starts specified monitor at specified size")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, setup")
@Name("d__monsize")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__monsize {

	@Description("Display monitor to start")
	@In
	public String $$setmonitorPARAMETER;

	@Description("Width in pixels of new display monitor")
	@In
	public String $$setwidthPARAMETER;

	@Description("Height in pixels of new display monitor")
	@In
	public String $$setheightPARAMETER;

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
