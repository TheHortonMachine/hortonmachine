package org.osgeo.grass.i;

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

@Description("6S - Second Simulation of Satellite Signal in the Solar Spectrum.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery, atmospheric correction")
@Label("Grass/Imagery Modules")
@Name("i__atcorr")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__atcorr {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$iimgPARAMETER;

	@Description("Input imagery range [0,255] (optional)")
	@In
	public String $$isclPARAMETER = "0,255";

	@UI("infile,grassfile")
	@Description("Input altitude raster map in m (optional) (optional)")
	@In
	public String $$ialtPARAMETER = "dem_float";

	@UI("infile,grassfile")
	@Description("Input visibility raster map in km (optional) (optional)")
	@In
	public String $$ivisPARAMETER;

	@Description("Name of input text file")
	@In
	public String $$icndPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$oimgPARAMETER;

	@Description("Rescale output raster map [0,255] (optional)")
	@In
	public String $$osclPARAMETER = "0,255";

	@Description("Output raster is floating point")
	@In
	public boolean $$fFLAG = false;

	@Description("Input map converted to reflectance (default is radiance)")
	@In
	public boolean $$rFLAG = false;

	@Description("Input from ETM+ image taken after July 1, 2000")
	@In
	public boolean $$aFLAG = false;

	@Description("Input from ETM+ image taken before July 1, 2000")
	@In
	public boolean $$bFLAG = false;

	@Description("Try to increase computation speed when categorized altitude or/and visibility map is used")
	@In
	public boolean $$oFLAG = false;

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
