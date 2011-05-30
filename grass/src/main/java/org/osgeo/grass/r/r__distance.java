package org.osgeo.grass.r;

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

@Description("Locates the closest points between objects in two raster maps.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__distance")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__distance {

	@UI("infile")
	@Description("Maps for computing inter-class distances")
	@In
	public String $$mapsPARAMETER;

	@Description("Output field separator (optional)")
	@In
	public String $$fsPARAMETER = ":";

	@Description("Include category labels in the output")
	@In
	public boolean $$lFLAG = false;

	@Description("Report zero distance if rasters are overlapping")
	@In
	public boolean $$oFLAG = false;

	@Description("Run quietly")
	@In
	public boolean $$qFLAG = false;

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
