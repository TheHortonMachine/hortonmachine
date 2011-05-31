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

@Description("Converts ASCII raster file to binary raster map layer.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, import, conversion")
@Label("Grass Raster Modules")
@Name("r__in__ascii")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__in__ascii {

	@Description("ASCII raster file to be imported. If not given reads from standard input (optional)")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Title for resultant raster map (optional)")
	@In
	public String $$titlePARAMETER;

	@Description("Multiplier for ASCII data (optional)")
	@In
	public String $$multPARAMETER = "1.0 or read from header";

	@Description("String representing NULL value data cell (optional)")
	@In
	public String $$nvPARAMETER = "* or read from header";

	@Description("Integer values are imported")
	@In
	public boolean $$iFLAG = false;

	@Description("Floating point values are imported")
	@In
	public boolean $$fFLAG = false;

	@Description("Double floating point values are imported")
	@In
	public boolean $$dFLAG = false;

	@Description("SURFER (Golden Software) ASCII file will be imported")
	@In
	public boolean $$sFLAG = false;

	@Description("Allow output files to overwrite existing files")
	@In
	public boolean $$overwriteFLAG = false;

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
