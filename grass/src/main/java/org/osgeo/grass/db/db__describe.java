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

@Description("Describes a table in detail.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("database, attribute table")
@Label("Grass/Database Modules")
@Name("db__describe")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class db__describe {

	@Description("Table name")
	@In
	public String $$tablePARAMETER;

	@Description("Driver name (optional)")
	@In
	public String $$driverPARAMETER = "dbf";

	@Description("Database name (optional)")
	@In
	public String $$databasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Print column names only instead of full column descriptions")
	@In
	public boolean $$cFLAG = false;

	@Description("Print table structure")
	@In
	public boolean $$tFLAG = false;

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
