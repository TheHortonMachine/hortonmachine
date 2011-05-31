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

@Description("Drapes a color raster over a shaded relief map using d.his")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Name("d__shadedmap")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__shadedmap {

	@UI("infile,grassfile")
	@Description("Name of shaded relief or aspect map")
	@In
	public String $$reliefmapPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster to drape over relief map")
	@In
	public String $$drapemapPARAMETER;

	@Description("Percent to brighten (optional)")
	@In
	public String $$brightenPARAMETER = "0";

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
