package org.osgeo.grass.i;

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

@Description("Targets an imagery group to a GRASS location and mapset.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery")
@Label("Grass Imagery Modules")
@Name("i__target")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__target {

	@Description("Name of input imagery group")
	@In
	public String $$groupPARAMETER;

	@Description("Name of imagery target location (optional)")
	@In
	public String $$locationPARAMETER;

	@Description("Name of target mapset (optional)")
	@In
	public String $$mapsetPARAMETER;

	@Description("Set current location and mapset as target for of imagery group")
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
