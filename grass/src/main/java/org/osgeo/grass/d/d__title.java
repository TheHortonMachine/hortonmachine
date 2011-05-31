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

@Description("Create a TITLE for a raster map in a form suitable for display with d.text.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__title")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__title {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$mapPARAMETER;

	@Description("Sets the text color (optional)")
	@In
	public String $$colorPARAMETER = "black";

	@Description("Sets the text size as percentage of the frame's height (optional)")
	@In
	public String $$sizePARAMETER = "4.0";

	@Description("Draw title on current display")
	@In
	public boolean $$dFLAG = false;

	@Description("Do a fancier title")
	@In
	public boolean $$fFLAG = false;

	@Description("Do a simple title")
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
