package org.osgeo.grass.v;

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

@Description("Calculates univariate statistics for attribute. Variance and standard deviation is calculated only for points if specified.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, statistics")
@Label("Grass Vector Modules")
@Name("v__univar")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__univar {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid,area";

	@Description("Column name")
	@In
	public String $$columnPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

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
