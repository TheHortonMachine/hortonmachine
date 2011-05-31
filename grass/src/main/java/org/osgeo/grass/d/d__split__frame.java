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

@Description("Split the display into subframes.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Label("Grass Display Modules")
@Name("d__split__frame")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__split__frame {

	@Description("Number of subframes (optional)")
	@In
	public String $$framesPARAMETER = "4";

	@Description("Split horizontally not vertically")
	@In
	public boolean $$hFLAG = false;

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
