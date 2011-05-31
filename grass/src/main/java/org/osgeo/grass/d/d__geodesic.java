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

@Description("Displays a geodesic line, tracing the shortest distance between two geographic points along a great circle, in a longitude/latitude data set.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass Display Modules")
@Name("d__geodesic")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__geodesic {

	@Description("Starting and ending coordinates (optional)")
	@In
	public String $$coorPARAMETER;

	@Description("Line color (optional)")
	@In
	public String $$lcolorPARAMETER = "black";

	@Description("Text color or \"none\" (optional)")
	@In
	public String $$tcolorPARAMETER;

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
