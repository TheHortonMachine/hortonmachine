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

@Description("Finds line id and real km+offset for given points in vector map using linear reference system.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, LRS, networking")
@Label("Grass/Vector Modules")
@Name("v__lrs__where")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__lrs__where {

	@UI("infile,grassfile")
	@Description("Input vector map containing lines")
	@In
	public String $$linesPARAMETER;

	@UI("infile,grassfile")
	@Description("Input vector map containing points")
	@In
	public String $$pointsPARAMETER;

	@Description("Line layer (optional)")
	@In
	public String $$llayerPARAMETER = "1";

	@Description("Point layer (optional)")
	@In
	public String $$playerPARAMETER = "1";

	@Description("Driver name for reference system table (optional)")
	@In
	public String $$rsdriverPARAMETER = "dbf";

	@Description("Database name for reference system table (optional)")
	@In
	public String $$rsdatabasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Name of the reference system table")
	@In
	public String $$rstablePARAMETER;

	@Description("Maximum distance to nearest line (optional)")
	@In
	public String $$threshPARAMETER = "1000";

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
