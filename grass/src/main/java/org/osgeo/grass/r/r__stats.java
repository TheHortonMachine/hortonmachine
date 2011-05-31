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

@Description("Generates area statistics for raster map layers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, statistics")
@Label("Grass/Raster Modules")
@Name("r__stats")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__stats {

	@UI("infile,grassfile")
	@Description("Name of input raster map(s)")
	@In
	public String $$inputPARAMETER;

	@Description("Name for output file (if omitted or \"-\" output to stdout) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Output field separator (optional)")
	@In
	public String $$fsPARAMETER = "space";

	@Description("String representing no data cell value (optional)")
	@In
	public String $$nvPARAMETER = "*";

	@Description("Number of fp subranges to collect stats from (optional)")
	@In
	public String $$nstepsPARAMETER = "255";

	@Description("One cell (range) per line")
	@In
	public boolean $$1FLAG = false;

	@Description("Print averaged values instead of intervals")
	@In
	public boolean $$AFLAG = false;

	@Description("Print area totals")
	@In
	public boolean $$aFLAG = false;

	@Description("Print cell counts")
	@In
	public boolean $$cFLAG = false;

	@Description("Print APPROXIMATE percents (total percent may not be 100%)")
	@In
	public boolean $$pFLAG = false;

	@Description("Print category labels")
	@In
	public boolean $$lFLAG = false;

	@Description("Print grid coordinates (east and north)")
	@In
	public boolean $$gFLAG = false;

	@Description("Print x and y (column and row)")
	@In
	public boolean $$xFLAG = false;

	@Description("Print raw indexes of fp ranges (fp maps only)")
	@In
	public boolean $$rFLAG = false;

	@Description("Suppress reporting of any NULLs")
	@In
	public boolean $$nFLAG = false;

	@Description("Suppress reporting of NULLs when all values are NULL")
	@In
	public boolean $$NFLAG = false;

	@Description("Report for cats fp ranges (fp maps only)")
	@In
	public boolean $$CFLAG = false;

	@Description("Read fp map as integer (use map's quant rules)")
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
