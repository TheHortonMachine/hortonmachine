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

@Description("Generates random cell values with spatial dependence.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__random__cells")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__random__cells {

	@UI("outfile")
	@Description("Name of independent cells map")
	@In
	public String $$outputPARAMETER;

	@Description("Input value: max. distance of spatial correlation (value(s) >= 0.0)")
	@In
	public String $$distancePARAMETER;

	@Description("Input value: random seed (SEED_MIN >= value >= SEED_MAX), default [random] (optional)")
	@In
	public String $$seedPARAMETER;

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
