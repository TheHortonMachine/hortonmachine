package org.osgeo.grass.v;

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

@Description("Converts all old GRASS < Ver5.7 sites maps in current mapset to vector maps.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("sites, vector, import")
@Label("Grass Vector Modules")
@Name("v__in__sites__all")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__sites__all {

	@Description("Run non-interactively")
	@In
	public boolean $$rFLAG = false;

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
