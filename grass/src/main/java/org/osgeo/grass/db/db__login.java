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

@Description("Sets user/password for driver/database.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("database, SQL")
@Label("Grass Database Modules")
@Name("db__login")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class db__login {

	@Description("Driver name")
	@In
	public String $$driverPARAMETER = "dbf";

	@Description("Database name")
	@In
	public String $$databasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Username (optional)")
	@In
	public String $$userPARAMETER;

	@Description("Password (optional)")
	@In
	public String $$passwordPARAMETER;

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
