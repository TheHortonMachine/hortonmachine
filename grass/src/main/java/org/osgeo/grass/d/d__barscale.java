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

@Description("Displays a barscale on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, cartography")
@Label("Grass/Display Modules")
@Name("d__barscale")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__barscale {

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$bcolorPARAMETER = "white";

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$tcolorPARAMETER = "black";

	@Description("The screen coordinates for top-left corner of label ([0,0] is top-left of frame) (optional)")
	@In
	public String $$atPARAMETER = "0.0,0.0";

	@Description("Use mouse to interactively place scale")
	@In
	public boolean $$mFLAG = false;

	@Description("Use feet/miles instead of meters")
	@In
	public boolean $$fFLAG = false;

	@Description("Draw a line scale instead of a bar scale")
	@In
	public boolean $$lFLAG = false;

	@Description("Write text on top of the scale, not to the right")
	@In
	public boolean $$tFLAG = false;

	@Description("Draw a north arrow only")
	@In
	public boolean $$nFLAG = false;

	@Description("Draw a scale bar only")
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
