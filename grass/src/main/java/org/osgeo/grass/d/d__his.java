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

@Description("Displays the result obtained by combining hue, intensity, and saturation (his) values from user-specified input raster map layers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__his")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__his {

	@UI("infile,grassfile")
	@Description("Name of layer to be used for HUE")
	@In
	public String $$h_mapPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of layer to be used for INTENSITY (optional)")
	@In
	public String $$i_mapPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of layer to be used for SATURATION (optional)")
	@In
	public String $$s_mapPARAMETER;

	@Description("Percent to brighten intensity channel (optional)")
	@In
	public String $$brightenPARAMETER = "0";

	@Description("Respect NULL values while drawing")
	@In
	public boolean $$nFLAG = false;

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
