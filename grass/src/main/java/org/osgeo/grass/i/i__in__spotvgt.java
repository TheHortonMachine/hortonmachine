package org.osgeo.grass.i;

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

@Description("Import of SPOT VGT NDVI file into a raster map")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, imagery, import")
@Label("Grass/Imagery Modules")
@Name("i__in__spotvgt")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__in__spotvgt {

	@Description("existing SPOT VGT NDVI HDF file (0001_NDV.HDF)")
	@In
	public String $$filePARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map (optional)")
	@In
	public String $$rastPARAMETER;

	@Description("also import quality map (SM status map layer) and filter NDVI map")
	@In
	public boolean $$aFLAG = false;

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
