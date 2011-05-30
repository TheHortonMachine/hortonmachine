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

@Description("Prints/sets general DB connection for current mapset and exits.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("database, attribute table")
@Label("Grass Database Modules")
@Name("db__connect")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class db__connect {

	@Description("Driver name (optional)")
	@In
	public String $$driverPARAMETER = "dbf";

	@Description("Database name (optional)")
	@In
	public String $$databasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Do not use this option if schemas are not supported by driver/database server (optional)")
	@In
	public String $$schemaPARAMETER;

	@Description("Default group of database users to which select privilege is granted (optional)")
	@In
	public String $$groupPARAMETER;

	@Description("Print current connection parameters and exit")
	@In
	public boolean $$pFLAG = false;

	@Description("Check connection parameters, set if uninitialized, and exit")
	@In
	public boolean $$cFLAG = false;

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
