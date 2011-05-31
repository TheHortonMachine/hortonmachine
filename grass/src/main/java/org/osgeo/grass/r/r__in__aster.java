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

@Description("Georeference, rectify and import Terra-ASTER imagery and relative DEM's using gdalwarp.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, imagery, import")
@Label("Grass/Raster Modules")
@Name("r__in__aster")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__in__aster {

	@Description("Input ASTER image to be georeferenced & rectified")
	@In
	public String $$inputPARAMETER;

	@Description("ASTER imagery processing type (Level 1A, Level 1B, or relative DEM)")
	@In
	public String $$proctypePARAMETER;

	@Description("L1A or L1B band to translate (1, 2, 3n, 3b, 4-14). Can only translate a single band")
	@In
	public String $$bandPARAMETER = "1";

	@UI("infile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

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
