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

@Description("Displays and overlays raster map layers in the active display frame on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, raster")
@Label("Grass Display Modules")
@Name("d__rast")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__rast {

	@UI("infile,grassfile")
	@Description("Raster map to be displayed")
	@In
	public String $$mapPARAMETER;

	@Description("List of categories to be displayed (INT maps) (optional)")
	@In
	public String $$catlistPARAMETER;

	@Description("List of values to be displayed (FP maps) (optional)")
	@In
	public String $$vallistPARAMETER;

	@Description("Background color (for null) (optional)")
	@In
	public String $$bgPARAMETER;

	@Description("Overlay (non-null values only)")
	@In
	public boolean $$oFLAG = false;

	@Description("Invert catlist")
	@In
	public boolean $$iFLAG = false;

	@Description("Don't add to list of rasters and commands in monitor")
	@In
	public boolean $$xFLAG = false;

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
