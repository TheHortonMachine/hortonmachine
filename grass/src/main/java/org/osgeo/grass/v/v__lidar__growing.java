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

@Description("Building contour determination and Region Growing algorithm for determining the building inside")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, LIDAR")
@Label("Grass Vector Modules")
@Name("v__lidar__growing")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__lidar__growing {

	@UI("infile")
	@Description("Input vector (v.lidar.edgedetection output")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@UI("infile")
	@Description("Name of the first pulse vector map")
	@In
	public String $$firstPARAMETER;

	@Description("Threshold for cell object frequency in region growing (optional)")
	@In
	public String $$tjPARAMETER = "0.2";

	@Description("Threshold for double pulse in region growing (optional)")
	@In
	public String $$tdPARAMETER = "0.6";

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
