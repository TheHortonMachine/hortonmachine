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

@Description("Saves active display monitor to PNG file in home directory")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, export")
@Label("Grass/Display Modules")
@Name("d__out__png")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__out__png {

	@Description("Name of PNG file")
	@In
	public String $$outputPARAMETER;

	@Description("Resolution of output file (single=1, double=2, quad=4) (optional)")
	@In
	public String $$resPARAMETER = "2";

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
