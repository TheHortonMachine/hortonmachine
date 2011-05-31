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

@Description("Produces a vector map of specified contours from a raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, DEM, contours, vector")
@Label("Grass/Raster Modules")
@Name("r__contour")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__contour {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("List of contour levels (optional)")
	@In
	public String $$levelsPARAMETER;

	@Description("Minimum contour level (optional)")
	@In
	public String $$minlevelPARAMETER;

	@Description("Maximum contour level (optional)")
	@In
	public String $$maxlevelPARAMETER;

	@Description("Increment between contour levels (optional)")
	@In
	public String $$stepPARAMETER;

	@Description("Minimum number of points for a contour line (0 -> no limit) (optional)")
	@In
	public String $$cutPARAMETER = "0";

	@Description("Run quietly")
	@In
	public boolean $$qFLAG = false;

	@Description("Suppress single crossing error messages")
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
