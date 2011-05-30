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

@Description("Convert coordinates from one projection to another (cs2cs frontend).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("miscellaneous, projection")
@Name("m__proj")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class m__proj {

	@Description("Input coordinate file (omit to read from stdin) (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Output coordinate file (omit to send to stdout) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Field separator (optional)")
	@In
	public String $$fsPARAMETER = "|";

	@Description("Input projection parameters (PROJ.4 style) (optional)")
	@In
	public String $$proj_inPARAMETER;

	@Description("Output projection parameters (PROJ.4 style) (optional)")
	@In
	public String $$proj_outPARAMETER;

	@Description("Use LL WGS84 as input and current location as output projection")
	@In
	public boolean $$iFLAG = false;

	@Description("Use current location as input and LL WGS84 as output projection")
	@In
	public boolean $$oFLAG = false;

	@Description("Output long/lat in decimal degrees or other projections with many decimal places")
	@In
	public boolean $$dFLAG = false;

	@Description("Script style output in CSV format respecting the field separator settings")
	@In
	public boolean $$gFLAG = false;

	@Description("Verbose mode (print projection parameters and filenames to stderr)")
	@In
	public boolean $$vFLAG = false;

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
