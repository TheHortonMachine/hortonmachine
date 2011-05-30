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

@Description("Outputs raster map layer values lying along user defined transect line(s).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__transect")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__transect {

	@UI("infile")
	@Description("Raster map to be queried")
	@In
	public String $$mapPARAMETER;

	@Description("Transect definition")
	@In
	public String $$linePARAMETER;

	@Description("Char string to represent no data cell (optional)")
	@In
	public String $$nullPARAMETER = "*";

	@Description("Output easting and northing in first two columns of four column output")
	@In
	public boolean $$gFLAG = false;

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
