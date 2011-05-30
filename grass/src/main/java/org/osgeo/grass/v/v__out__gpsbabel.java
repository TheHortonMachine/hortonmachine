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

@Description("Exports a vector map to a GPS receiver or file format supported by GpsBabel.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, export, GPS")
@Label("Grass Vector Modules")
@Name("v__out__gpsbabel")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__out__gpsbabel {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@Description("Feature type(s) (optional)")
	@In
	public String $$typePARAMETER;

	@Description("Name for output file or GPS device")
	@In
	public String $$outputPARAMETER;

	@Description("GpsBabel supported output format (optional)")
	@In
	public String $$formatPARAMETER = "gpx";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Export as waypoints")
	@In
	public boolean $$wFLAG = false;

	@Description("Export as routes")
	@In
	public boolean $$rFLAG = false;

	@Description("Export as tracks")
	@In
	public boolean $$tFLAG = false;

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
