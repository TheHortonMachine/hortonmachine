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

@Description("Saves the contents of the active display monitor to a graphics file.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, export")
@Name("d__out__file")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__out__file {

	@Description("Name for output file (do NOT add extension)")
	@In
	public String $$outputPARAMETER;

	@Description("Graphics file format")
	@In
	public String $$formatPARAMETER = "png";

	@Description("(same=1, double size=2, quadruple size=4) (optional)")
	@In
	public String $$resolutionPARAMETER = "1";

	@Description("Width and height of output image (overrides resolution setting) (optional)")
	@In
	public String $$sizePARAMETER;

	@Description("(0=none, 1=fastest, 9=most; lossless, only time vs. filesize) (optional)")
	@In
	public String $$compressionPARAMETER = "9";

	@Description("(10=smallest/worst, 100=largest/best) (optional)")
	@In
	public String $$qualityPARAMETER = "75";

	@Description("Paper size for PostScript output (optional)")
	@In
	public String $$paperPARAMETER = "a4";

	@Description("PostScript level (only limits functionality!) (optional)")
	@In
	public String $$ps_levelPARAMETER = "2";

	@Description("In the form of \"NAME=VALUE\", separate multiple entries with a comma. (optional)")
	@In
	public String $$createoptPARAMETER;

	@Description("In the form of \"META-TAG=VALUE\", separate multiple entries with a comma. (optional)")
	@In
	public String $$metaoptPARAMETER;

	@Description("Set background color to black (white default)")
	@In
	public boolean $$bFLAG = false;

	@Description("Set transparent background")
	@In
	public boolean $$tFLAG = false;

	@Description("Use the Cario driver to render images")
	@In
	public boolean $$cFLAG = false;

	@Description("Set paper orientation to landscape (for PostScript output)")
	@In
	public boolean $$rFLAG = false;

	@Description("Do not crop away margins")
	@In
	public boolean $$mFLAG = false;

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
