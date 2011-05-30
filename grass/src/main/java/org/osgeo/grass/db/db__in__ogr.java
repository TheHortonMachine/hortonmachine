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

@Description("Imports attribute tables in various formats.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("database, attribute table")
@Label("Grass Database Modules")
@Name("db__in__ogr")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class db__in__ogr {

	@Description("Table file to be imported or DB connection string")
	@In
	public String $$dsnPARAMETER;

	@Description("Table name of SQL DB table (optional)")
	@In
	public String $$db_tablePARAMETER;

	@Description("Name for output table (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Name for auto-generated unique key column (optional)")
	@In
	public String $$keyPARAMETER;

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
