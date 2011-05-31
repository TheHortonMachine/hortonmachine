package org.osgeo.grass.r3;

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

@Description("Converts 3D raster maps to 2D raster maps")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, voxel")
@Label("Grass/Raster 3D Modules")
@Name("r3__to__rast")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__to__rast {

	@UI("infile,grassfile")
	@Description("3d raster map(s) to be converted to 2D raster slices")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Basename for resultant raster slice maps")
	@In
	public String $$outputPARAMETER;

	@Description("Use G3D mask (if exists) with input map")
	@In
	public boolean $$mFLAG = false;

	@Description("Use the same resolution as the input G3D map for the 2d output maps, independent of the current region settings")
	@In
	public boolean $$rFLAG = false;

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
