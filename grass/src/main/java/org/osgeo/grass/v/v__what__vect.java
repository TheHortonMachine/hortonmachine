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

@Description("Uploads vector values at positions of vector points to the table.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, database, attribute table")
@Label("Grass Vector Modules")
@Name("v__what__vect")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__what__vect {

	@UI("infile")
	@Description("Vector map to modify")
	@In
	public String $$vectorPARAMETER;

	@Description("Layer in the vector to be modified (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Column to be updated with the query result")
	@In
	public String $$columnPARAMETER;

	@UI("infile")
	@Description("Vector map to be queried")
	@In
	public String $$qvectorPARAMETER;

	@Description("Layer of the query vector containing data (optional)")
	@In
	public String $$qlayerPARAMETER = "1";

	@Description("Column to be queried")
	@In
	public String $$qcolumnPARAMETER;

	@Description("Maximum query distance in map units (optional)")
	@In
	public String $$dmaxPARAMETER = "0.0";

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
