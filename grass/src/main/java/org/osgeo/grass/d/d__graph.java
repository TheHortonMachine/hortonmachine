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

@Description("Program for generating and displaying simple graphics on the display monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, cartography")
@Name("d__graph")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__graph {

	@Description("Name of file containing graphics commands, if not given reads from standard input (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Color to draw with, either a standard GRASS color or R:G:B triplet (optional)")
	@In
	public String $$colorPARAMETER = "black";

	@Description("Coordinates are given in map units")
	@In
	public boolean $$mFLAG = false;

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
