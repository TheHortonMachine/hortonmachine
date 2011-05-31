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

@Description("Computes horizon angle height from a digital elevation model. The module has two different modes of operation:  1. Computes the entire horizon around a single point whose coordinates are given with the 'coord' option. The horizon height (in radians). 2. Computes one or more raster maps of the horizon height in a single direction.  The input for this is the angle (in degrees), which is measured  counterclockwise with east=0, north=90 etc. The output is the horizon height in radians.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__horizon")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__horizon {

	@UI("infile,grassfile")
	@Description("Name of the input elevation raster map [meters]")
	@In
	public String $$elevinPARAMETER;

	@Description("Direction in which you want to know the horizon height (optional)")
	@In
	public String $$directionPARAMETER;

	@Description("Angle step size for multidirectional horizon [degrees] (optional)")
	@In
	public String $$horizonstepPARAMETER;

	@Description("For horizon rasters, read from the DEM an extra buffer around the present region (optional)")
	@In
	public String $$bufferzonePARAMETER;

	@Description("For horizon rasters, read from the DEM an extra buffer eastward the present region (optional)")
	@In
	public String $$e_buffPARAMETER;

	@Description("For horizon rasters, read from the DEM an extra buffer westward the present region (optional)")
	@In
	public String $$w_buffPARAMETER;

	@Description("For horizon rasters, read from the DEM an extra buffer northward the present region (optional)")
	@In
	public String $$n_buffPARAMETER;

	@Description("For horizon rasters, read from the DEM an extra buffer southward the present region (optional)")
	@In
	public String $$s_buffPARAMETER;

	@Description("The maximum distance to consider when finding the horizon height (optional)")
	@In
	public String $$maxdistancePARAMETER;

	@UI("infile,grassfile")
	@Description("Prefix of the horizon raster output maps (optional)")
	@In
	public String $$horizonPARAMETER;

	@Description("Coordinate for which you want to calculate the horizon (optional)")
	@In
	public String $$coordPARAMETER;

	@Description("Sampling distance step coefficient (0.5-1.5) (optional)")
	@In
	public String $$distPARAMETER = "1.0";

	@Description("Write output in degrees (default is radians)")
	@In
	public boolean $$dFLAG = false;

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
