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

@Description("Calculates univariate statistics on selected table column for a GRASS vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, statistics")
@Label("Grass Vector Modules")
@Name("v__db__univar")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__db__univar {

	@UI("infile")
	@Description("Name of data table")
	@In
	public String $$tablePARAMETER;

	@Description("Column on which to calculate statistics (must be numeric)")
	@In
	public String $$columnPARAMETER;

	@Description("Database/directory for table (optional)")
	@In
	public String $$databasePARAMETER;

	@Description("Database driver (optional)")
	@In
	public String $$driverPARAMETER;

	@Description("WHERE conditions of SQL statement without 'where' keyword (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Extended statistics (quartiles and 90th percentile)")
	@In
	public boolean $$eFLAG = false;

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
