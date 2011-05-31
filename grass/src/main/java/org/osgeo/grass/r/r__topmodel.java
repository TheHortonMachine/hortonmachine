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

@Description("Simulates TOPMODEL which is a physically based hydrologic model.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__topmodel")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__topmodel {

	@UI("infile,grassfile")
	@Description("(i)   Basin map created by r.water.outlet (MASK) (optional)")
	@In
	public String $$basinPARAMETER;

	@UI("infile,grassfile")
	@Description("(i)   Elevation map (optional)")
	@In
	public String $$elevationPARAMETER;

	@UI("outfile,grassfile")
	@Description("(o)   Depressionless elevation map (optional)")
	@In
	public String $$depressionlessPARAMETER;

	@UI("outfile,grassfile")
	@Description("(o)   Direction map for depressionless elevation map (optional)")
	@In
	public String $$directionPARAMETER;

	@UI("outfile,grassfile")
	@Description("(o/i) Basin elevation map (MASK applied) (optional)")
	@In
	public String $$belevationPARAMETER;

	@UI("outfile,grassfile")
	@Description("(o)   Topographic index ln(a/tanB) map (MASK applied) (optional)")
	@In
	public String $$topidxPARAMETER;

	@Description("(i)   Number of topographic index classes (optional)")
	@In
	public String $$nidxclassPARAMETER = "30";

	@Description("(o/i) Topographic index statistics file")
	@In
	public String $$idxstatsPARAMETER;

	@Description("(i)   TOPMODEL Parameters file")
	@In
	public String $$parametersPARAMETER;

	@Description("(i)   Rainfall and potential evapotranspiration data file")
	@In
	public String $$inputPARAMETER;

	@Description("(o)   Output file")
	@In
	public String $$outputPARAMETER;

	@Description("(i)   OPTIONAL Observed flow file (optional)")
	@In
	public String $$QobsPARAMETER;

	@Description("(i)   OPTIONAL Output for given time step (optional)")
	@In
	public String $$timestepPARAMETER;

	@Description("(i)   OPTIONAL Output for given topographic index class (optional)")
	@In
	public String $$idxclassPARAMETER;

	@Description("Input data given for (o/i)")
	@In
	public boolean $$iFLAG = false;

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
