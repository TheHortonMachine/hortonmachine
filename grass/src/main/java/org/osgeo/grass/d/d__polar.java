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

@Description("Draws polar diagram of angle map such as aspect or flow directions")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, diagram")
@Name("d__polar")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__polar {

	@UI("infile,grassfile")
	@Description("Name of raster angle map")
	@In
	public String $$mapPARAMETER;

	@Description("Pixel value to be interpreted as undefined (different from NULL) (optional)")
	@In
	public String $$undefPARAMETER;

	@Description("Name of optional EPS output file (optional)")
	@In
	public String $$epsPARAMETER;

	@Description("Plot using Xgraph")
	@In
	public boolean $$xFLAG = false;

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
