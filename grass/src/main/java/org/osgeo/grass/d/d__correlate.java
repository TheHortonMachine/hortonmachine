package org.osgeo.grass.d;

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

@Description("Prints a graph of the correlation between data layers (in pairs).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, diagram")
@Name("d__correlate")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__correlate {

	@UI("infile")
	@Description("raster input map")
	@In
	public String $$layer1PARAMETER;

	@UI("infile")
	@Description("raster input map")
	@In
	public String $$layer2PARAMETER;

	@UI("infile")
	@Description("raster input map (optional)")
	@In
	public String $$layer3PARAMETER;

	@UI("infile")
	@Description("raster input map (optional)")
	@In
	public String $$layer4PARAMETER;

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
