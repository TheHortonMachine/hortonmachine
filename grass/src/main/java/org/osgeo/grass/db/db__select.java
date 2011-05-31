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

@Description("Selects data from table.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("database, attribute table, SQL")
@Label("Grass/Database Modules")
@Name("db__select")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class db__select {

	@Description("Table name (optional)")
	@In
	public String $$tablePARAMETER;

	@Description("Database name (optional)")
	@In
	public String $$databasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Driver name (optional)")
	@In
	public String $$driverPARAMETER = "dbf";

	@Description("For example: 'select * from rybniky where kapri = 'hodne' (optional)")
	@In
	public String $$sqlPARAMETER;

	@Description("Name of file with sql statement (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Output field separator (optional)")
	@In
	public String $$fsPARAMETER = "|";

	@Description("Output vertical record separator (optional)")
	@In
	public String $$vsPARAMETER;

	@Description("Null value indicator (optional)")
	@In
	public String $$nvPARAMETER;

	@Description("Do not include column names in output")
	@In
	public boolean $$cFLAG = false;

	@Description("Describe query only (don't run it)")
	@In
	public boolean $$dFLAG = false;

	@Description("Vertical output (instead of horizontal)")
	@In
	public boolean $$vFLAG = false;

	@Description("Only test query, do not execute")
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
