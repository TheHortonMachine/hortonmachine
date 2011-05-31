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

@Description("Outputs basic information about a user-specified 3D raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, voxel")
@Label("Grass/Raster 3D Modules")
@Name("r3__info")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__info {

	@UI("infile,grassfile")
	@Description("Name of input 3D raster map")
	@In
	public String $$mapPARAMETER;

	@Description("Print range only")
	@In
	public boolean $$rFLAG = false;

	@Description("Print 3D raster map resolution (NS-res, EW-res, TB-res) only")
	@In
	public boolean $$sFLAG = false;

	@Description("Print 3D raster map type (float/double) only")
	@In
	public boolean $$tFLAG = false;

	@Description("Print 3D raster map region only")
	@In
	public boolean $$gFLAG = false;

	@Description("Print 3D raster history instead of info")
	@In
	public boolean $$hFLAG = false;

	@Description("Print 3D raster map timestamp (day.month.year hour:minute:seconds) only")
	@In
	public boolean $$pFLAG = false;

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
