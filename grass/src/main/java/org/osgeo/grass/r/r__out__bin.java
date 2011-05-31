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

@Description("Exports a GRASS raster to a binary array.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__out__bin")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__bin {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@Description("Name for output binary map (use output=- for stdout) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Value to write out for null (optional)")
	@In
	public String $$nullPARAMETER = "0";

	@Description("Output integer category values, not cell values")
	@In
	public boolean $$iFLAG = false;

	@Description("Export array with GMT compatible header")
	@In
	public boolean $$hFLAG = false;

	@Description("Generate BIL world and header files")
	@In
	public boolean $$bFLAG = false;

	@Description("Byte swap output")
	@In
	public boolean $$sFLAG = false;

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
