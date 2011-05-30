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

@Description("Re-projects a raster map from one location to the current location.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, projection")
@Label("Grass Raster Modules")
@Name("r__proj")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__proj {

	@UI("infile")
	@Description("Name of input raster map to re-project (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Location of input raster map")
	@In
	public String $$locationPARAMETER;

	@Description("Mapset of input raster map (optional)")
	@In
	public String $$mapsetPARAMETER;

	@Description("Path to GRASS database of input location (optional)")
	@In
	public String $$dbasePARAMETER;

	@UI("outfile")
	@Description("Name for output raster map (default: input) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Interpolation method to use (optional)")
	@In
	public String $$methodPARAMETER = "nearest";

	@Description("Cache size (MiB) (optional)")
	@In
	public String $$memoryPARAMETER;

	@Description("Resolution of output map (optional)")
	@In
	public String $$resolutionPARAMETER;

	@Description("List raster maps in input location and exit")
	@In
	public boolean $$lFLAG = false;

	@Description("Do not perform region cropping optimization")
	@In
	public boolean $$nFLAG = false;

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
