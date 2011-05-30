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

@Description("Link GDAL supported raster file to a binary raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, import")
@Label("Grass Raster Modules")
@Name("r__external")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__external {

	@Description("Raster file to be linked (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Name of non-file GDAL data source (optional)")
	@In
	public String $$sourcePARAMETER;

	@UI("outfile")
	@Description("Name for output raster map (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Band to select (default: all) (optional)")
	@In
	public String $$bandPARAMETER;

	@Description("Title for resultant raster map (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("Override projection (use location's projection)")
	@In
	public boolean $$oFLAG = false;

	@Description("Extend location extents based on new dataset")
	@In
	public boolean $$eFLAG = false;

	@Description("Require exact range")
	@In
	public boolean $$rFLAG = false;

	@Description("List supported formats and exit")
	@In
	public boolean $$fFLAG = false;

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
