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

@Description("Reclasses a raster map greater or less than user specified area size (in hectares).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, statistics, aggregation")
@Label("Grass Raster Modules")
@Name("r__reclass__area")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__reclass__area {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Lesser value option that sets the <= area size limit [hectares] (optional)")
	@In
	public String $$lesserPARAMETER;

	@Description("Greater value option that sets the >= area size limit [hectares] (optional)")
	@In
	public String $$greaterPARAMETER;

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
