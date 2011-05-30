package org.osgeo.grass.g;

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

@Description("Prompts the user for the names of GRASS data base files.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general")
@Label("Grass General Modules")
@Name("g__ask")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__ask {

	@Description("The type of query")
	@In
	public String $$typePARAMETER;

	@Description("The prompt to be displayed to the user (optional)")
	@In
	public String $$promptPARAMETER;

	@Description("The database element to be queried")
	@In
	public String $$elementPARAMETER;

	@Description("A short description of the database element (optional)")
	@In
	public String $$descPARAMETER;

	@Description("The name of a unix file to store the user's response")
	@In
	public String $$unixfilePARAMETER;

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
