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

@Description("Create points along input lines in new vector with 2 layers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, geometry")
@Label("Grass/Vector Modules")
@Name("v__to__points")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__to__points {

	@UI("infile,grassfile")
	@Description("Input vector map containing lines")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output vector map where points will be written")
	@In
	public String $$outputPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid";

	@Description("Line layer (optional)")
	@In
	public String $$llayerPARAMETER = "1";

	@Description("Maximum distance between points in map units (optional)")
	@In
	public String $$dmaxPARAMETER = "100";

	@Description("Write line nodes")
	@In
	public boolean $$nFLAG = false;

	@Description("Write line vertices")
	@In
	public boolean $$vFLAG = false;

	@Description("Interpolate points between line vertices")
	@In
	public boolean $$iFLAG = false;

	@Description("Do not create attribute table")
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
