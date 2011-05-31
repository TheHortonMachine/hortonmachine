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

@Description("Watershed basin analysis program.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__watershed")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__watershed {

	@UI("infile,grassfile")
	@Description("Input map: elevation on which entire analysis is based")
	@In
	public String $$elevationPARAMETER;

	@UI("infile,grassfile")
	@Description("Input map: locations of real depressions (optional)")
	@In
	public String $$depressionPARAMETER;

	@UI("infile,grassfile")
	@Description("Input map: amount of overland flow per cell (optional)")
	@In
	public String $$flowPARAMETER;

	@UI("infile,grassfile")
	@Description("Input map or value: percent of disturbed land, for USLE (optional)")
	@In
	public String $$disturbed__landPARAMETER;

	@UI("infile,grassfile")
	@Description("Input map: terrain blocking overland surface flow, for USLE (optional)")
	@In
	public String $$blockingPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output map: number of cells that drain through each cell (optional)")
	@In
	public String $$accumulationPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output map: drainage direction (optional)")
	@In
	public String $$drainagePARAMETER;

	@UI("outfile,grassfile")
	@Description("Output map: unique label for each watershed basin (optional)")
	@In
	public String $$basinPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output map: stream segments (optional)")
	@In
	public String $$streamPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output map: each half-basin is given a unique value (optional)")
	@In
	public String $$half__basinPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output map: useful for visual display of results (optional)")
	@In
	public String $$visualPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output map: slope length and steepness (LS) factor for USLE (optional)")
	@In
	public String $$length__slopePARAMETER;

	@UI("outfile,grassfile")
	@Description("Output map: slope steepness (S) factor for USLE (optional)")
	@In
	public String $$slope__steepnessPARAMETER;

	@Description("Input value: minimum size of exterior watershed basin (optional)")
	@In
	public String $$thresholdPARAMETER;

	@Description("Input value: maximum length of surface flow, for USLE (optional)")
	@In
	public String $$max__slope__lengthPARAMETER;

	@Description("1 = most diverging flow, 10 = most converging flow. Recommended: 5 (optional)")
	@In
	public String $$convergencePARAMETER = "5";

	@Description("Maximum memory to be used with -m flag (in MB) (optional)")
	@In
	public String $$memoryPARAMETER = "300";

	@Description("SFD: single flow direction, MFD: multiple flow direction")
	@In
	public boolean $$fFLAG = false;

	@Description("Allow only horizontal and vertical flow of water")
	@In
	public boolean $$4FLAG = false;

	@Description("Only needed if memory requirements exceed available RAM; see manual on how to calculate memory requirements")
	@In
	public boolean $$mFLAG = false;

	@Description("See manual for a detailed description of flow accumulation output")
	@In
	public boolean $$aFLAG = false;

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
