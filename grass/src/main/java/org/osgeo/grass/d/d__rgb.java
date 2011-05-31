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

@Description("Displays three user-specified raster map layers as red, green, and blue overlays in the active graphics frame.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__rgb")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__rgb {

	@UI("infile,grassfile")
	@Description("Name of raster map to be used for <red>")
	@In
	public String $$redPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map to be used for <green>")
	@In
	public String $$greenPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map to be used for <blue>")
	@In
	public String $$bluePARAMETER;

	@Description("Overlay (non-null values only)")
	@In
	public boolean $$oFLAG = false;

	@Description("Don't add to list of commands in monitor")
	@In
	public boolean $$xFLAG = false;

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
