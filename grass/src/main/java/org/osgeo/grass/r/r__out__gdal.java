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

@Description("Exports GRASS raster maps into GDAL supported formats.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, export")
@Label("Grass Raster Modules")
@Name("r__out__gdal")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__gdal {

	@UI("infile")
	@Description("Name of raster map (or group) to export (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("GIS format to write (case sensitive, see also -l flag) (optional)")
	@In
	public String $$formatPARAMETER = "GTiff";

	@Description("File type (optional)")
	@In
	public String $$typePARAMETER;

	@Description("Name for output raster file (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("In the form of \"NAME=VALUE\", separate multiple entries with a comma. (optional)")
	@In
	public String $$createoptPARAMETER;

	@Description("In the form of \"META-TAG=VALUE\", separate multiple entries with a comma. Not supported by all output format drivers. (optional)")
	@In
	public String $$metaoptPARAMETER;

	@Description("Assign a specified nodata value to output bands (optional)")
	@In
	public String $$nodataPARAMETER;

	@Description("List supported output formats")
	@In
	public boolean $$lFLAG = false;

	@Description("Only applicable to Byte or UInt16 data types.")
	@In
	public boolean $$cFLAG = false;

	@Description("Overrides nodata safety check.")
	@In
	public boolean $$fFLAG = false;

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
