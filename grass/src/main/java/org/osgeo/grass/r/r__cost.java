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

@Description("Creates a raster map showing the cumulative cost of moving between different geographic locations on an input raster map whose cell category values represent cost.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, cost surface, cumulative costs")
@Label("Grass Raster Modules")
@Name("r__cost")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__cost {

	@UI("infile,grassfile")
	@Description("Name of raster map containing grid cell cost information")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of starting vector points map (optional)")
	@In
	public String $$start_pointsPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of stop vector points map (optional)")
	@In
	public String $$stop_pointsPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of starting raster points map (optional)")
	@In
	public String $$start_rastPARAMETER;

	@Description("Map grid coordinates of a starting point (E,N) (optional)")
	@In
	public String $$coordinatePARAMETER;

	@Description("Map grid coordinates of a stopping point (E,N) (optional)")
	@In
	public String $$stop_coordinatePARAMETER;

	@Description("Optional maximum cumulative cost (optional)")
	@In
	public String $$max_costPARAMETER = "0";

	@Description("Cost assigned to null cells. By default, null cells are excluded (optional)")
	@In
	public String $$null_costPARAMETER;

	@Description("Percent of map to keep in memory (optional)")
	@In
	public String $$percent_memoryPARAMETER = "100";

	@Description("Use the 'Knight's move'; slower, but more accurate")
	@In
	public boolean $$kFLAG = false;

	@Description("Keep null values in output raster map")
	@In
	public boolean $$nFLAG = false;

	@Description("Start with values in raster map")
	@In
	public boolean $$rFLAG = false;

	@Description("Run verbosely")
	@In
	public boolean $$vFLAG = false;

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
