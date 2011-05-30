package org.osgeo.grass.r;

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

@Description("Converts a raster map layer into an ASCII text file.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, export")
@Label("Grass Raster Modules")
@Name("r__out__ascii")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__ascii {

	@UI("infile")
	@Description("Name of an existing raster map")
	@In
	public String $$inputPARAMETER;

	@Description("Name for output ASCII grid map (use out=- for stdout) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Number of significant digits (floating point only) (optional)")
	@In
	public String $$dpPARAMETER;

	@Description("Number of values printed before wrapping a line (only SURFER or MODFLOW format) (optional)")
	@In
	public String $$widthPARAMETER;

	@Description("String to represent null cell (GRASS grid only) (optional)")
	@In
	public String $$nullPARAMETER = "*";

	@Description("Suppress printing of header information")
	@In
	public boolean $$hFLAG = false;

	@Description("Write SURFER (Golden Software) ASCII grid")
	@In
	public boolean $$sFLAG = false;

	@Description("Write MODFLOW (USGS) ASCII array")
	@In
	public boolean $$mFLAG = false;

	@Description("Force output of integer values")
	@In
	public boolean $$iFLAG = false;

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
