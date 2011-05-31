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

@Description("Allows creation and/or modification of raster map layer support files.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, metadata")
@Label("Grass/Raster Modules")
@Name("r__support")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__support {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$mapPARAMETER;

	@Description("Text to use for map title (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("Text to append to the next line of the map's metadata file (optional)")
	@In
	public String $$historyPARAMETER;

	@Description("Text to use for map data units (optional)")
	@In
	public String $$unitsPARAMETER;

	@Description("Text to use for map vertical datum (optional)")
	@In
	public String $$vdatumPARAMETER;

	@Description("Text to use for data source, line 1 (optional)")
	@In
	public String $$source1PARAMETER;

	@Description("Text to use for data source, line 2 (optional)")
	@In
	public String $$source2PARAMETER;

	@Description("Text to use for data description or keyword(s) (optional)")
	@In
	public String $$descriptionPARAMETER;

	@UI("infile,grassfile")
	@Description("Raster map from which to copy category table (optional)")
	@In
	public String $$rasterPARAMETER;

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
