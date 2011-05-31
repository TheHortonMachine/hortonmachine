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

@Description("Creates points/segments from input lines, linear reference system and positions read from stdin or a file.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, LRS, networking")
@Label("Grass Vector Modules")
@Name("v__lrs__segment")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__lrs__segment {

	@UI("infile,grassfile")
	@Description("Input vector map containing lines")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output vector map where segments will be written")
	@In
	public String $$outputPARAMETER;

	@Description("Line layer (optional)")
	@In
	public String $$llayerPARAMETER = "1";

	@Description("Driver name for reference system table (optional)")
	@In
	public String $$rsdriverPARAMETER = "dbf";

	@Description("Database name for reference system table (optional)")
	@In
	public String $$rsdatabasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Name of the reference system table")
	@In
	public String $$rstablePARAMETER;

	@Description("Name of file containing segment rules. If not given, read from stdin. (optional)")
	@In
	public String $$filePARAMETER;

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
