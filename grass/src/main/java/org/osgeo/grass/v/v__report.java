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

@Description("Reports geometry statistics for vectors.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, report, statistics")
@Label("Grass Vector Modules")
@Name("v__report")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__report {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Layer number (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Value to calculate")
	@In
	public String $$optionPARAMETER;

	@Description("mi(les),f(eet),me(ters),k(ilometers),a(cres),h(ectares),p(ercent) (optional)")
	@In
	public String $$unitsPARAMETER;

	@Description("Reverse sort the result")
	@In
	public boolean $$rFLAG = false;

	@Description("Sort the result")
	@In
	public boolean $$sFLAG = false;

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
