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

@Description("Interactive tool used to setup the sampling and analysis framework that will be used by the other r.le programs.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__le__setup")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__le__setup {

	@UI("infile,grassfile")
	@Description("Raster map to use to setup sampling")
	@In
	public String $$mapPARAMETER;

	@UI("infile,grassfile")
	@Description("Vector map to overlay (optional)")
	@In
	public String $$vectPARAMETER;

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
