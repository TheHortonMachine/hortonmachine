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

@Description("Measures the lengths and areas of features drawn by the user in the active display frame on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass Display Modules")
@Name("d__measure")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__measure {

	@Description("Line color 1 (optional)")
	@In
	public String $$c1PARAMETER = "white";

	@Description("Line color 2 (optional)")
	@In
	public String $$c2PARAMETER = "black";

	@Description("Suppress clear screen")
	@In
	public boolean $$sFLAG = false;

	@Description("Output in meters only")
	@In
	public boolean $$mFLAG = false;

	@Description("Output in kilometers as well")
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
