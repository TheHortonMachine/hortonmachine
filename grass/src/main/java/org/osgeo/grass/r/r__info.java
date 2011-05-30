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

@Description("Output basic information about a raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__info")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__info {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$mapPARAMETER;

	@Description("Print range only")
	@In
	public boolean $$rFLAG = false;

	@Description("Print raster map resolution (NS-res, EW-res) only")
	@In
	public boolean $$sFLAG = false;

	@Description("Print raster map type only")
	@In
	public boolean $$tFLAG = false;

	@Description("Print map region only")
	@In
	public boolean $$gFLAG = false;

	@Description("Print raster history instead of info")
	@In
	public boolean $$hFLAG = false;

	@Description("Print raster map data units only")
	@In
	public boolean $$uFLAG = false;

	@Description("Print raster map vertical datum only")
	@In
	public boolean $$dFLAG = false;

	@Description("Print map title only")
	@In
	public boolean $$mFLAG = false;

	@Description("Print raster map timestamp (day.month.year hour:minute:seconds) only")
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
