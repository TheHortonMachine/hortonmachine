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

@Description("Exports GRASS raster into GDAL supported formats.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, export")
@Label("Grass/Raster Modules")
@Name("r__out__gdal__sh")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__gdal__sh {

	@UI("infile,grassfile")
	@Description("Name of input raster map (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("GIS format to write (case sensitive, see also -l flag) (optional)")
	@In
	public String $$formatPARAMETER = "GTiff";

	@Description("File type (optional)")
	@In
	public String $$typePARAMETER;

	@Description("Name for output file (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Creation option to the output format driver. Multiple options may be listed (optional)")
	@In
	public String $$createoptPARAMETER;

	@Description("Metadata key passed on the output dataset if possible (optional)")
	@In
	public String $$metaoptPARAMETER;

	@Description("List supported output formats")
	@In
	public boolean $$lFLAG = false;

	@Description("Region sensitive output")
	@In
	public boolean $$rFLAG = false;

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
