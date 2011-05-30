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

@Description("Converts an ESRI ARC/INFO ascii raster file (GRID) into a (binary) raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__in__arc")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__in__arc {

	@Description("ARC/INFO ASCII raster file (GRID) to be imported")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Storage type for resultant raster map (optional)")
	@In
	public String $$typePARAMETER = "FCELL";

	@Description("Title for resultant raster map (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("Multiplier for ASCII data (optional)")
	@In
	public String $$multPARAMETER = "1.0";

	@Description("Allow output files to overwrite existing files")
	@In
	public boolean $$overwriteFLAG = false;

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
