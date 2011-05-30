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

@Description("Creates new vector (points) map from database table containing coordinates.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, import, database, points")
@Label("Grass Vector Modules")
@Name("v__in__db")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__db {

	@Description("Input table name")
	@In
	public String $$tablePARAMETER;

	@Description("Driver name (optional)")
	@In
	public String $$driverPARAMETER = "dbf";

	@Description("Database name (optional)")
	@In
	public String $$databasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Name of column containing x coordinate")
	@In
	public String $$xPARAMETER;

	@Description("Name of column containing y coordinate")
	@In
	public String $$yPARAMETER;

	@Description("Name of column containing z coordinate (optional)")
	@In
	public String $$zPARAMETER;

	@Description("Must refer to an integer column")
	@In
	public String $$keyPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

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
