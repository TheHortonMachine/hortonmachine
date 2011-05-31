package org.osgeo.grass.m;

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

@Description("It assumes a cartesian coordinate system")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("miscellaneous")
@Label("Grass")
@Name("m__cogo")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class m__cogo {

	@Description("Name of input file (optional)")
	@In
	public String $$inputPARAMETER = "-";

	@Description("Name for output file (optional)")
	@In
	public String $$outputPARAMETER = "-";

	@Description("Starting coordinate pair (optional)")
	@In
	public String $$coordPARAMETER = "0.0,0.0";

	@Description("Lines are labelled")
	@In
	public boolean $$lFLAG = false;

	@Description("Suppress warnings")
	@In
	public boolean $$qFLAG = false;

	@Description("Convert from coordinates to bearing and distance")
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
