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

@Description("Export display monitor to a GpsDrive compatible backdrop image")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, export, GPS")
@Name("d__out__gpsdrive")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__out__gpsdrive {

	@Description("name for new map image (lives in ~/.gpsdrive/maps/)")
	@In
	public String $$outputPARAMETER;

	@Description("Make JPEG instead of PNG image")
	@In
	public boolean $$jFLAG = false;

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
