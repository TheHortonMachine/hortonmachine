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

@Description("Line-of-sight raster analysis program.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__los")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__los {

	@UI("infile,grassfile")
	@Description("Name of elevation raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Coordinate identifying the viewing position")
	@In
	public String $$coordinatePARAMETER;

	@UI("infile,grassfile")
	@Description("Binary (1/0) raster map to use as a mask (optional)")
	@In
	public String $$patt_mapPARAMETER;

	@Description("Viewing position height above the ground (optional)")
	@In
	public String $$obs_elevPARAMETER = "1.75";

	@Description("Maximum distance from the viewing point (meters) (optional)")
	@In
	public String $$max_distPARAMETER = "10000";

	@Description("Consider earth curvature (current ellipsoid)")
	@In
	public boolean $$cFLAG = false;

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
