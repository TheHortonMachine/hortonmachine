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

@Description("Finds shortest path on vector network.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, networking")
@Label("Grass/Vector Modules")
@Name("v__net__path")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__net__path {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Arc type (optional)")
	@In
	public String $$typePARAMETER = "line,boundary";

	@Description("Arc layer (optional)")
	@In
	public String $$alayerPARAMETER = "1";

	@Description("Node layer (optional)")
	@In
	public String $$nlayerPARAMETER = "2";

	@Description("Name of file containing start and end points. If not given, read from stdin (optional)")
	@In
	public String $$filePARAMETER;

	@Description("Arc forward/both direction(s) cost column (optional)")
	@In
	public String $$afcolumnPARAMETER;

	@Description("Arc backward direction cost column (optional)")
	@In
	public String $$abcolumnPARAMETER;

	@Description("Node cost column (optional)")
	@In
	public String $$ncolumnPARAMETER;

	@Description("If start/end are given as coordinates. If start/end point is outside this threshold, the path is not found and error message is printed. To speed up the process, keep this value as low as possible. (optional)")
	@In
	public String $$dmaxPARAMETER = "1000";

	@Description("Use geodesic calculation for longitude-latitude locations")
	@In
	public boolean $$gFLAG = false;

	@Description("Write output as original input segments, not each path as one line.")
	@In
	public boolean $$sFLAG = false;

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
