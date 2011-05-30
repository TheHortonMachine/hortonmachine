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

@Description("Calculates univariate statistics from the non-null 3d cells of a raster3d map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, statistics")
@Label("Grass Raster 3D Modules")
@Name("r3__univar")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__univar {

	@UI("infile")
	@Description("Name of input raster3d map")
	@In
	public String $$inputPARAMETER;

	@Description("Percentile to calculate (requires extended statistics flag) (optional)")
	@In
	public String $$percentilePARAMETER = "90";

	@Description("Print the stats in shell script style")
	@In
	public boolean $$gFLAG = false;

	@Description("Calculate extended statistics")
	@In
	public boolean $$eFLAG = false;

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
