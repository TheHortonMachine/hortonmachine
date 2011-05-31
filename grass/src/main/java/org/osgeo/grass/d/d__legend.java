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

@Description("Displays a legend for a raster map in the active frame of the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, cartography")
@Label("Grass/Display Modules")
@Name("d__legend")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__legend {

	@UI("infile,grassfile")
	@Description("Name of raster map")
	@In
	public String $$mapPARAMETER;

	@Description("Sets the legend's text color (optional)")
	@In
	public String $$colorPARAMETER = "black";

	@Description("Number of text lines (useful for truncating long legends) (optional)")
	@In
	public String $$linesPARAMETER = "0";

	@Description("Thinning factor (thin=10 gives cats 0,10,20...) (optional)")
	@In
	public String $$thinPARAMETER = "1";

	@Description("Number of text labels for smooth gradient legend (optional)")
	@In
	public String $$labelnumPARAMETER = "5";

	@Description("bottom,top,left,right (optional)")
	@In
	public String $$atPARAMETER;

	@Description("List of discrete category numbers/values for legend (optional)")
	@In
	public String $$usePARAMETER;

	@Description("Use a subset of the map range for the legend (min,max) (optional)")
	@In
	public String $$rangePARAMETER;

	@Description("Use mouse to size & place legend")
	@In
	public boolean $$mFLAG = false;

	@Description("Do not show category labels")
	@In
	public boolean $$vFLAG = false;

	@Description("Do not show category numbers")
	@In
	public boolean $$cFLAG = false;

	@Description("Skip categories with no label")
	@In
	public boolean $$nFLAG = false;

	@Description("Draw smooth gradient")
	@In
	public boolean $$sFLAG = false;

	@Description("Flip legend")
	@In
	public boolean $$fFLAG = false;

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
