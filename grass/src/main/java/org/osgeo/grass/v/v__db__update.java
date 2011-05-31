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

@Description("Allows to update a column in the attribute table connected to a vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, database, attribute table")
@Label("Grass Vector Modules")
@Name("v__db__update")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__db__update {

	@UI("infile,grassfile")
	@Description("Vector map to edit the attribute table for")
	@In
	public String $$mapPARAMETER;

	@Description("Layer to which the table to be changed is connected (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Column to update")
	@In
	public String $$columnPARAMETER;

	@Description("Value to update the column with (varchar values have to be in single quotes, e.g. 'grass') (optional)")
	@In
	public String $$valuePARAMETER;

	@Description("Column to query (optional)")
	@In
	public String $$qcolumnPARAMETER;

	@Description("WHERE conditions for update, without 'where' keyword (e.g. cat=1 or col1/col2>1) (optional)")
	@In
	public String $$wherePARAMETER;

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
