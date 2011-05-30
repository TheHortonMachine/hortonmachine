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

@Description("Selects the font in which text will be displayed on the user's graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__font")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__font {

	@Description("Choose new current font (optional)")
	@In
	public String $$fontPARAMETER = "romans";

	@Description("Path to Freetype-compatible font including file name (optional)")
	@In
	public String $$pathPARAMETER;

	@Description("Character encoding (optional)")
	@In
	public String $$charsetPARAMETER = "UTF-8";

	@Description("List fonts")
	@In
	public boolean $$lFLAG = false;

	@Description("List fonts verbosely")
	@In
	public boolean $$LFLAG = false;

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
