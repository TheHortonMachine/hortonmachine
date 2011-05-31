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

@Description("Fills lake from seed at given level.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__lake")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__lake {

	@UI("infile,grassfile")
	@Description("Name of terrain raster map (DEM)")
	@In
	public String $$demPARAMETER;

	@Description("Water level")
	@In
	public String $$wlPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map with lake (optional)")
	@In
	public String $$lakePARAMETER;

	@Description("Seed point coordinates (optional)")
	@In
	public String $$xyPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map with seed (at least 1 cell > 0) (optional)")
	@In
	public String $$seedPARAMETER;

	@Description("Use negative depth values for lake raster map")
	@In
	public boolean $$nFLAG = false;

	@Description("Overwrite seed map with result (lake) map")
	@In
	public boolean $$oFLAG = false;

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
