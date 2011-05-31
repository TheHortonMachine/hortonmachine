package org.osgeo.grass.v;

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

@Description("Exports a GRASS vector map to SVG.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, export")
@Label("Grass/Vector Modules")
@Name("v__out__svg")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__out__svg {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@Description("Name for SVG output file")
	@In
	public String $$outputPARAMETER;

	@Description("Defines which feature-type will be extracted")
	@In
	public String $$typePARAMETER = "poly";

	@Description("Coordinate precision (optional)")
	@In
	public String $$precisionPARAMETER = "6";

	@Description("Attribute(s) to include in output SVG (optional)")
	@In
	public String $$attributePARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

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
