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

@Description("Reports statistics for raster map layers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, statistics")
@Label("Grass Raster Modules")
@Name("r__report")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__report {

	@UI("infile,grassfile")
	@Description("Raster map(s) to report on")
	@In
	public String $$mapPARAMETER;

	@Description("Units (optional)")
	@In
	public String $$unitsPARAMETER;

	@Description("Character representing no data cell value (optional)")
	@In
	public String $$nullPARAMETER = "*";

	@Description("Page length (default: 0 lines) (optional)")
	@In
	public String $$plPARAMETER;

	@Description("Page width (default: 79 characters) (optional)")
	@In
	public String $$pwPARAMETER;

	@Description("Name of an output file to hold the report (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Number of fp subranges to collect stats from (optional)")
	@In
	public String $$nstepsPARAMETER = "255";

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

	@Description("Suppress page headers")
	@In
	public boolean $$hFLAG = false;

	@Description("Use formfeeds between pages")
	@In
	public boolean $$fFLAG = false;

	@Description("Scientific format")
	@In
	public boolean $$eFLAG = false;

	@Description("Filter out all no data cells")
	@In
	public boolean $$nFLAG = false;

	@Description("Filter out cells where all maps have no data")
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
