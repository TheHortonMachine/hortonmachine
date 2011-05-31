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

@Description("Either 'from_table' (optionally with 'where') can be used or 'select' option, but not 'from_table' and 'select' at the same time.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("database, attribute table, SQL")
@Label("Grass/Database Modules")
@Name("db__copy")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class db__copy {

	@Description("Input driver name (optional)")
	@In
	public String $$from_driverPARAMETER = "dbf";

	@Description("Input database name (optional)")
	@In
	public String $$from_databasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Input table name (only, if 'select' is not used) (optional)")
	@In
	public String $$from_tablePARAMETER;

	@Description("Output driver name (optional)")
	@In
	public String $$to_driverPARAMETER = "dbf";

	@Description("Output database name (optional)")
	@In
	public String $$to_databasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@UI("outfile,grassfile")
	@Description("Output table name")
	@In
	public String $$to_tablePARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("E.g.: SELECT dedek FROM starobince WHERE obec = 'Frimburg' (optional)")
	@In
	public String $$selectPARAMETER;

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
