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

@Description("Calculates linear regression from two raster maps: y = a + b*x")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, statistics")
@Label("Grass Raster Modules")
@Name("r__regression__line")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__regression__line {

	@UI("infile")
	@Description("Map for x coefficient")
	@In
	public String $$map1PARAMETER;

	@UI("infile")
	@Description("Map for y coefficient")
	@In
	public String $$map2PARAMETER;

	@Description("ASCII file for storing regression coefficients (output to screen if file not specified). (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Print in shell script style")
	@In
	public boolean $$gFLAG = false;

	@Description("Slower but accurate (applies to FP maps only)")
	@In
	public boolean $$sFLAG = false;

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
