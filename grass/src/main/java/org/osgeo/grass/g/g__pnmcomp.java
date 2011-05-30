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

@Description("Overlays multiple PPM image files")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general")
@Label("Grass General Modules")
@Name("g__pnmcomp")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__pnmcomp {

	@UI("infile")
	@Description("Names of input files")
	@In
	public String $$inputPARAMETER;

	@UI("infile")
	@Description("Names of mask files (optional)")
	@In
	public String $$maskPARAMETER;

	@Description("Layer opacities (optional)")
	@In
	public String $$opacityPARAMETER;

	@Description("Name of output file")
	@In
	public String $$outputPARAMETER;

	@Description("Name of output mask file (optional)")
	@In
	public String $$outmaskPARAMETER;

	@Description("Image width")
	@In
	public String $$widthPARAMETER;

	@Description("Image height")
	@In
	public String $$heightPARAMETER;

	@Description("Background color (optional)")
	@In
	public String $$backgroundPARAMETER;

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
