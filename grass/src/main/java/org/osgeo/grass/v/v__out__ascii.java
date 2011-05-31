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

@Description("Converts a GRASS binary vector map to a GRASS ASCII vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, export")
@Label("Grass Vector Modules")
@Name("v__out__ascii")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__out__ascii {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@Description("Path to resulting ASCII file or ASCII vector name if '-o' is defined (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Output format (optional)")
	@In
	public String $$formatPARAMETER = "point";

	@Description("Field separator (points mode) (optional)")
	@In
	public String $$fsPARAMETER = "|";

	@Description("Number of significant digits (floating point only) (optional)")
	@In
	public String $$dpPARAMETER = "8";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Name of attribute column(s) to be exported (point mode) (optional)")
	@In
	public String $$columnsPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Create old (version 4) ASCII file")
	@In
	public boolean $$oFLAG = false;

	@Description("Only export points falling within current 3D region (points mode)")
	@In
	public boolean $$rFLAG = false;

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
