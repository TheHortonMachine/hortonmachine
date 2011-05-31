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

@Description("Overlays a user-specified grid in the active display frame on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, cartography")
@Label("Grass/Display Modules")
@Name("d__grid")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__grid {

	@Description("In map units or DDD:MM:SS format. Example: \"1000\" or \"0:10\"")
	@In
	public String $$sizePARAMETER;

	@Description("Lines of the grid pass through this coordinate (optional)")
	@In
	public String $$originPARAMETER = "0,0";

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$colorPARAMETER = "gray";

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$bordercolorPARAMETER = "black";

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$textcolorPARAMETER = "gray";

	@Description("Font size for gridline coordinate labels (optional)")
	@In
	public String $$fontsizePARAMETER = "9";

	@Description("Draw geographic grid (referenced to current ellipsoid)")
	@In
	public boolean $$gFLAG = false;

	@Description("Draw geographic grid (referenced to WGS84 ellipsoid)")
	@In
	public boolean $$wFLAG = false;

	@Description("Draw '+' marks instead of grid lines")
	@In
	public boolean $$cFLAG = false;

	@Description("Draw fiducial marks instead of grid lines")
	@In
	public boolean $$fFLAG = false;

	@Description("Disable grid drawing")
	@In
	public boolean $$nFLAG = false;

	@Description("Disable border drawing")
	@In
	public boolean $$bFLAG = false;

	@Description("Disable text drawing")
	@In
	public boolean $$tFLAG = false;

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
