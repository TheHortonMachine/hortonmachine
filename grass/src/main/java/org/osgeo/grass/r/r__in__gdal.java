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

@Description("Import GDAL supported raster file into a binary raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, import")
@Label("Grass Raster Modules")
@Name("r__in__gdal")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__in__gdal {

	@Description("Raster file to be imported (optional)")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output raster map (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Band to select (default is all bands) (optional)")
	@In
	public String $$bandPARAMETER;

	@Description("Cache size (MiB) (optional)")
	@In
	public String $$memoryPARAMETER;

	@Description("Name of location to read projection from for GCPs transformation (optional)")
	@In
	public String $$targetPARAMETER;

	@Description("Title for resultant raster map (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("Name for new location to create (optional)")
	@In
	public String $$locationPARAMETER;

	@Description("Override projection (use location's projection)")
	@In
	public boolean $$oFLAG = false;

	@Description("Extend location extents based on new dataset")
	@In
	public boolean $$eFLAG = false;

	@Description("List supported formats and exit")
	@In
	public boolean $$fFLAG = false;

	@Description("Keep band numbers instead of using band color names")
	@In
	public boolean $$kFLAG = false;

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
