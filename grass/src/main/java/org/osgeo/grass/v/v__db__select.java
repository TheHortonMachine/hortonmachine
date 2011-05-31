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

@Description("Prints vector map attributes.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, database, attribute table")
@Label("Grass Vector Modules")
@Name("v__db__select")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__db__select {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Name of attribute column(s) (optional)")
	@In
	public String $$columnsPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Output field separator (optional)")
	@In
	public String $$fsPARAMETER = "|";

	@Description("Output vertical record separator (optional)")
	@In
	public String $$vsPARAMETER;

	@Description("Null value indicator (optional)")
	@In
	public String $$nvPARAMETER;

	@Description("Print minimal region extent of selected vector features instead of attributes")
	@In
	public boolean $$rFLAG = false;

	@Description("Do not include column names in output")
	@In
	public boolean $$cFLAG = false;

	@Description("Vertical output (instead of horizontal)")
	@In
	public boolean $$vFLAG = false;

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
