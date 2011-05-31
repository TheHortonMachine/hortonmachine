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

@Description("Divides active display into two frames & displays maps/executes commands in each frame.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, setup")
@Label("Grass/Display Modules")
@Name("d__split")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__split {

	@UI("infile,grassfile")
	@Description("Enter raster map to display in 1st frame (optional)")
	@In
	public String $$map1PARAMETER;

	@Description("Enter command to execute in 1st frame (optional)")
	@In
	public String $$cmd1PARAMETER = "d.rast";

	@UI("infile,grassfile")
	@Description("Enter raster map to display in 2nd frame (optional)")
	@In
	public String $$map2PARAMETER;

	@Description("Enter command to execute in 2nd frame (optional)")
	@In
	public String $$cmd2PARAMETER = "d.rast";

	@Description("How to split display")
	@In
	public String $$viewPARAMETER = "vert";

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
