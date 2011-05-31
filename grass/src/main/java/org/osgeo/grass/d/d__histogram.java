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

@Description("Displays a histogram in the form of a pie or bar chart for a user-specified raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass Display Modules")
@Name("d__histogram")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__histogram {

	@UI("infile,grassfile")
	@Description("Raster map for which histogram will be displayed")
	@In
	public String $$mapPARAMETER;

	@Description("Indicate if a pie or bar chart is desired (optional)")
	@In
	public String $$stylePARAMETER = "bar";

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$colorPARAMETER = "black";

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$bgcolorPARAMETER = "white";

	@Description("Number of steps to divide the data range into (fp maps only) (optional)")
	@In
	public String $$nstepsPARAMETER = "255";

	@Description("Display information for null cells")
	@In
	public boolean $$nFLAG = false;

	@Description("Gather the histogram quietly")
	@In
	public boolean $$qFLAG = false;

	@Description("Report for ranges defined in cats file (fp maps only)")
	@In
	public boolean $$CFLAG = false;

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
