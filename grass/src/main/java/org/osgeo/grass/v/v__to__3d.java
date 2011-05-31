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

@Description("Performs transformation of 2D vector features to 3D.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, transformation, 3D")
@Label("Grass/Vector Modules")
@Name("v__to__3d")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__to__3d {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid";

	@Description("Fixed height for 3D vector features (optional)")
	@In
	public String $$heightPARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Can be used for reverse transformation, to store height of points (optional)")
	@In
	public String $$columnPARAMETER;

	@Description("Reverse transformation; 3D vector features to 2D")
	@In
	public boolean $$rFLAG = false;

	@Description("Do not copy table")
	@In
	public boolean $$tFLAG = false;

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
