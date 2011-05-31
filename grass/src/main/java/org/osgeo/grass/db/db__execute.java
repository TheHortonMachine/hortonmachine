package org.osgeo.grass.db;

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

@Description("Executes any SQL statement.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("database, attribute table, SQL")
@Label("Grass/Database Modules")
@Name("db__execute")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class db__execute {

	@Description("Name of file containing SQL statements (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Driver name (optional)")
	@In
	public String $$driverPARAMETER = "dbf";

	@Description("Database name (optional)")
	@In
	public String $$databasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Ignore SQL errors and continue")
	@In
	public boolean $$iFLAG = false;

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
