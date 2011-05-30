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

@Description("Creates shaded relief map from an elevation map (DEM).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, elevation")
@Label("Grass Raster Modules")
@Name("r__shaded__relief")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__shaded__relief {

	@UI("infile")
	@Description("Input elevation map")
	@In
	public String $$mapPARAMETER;

	@UI("outfile")
	@Description("Output shaded relief map name (optional)")
	@In
	public String $$shadedmapPARAMETER;

	@Description("Altitude of the sun in degrees above the horizon (optional)")
	@In
	public String $$altitudePARAMETER = "30";

	@Description("Azimuth of the sun in degrees to the east of north (optional)")
	@In
	public String $$azimuthPARAMETER = "270";

	@Description("Factor for exaggerating relief (optional)")
	@In
	public String $$zmultPARAMETER = "1";

	@Description("Scale factor for converting horizontal units to elevation units (optional)")
	@In
	public String $$scalePARAMETER = "1";

	@Description("Set scaling factor (applies to lat./long. locations only, none: scale=1) (optional)")
	@In
	public String $$unitsPARAMETER = "none";

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
