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

@Description("Slide show of GRASS raster/vector maps.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, slideshow")
@Name("d__slide__show")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__slide__show {

	@Description("Map prefix. Specify character(s) to view selected maps only (optional)")
	@In
	public String $$prefixPARAMETER;

	@Description("Map number show across the monitor (optional)")
	@In
	public String $$acrossPARAMETER;

	@Description("Map number show down the monitor (optional)")
	@In
	public String $$downPARAMETER;

	@Description("Mapsets to use. Specify multiple mapsets comma separated (optional)")
	@In
	public String $$mapsetsPARAMETER;

	@Description("Number of seconds to pause between slides (optional)")
	@In
	public String $$delayPARAMETER = "0";

	@Description("Show vector maps rather than raster maps")
	@In
	public boolean $$vFLAG = false;

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
