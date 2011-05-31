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

@Description("Reconnects vectors to a new database.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, database, attribute table")
@Label("Grass/Vector Modules")
@Name("v__db__reconnect__all")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__db__reconnect__all {

	@Description("The database must be in form printed by v.db.connect -g, i.e. with substituted variables")
	@In
	public String $$old_databasePARAMETER;

	@Description("Name of new database")
	@In
	public String $$new_databasePARAMETER;

	@Description("Old schema (optional)")
	@In
	public String $$old_schemaPARAMETER;

	@Description("New schema (optional)")
	@In
	public String $$new_schemaPARAMETER;

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
