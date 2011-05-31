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

@Description("Finds shortest path for selected starting and ending node.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, networking")
@Name("d__path")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__path {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Arc type (optional)")
	@In
	public String $$typePARAMETER = "line,boundary";

	@Description("Starting and ending coordinates (optional)")
	@In
	public String $$coorPARAMETER;

	@Description("Arc layer (optional)")
	@In
	public String $$alayerPARAMETER = "1";

	@Description("Node layer (optional)")
	@In
	public String $$nlayerPARAMETER = "2";

	@Description("Arc forward/both direction(s) cost column (optional)")
	@In
	public String $$afcolPARAMETER;

	@Description("Arc backward direction cost column (optional)")
	@In
	public String $$abcolPARAMETER;

	@Description("Node cost column (optional)")
	@In
	public String $$ncolPARAMETER;

	@Description("Original line color (optional)")
	@In
	public String $$colorPARAMETER = "black";

	@Description("Highlight color (optional)")
	@In
	public String $$hcolorPARAMETER = "red";

	@Description("Background color (optional)")
	@In
	public String $$bgcolorPARAMETER = "white";

	@Description("Use geodesic calculation for longitude-latitude locations")
	@In
	public boolean $$gFLAG = false;

	@Description("Render bold lines")
	@In
	public boolean $$bFLAG = false;

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
