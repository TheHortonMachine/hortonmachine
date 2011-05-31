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

@Description("Outputs a covariance/correlation matrix for user-specified raster map layer(s).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__covar")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__covar {

	@UI("infile,grassfile")
	@Description("Name of input raster map(s)")
	@In
	public String $$mapPARAMETER;

	@Description("Print correlation matrix")
	@In
	public boolean $$rFLAG = false;

	@Description("Run quietly")
	@In
	public boolean $$qFLAG = false;

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
