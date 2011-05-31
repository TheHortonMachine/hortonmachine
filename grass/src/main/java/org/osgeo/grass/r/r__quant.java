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

@Description("Produces the quantization file for a floating-point map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__quant")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__quant {

	@UI("infile,grassfile")
	@Description("Base map to take quant rules from (optional)")
	@In
	public String $$basemapPARAMETER = "NONE";

	@UI("infile,grassfile")
	@Description("Raster map(s) to be quantized")
	@In
	public String $$inputPARAMETER;

	@Description("Floating point range: dmin,dmax")
	@In
	public String $$fprangePARAMETER = "";

	@Description("Integer range: min,max")
	@In
	public String $$rangePARAMETER = "1,255";

	@Description("Truncate floating point data")
	@In
	public boolean $$tFLAG = false;

	@Description("Round floating point data")
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
