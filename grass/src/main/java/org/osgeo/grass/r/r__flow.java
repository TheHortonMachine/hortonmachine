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

@Description("Construction of slope curves (flowlines), flowpath lengths, and flowline densities (upslope areas) from a raster digital elevation model (DEM)")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__flow")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__flow {

	@UI("infile")
	@Description("Input elevation raster map")
	@In
	public String $$elevinPARAMETER;

	@UI("infile")
	@Description("Input aspect raster map (optional)")
	@In
	public String $$aspinPARAMETER;

	@UI("infile")
	@Description("Input barrier raster map (optional)")
	@In
	public String $$barinPARAMETER;

	@Description("Number of cells between flowlines (optional)")
	@In
	public String $$skipPARAMETER = "1";

	@Description("Maximum number of segments per flowline (optional)")
	@In
	public String $$boundPARAMETER = "5";

	@Description("Output flowline vector map (optional)")
	@In
	public String $$floutPARAMETER;

	@Description("Output flowpath length raster map (optional)")
	@In
	public String $$lgoutPARAMETER;

	@Description("Output flowline density raster map (optional)")
	@In
	public String $$dsoutPARAMETER;

	@Description("Compute upslope flowlines instead of default downhill flowlines")
	@In
	public boolean $$uFLAG = false;

	@Description("3-D lengths instead of 2-D")
	@In
	public boolean $$3FLAG = false;

	@Description("Use less memory, at a performance penalty")
	@In
	public boolean $$mFLAG = false;

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
