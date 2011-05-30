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

@Description("Allows the user to interactively query a vector map layer at user-selected locations within the current geographic region.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__what__vect")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__what__vect {

	@UI("infile")
	@Description("Name of existing vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Identify just one location")
	@In
	public boolean $$1FLAG = false;

	@Description("Terse output. For parsing by programs")
	@In
	public boolean $$tFLAG = false;

	@Description("Print information as plain text to terminal window")
	@In
	public boolean $$xFLAG = false;

	@Description("Print topological information (debugging)")
	@In
	public boolean $$dFLAG = false;

	@Description("Enable flashing (slower)")
	@In
	public boolean $$fFLAG = false;

	@Description("Open form in edit mode")
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
