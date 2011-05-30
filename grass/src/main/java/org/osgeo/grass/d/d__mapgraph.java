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

@Description("Generates and displays simple graphics on map layers drawn in the active graphics monitor display frame.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__mapgraph")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__mapgraph {

	@Description("Unix file containg graphing instructions, if not given reads from standard input (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Color to draw with, either a standard GRASS color or R:G:B triplet (separated by colons) (optional)")
	@In
	public String $$colorPARAMETER = "black";

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
