package org.osgeo.grass.ps;

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

@Description("Hardcopy PostScript map output utility.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("postscript, map, printing")
@Label("Grass")
@Name("ps__map")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class ps__map {

	@Description("File containing mapping instructions (or use input=- to enter from keyboard) (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("PostScript output file (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Scale of the output map, e.g. 1:25000 (default: Auto-sized to fit page) (optional)")
	@In
	public String $$scalePARAMETER;

	@Description("Number of copies to print (optional)")
	@In
	public String $$copiesPARAMETER;

	@Description("Rotate plot 90 degrees")
	@In
	public boolean $$rFLAG = false;

	@Description("List paper formats ( name width height left right top bottom(margin) )")
	@In
	public boolean $$pFLAG = false;

	@Description("Create EPS (Encapsulated PostScript) instead of PostScript file")
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
