package org.osgeo.grass.g;

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

@Description("This module should be used in scripts for messages served to user.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, scripts")
@Label("Grass/General Modules")
@Name("g__message")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__message {

	@Description("Text of the message to be printed")
	@In
	public String $$messagePARAMETER;

	@Description("Level to use for debug messages (optional)")
	@In
	public String $$debugPARAMETER = "1";

	@Description("Print message as warning")
	@In
	public boolean $$wFLAG = false;

	@Description("Print message as fatal error")
	@In
	public boolean $$eFLAG = false;

	@Description("Print message as debug message")
	@In
	public boolean $$dFLAG = false;

	@Description("Print message as progress info")
	@In
	public boolean $$pFLAG = false;

	@Description("Don't print message in quiet mode")
	@In
	public boolean $$iFLAG = false;

	@Description("Print message only in verbose mode")
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
