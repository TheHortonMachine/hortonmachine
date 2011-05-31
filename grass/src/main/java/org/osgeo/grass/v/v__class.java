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

@Description("Classifies attribute data, e.g. for thematic mapping")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, statistics")
@Label("Grass Vector Modules")
@Name("v__class")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__class {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Column name or expression")
	@In
	public String $$columnPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Algorithm to use for classification")
	@In
	public String $$algorithmPARAMETER;

	@Description("Number of classes to define")
	@In
	public String $$nbclassesPARAMETER;

	@Description("Print only class breaks (without min and max)")
	@In
	public boolean $$gFLAG = false;

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
