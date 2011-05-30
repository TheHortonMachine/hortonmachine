package org.osgeo.grass.d;

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

@Description("Displays text labels (created with v.label) to the active frame on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__labels")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__labels {

	@UI("infile")
	@Description("Name of label file")
	@In
	public String $$labelsPARAMETER;

	@Description("Minimum region size (diagonal) when labels are displayed (optional)")
	@In
	public String $$minregPARAMETER;

	@Description("Maximum region size (diagonal) when labels are displayed (optional)")
	@In
	public String $$maxregPARAMETER;

	@Description("Ignore rotation setting and draw horizontally")
	@In
	public boolean $$iFLAG = false;

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
