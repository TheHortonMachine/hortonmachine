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

@Description("Populates database values from vector features.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, database, attribute table")
@Label("Grass Vector Modules")
@Name("v__to__db")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__to__db {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("For coor valid point/centroid, for length valid line/boundary (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$qlayerPARAMETER = "1";

	@Description("Value to upload")
	@In
	public String $$optionPARAMETER;

	@Description("mi(les),f(eet),me(ters),k(ilometers),a(cres),h(ectares) (optional)")
	@In
	public String $$unitsPARAMETER;

	@Description("Name of attribute column(s) (optional)")
	@In
	public String $$columnsPARAMETER;

	@Description("E.g. 'cat', 'count(*)', 'sum(val)' (optional)")
	@In
	public String $$qcolumnPARAMETER;

	@Description("Print only")
	@In
	public boolean $$pFLAG = false;

	@Description("Only print SQL statements")
	@In
	public boolean $$sFLAG = false;

	@Description("In print mode prints totals for options: length,area,count")
	@In
	public boolean $$cFLAG = false;

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
