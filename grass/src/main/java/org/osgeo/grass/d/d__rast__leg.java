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

@Description("Displays a raster map and its legend on a graphics window")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__rast__leg")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__rast__leg {

	@UI("infile")
	@Description("Name of raster map")
	@In
	public String $$mapPARAMETER;

	@Description("Number of lines to appear in the legend (optional)")
	@In
	public String $$num_of_linesPARAMETER;

	@UI("infile")
	@Description("Name of raster map to generate legend from (optional)")
	@In
	public String $$rastPARAMETER;

	@Description("Name of raster map to print in legend (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("Position of vertical map-legend separator (in percent) (optional)")
	@In
	public String $$positionPARAMETER = "65";

	@Description("Flip legend")
	@In
	public boolean $$fFLAG = false;

	@Description("Omit entries with missing label")
	@In
	public boolean $$nFLAG = false;

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
