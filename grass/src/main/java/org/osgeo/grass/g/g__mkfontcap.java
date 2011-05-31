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

@Description("Generates the font configuration file by scanning various directories for fonts")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general")
@Label("Grass/General Modules")
@Name("g__mkfontcap")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__mkfontcap {

	@Description("Comma-separated list of extra directories to scan for Freetype-compatible fonts as well as the defaults (see documentation) (optional)")
	@In
	public String $$extradirsPARAMETER;

	@Description("Overwrite font configuration file if already existing")
	@In
	public boolean $$oFLAG = false;

	@Description("Write font configuration file to standard output instead of $GISBASE/etc")
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
