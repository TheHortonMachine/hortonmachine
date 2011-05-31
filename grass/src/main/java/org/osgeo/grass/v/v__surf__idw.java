package org.osgeo.grass.v;

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

@Description("Surface interpolation from vector point data by Inverse Distance Squared Weighting.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, interpolation")
@Label("Grass/Vector Modules")
@Name("v__surf__idw")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__surf__idw {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Number of interpolation points (optional)")
	@In
	public String $$npointsPARAMETER = "12";

	@Description("Power parameter; greater values assign greater influence to closer points (optional)")
	@In
	public String $$powerPARAMETER = "2.0";

	@Description("If set to 0, z coordinates are used (3D vector only) (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Required if layer > 0 (optional)")
	@In
	public String $$columnPARAMETER;

	@Description("Slower but uses less memory and includes points from outside region in the interpolation")
	@In
	public boolean $$nFLAG = false;

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
