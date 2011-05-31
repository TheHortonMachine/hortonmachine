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

@Description("Prints/sets DB connection for a vector map to attribute table.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, database, attribute table")
@Label("Grass/Vector Modules")
@Name("v__db__connect")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__db__connect {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Driver name (optional)")
	@In
	public String $$driverPARAMETER = "dbf";

	@Description("Database name (optional)")
	@In
	public String $$databasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Table name (optional)")
	@In
	public String $$tablePARAMETER;

	@Description("Must refer to an integer column (optional)")
	@In
	public String $$keyPARAMETER = "cat";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Field separator for shell script style output (optional)")
	@In
	public String $$fsPARAMETER = "";

	@Description("Print all map connection parameters and exit")
	@In
	public boolean $$pFLAG = false;

	@Description("Format: layer[/layer name] table key database driver")
	@In
	public boolean $$gFLAG = false;

	@Description("When printing, limit to layer specified by the layer option")
	@In
	public boolean $$lFLAG = false;

	@Description("Print types/names of table columns for specified layer and exit")
	@In
	public boolean $$cFLAG = false;

	@Description("Overwrite connection parameter for certain layer")
	@In
	public boolean $$oFLAG = false;

	@Description("Delete connection for certain layer (not the table)")
	@In
	public boolean $$dFLAG = false;

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
