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

@Description("Import SRTM HGT files into GRASS")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, import")
@Label("Grass Raster Modules")
@Name("r__in__srtm")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__in__srtm {

	@Description("SRTM input tile (file without .hgt.zip extension)")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Output raster map (default: input tile) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Input is a 1-arcsec tile (default: 3-arcsec)")
	@In
	public boolean $$1FLAG = false;

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
