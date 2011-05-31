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

@Description("Generates random surface(s) with spatial dependence.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__random__surface")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__random__surface {

	@UI("outfile,grassfile")
	@Description("Names of the resulting maps")
	@In
	public String $$outputPARAMETER;

	@Description("Input value: max. distance of spatial correlation (value >= 0.0, default [0.0]) (optional)")
	@In
	public String $$distancePARAMETER;

	@Description("Input value: distance decay exponent (value > 0.0), default [1.0]) (optional)")
	@In
	public String $$exponentPARAMETER;

	@Description("Input value: distance filter remains flat before beginning exponent, default [0.0] (optional)")
	@In
	public String $$flatPARAMETER;

	@Description("Input value: random seed (SEED_MIN >= value >= SEED_MAX), default [random] (optional)")
	@In
	public String $$seedPARAMETER;

	@Description("Input value: maximum cell value of distribution, default [255] (optional)")
	@In
	public String $$highPARAMETER;

	@Description("Uniformly distributed cell values")
	@In
	public boolean $$uFLAG = false;

	@Description("No (quiet) description during run")
	@In
	public boolean $$qFLAG = false;

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
