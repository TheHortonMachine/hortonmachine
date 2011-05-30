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

@Description("Flow computation for massive grids (Float version).")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__terraflow")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__terraflow {

	@UI("infile")
	@Description("Name of elevation raster map")
	@In
	public String $$elevationPARAMETER;

	@UI("outfile")
	@Description("Output filled (flooded) elevation raster map")
	@In
	public String $$filledPARAMETER;

	@UI("outfile")
	@Description("Output flow direction raster map")
	@In
	public String $$directionPARAMETER;

	@UI("outfile")
	@Description("Output sink-watershed raster map")
	@In
	public String $$swatershedPARAMETER;

	@UI("outfile")
	@Description("Output flow accumulation raster map")
	@In
	public String $$accumulationPARAMETER;

	@UI("outfile")
	@Description("Output topographic convergence index (tci) raster map")
	@In
	public String $$tciPARAMETER;

	@Description("If flow accumulation is larger than this value it is routed using SFD (D8) direction   		 (meaningfull only  for MFD flow) (optional)")
	@In
	public String $$d8cutPARAMETER = "infinity";

	@Description("Maximum runtime memory size (in MB) (optional)")
	@In
	public String $$memoryPARAMETER = "300";

	@Description("Directory to hold temporary files (they can be large) (optional)")
	@In
	public String $$STREAM_DIRPARAMETER = "/var/tmp/";

	@Description("Name of file containing runtime statistics (optional)")
	@In
	public String $$statsPARAMETER = "stats.out";

	@Description("SFD (D8) flow (default is MFD)")
	@In
	public boolean $$sFLAG = false;

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

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
