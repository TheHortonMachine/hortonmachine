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

@Description("Create a raster map from an assemblage of many coordinates using univariate statistics.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, import, LIDAR")
@Label("Grass Raster Modules")
@Name("r__in__xyz")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__in__xyz {

	@Description("ASCII file containing input data (or \"-\" to read from stdin)")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Statistic to use for raster values (optional)")
	@In
	public String $$methodPARAMETER = "mean";

	@Description("Storage type for resultant raster map (optional)")
	@In
	public String $$typePARAMETER = "FCELL";

	@Description("Field separator (optional)")
	@In
	public String $$fsPARAMETER = "|";

	@Description("Column number of x coordinates in input file (first column is 1) (optional)")
	@In
	public String $$xPARAMETER = "1";

	@Description("Column number of y coordinates in input file (optional)")
	@In
	public String $$yPARAMETER = "2";

	@Description("Column number of data values in input file (optional)")
	@In
	public String $$zPARAMETER = "3";

	@Description("Filter range for z data (min,max) (optional)")
	@In
	public String $$zrangePARAMETER;

	@Description("Scale to apply to z data (optional)")
	@In
	public String $$zscalePARAMETER = "1.0";

	@Description("Percent of map to keep in memory (optional)")
	@In
	public String $$percentPARAMETER = "100";

	@Description("pth percentile of the values (optional)")
	@In
	public String $$pthPARAMETER;

	@Description("Discard <trim> percent of the smallest and <trim> percent of the largest observations (optional)")
	@In
	public String $$trimPARAMETER;

	@Description("Scan data file for extent then exit")
	@In
	public boolean $$sFLAG = false;

	@Description("In scan mode, print using shell script style")
	@In
	public boolean $$gFLAG = false;

	@Description("Ignore broken lines")
	@In
	public boolean $$iFLAG = false;

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
