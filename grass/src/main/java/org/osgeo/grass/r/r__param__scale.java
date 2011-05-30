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

@Description("Uses a multi-scale approach by taking fitting quadratic parameters to any size window (via least squares).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, geomorphology")
@Label("Grass Raster Modules")
@Name("r__param__scale")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__param__scale {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Output raster layer containing morphometric parameter")
	@In
	public String $$outputPARAMETER;

	@Description("Slope tolerance that defines a 'flat' surface (degrees) (optional)")
	@In
	public String $$s_tolPARAMETER = "1.0";

	@Description("Curvature tolerance that defines 'planar' surface (optional)")
	@In
	public String $$c_tolPARAMETER = "0.0001";

	@Description("Size of processing window (odd number only, max: 69) (optional)")
	@In
	public String $$sizePARAMETER = "3";

	@Description("Morphometric parameter in 'size' window to calculate (optional)")
	@In
	public String $$paramPARAMETER = "elev";

	@Description("Exponent for distance weighting (0.0-4.0) (optional)")
	@In
	public String $$expPARAMETER = "0.0";

	@Description("Vertical scaling factor (optional)")
	@In
	public String $$zscalePARAMETER = "1.0";

	@Description("Constrain model through central window cell")
	@In
	public boolean $$cFLAG = false;

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
