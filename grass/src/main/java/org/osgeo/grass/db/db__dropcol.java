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

@Description("Drops a column from selected attribute table")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("database, attribute table")
@Label("Grass Database Modules")
@Name("db__dropcol")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class db__dropcol {

	@Description("Table from which to drop attribute column")
	@In
	public String $$tablePARAMETER;

	@Description("Name of the column")
	@In
	public String $$columnPARAMETER;

	@Description("Force removal (required for actual deletion of files)")
	@In
	public boolean $$fFLAG = false;

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
