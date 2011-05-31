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

@Description("Allows the user to change the current geographic region settings interactively, with a mouse.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass Display Modules")
@Name("d__zoom")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__zoom {

	@UI("infile,grassfile")
	@Description("Name of raster map")
	@In
	public String $$rastPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of vector map (optional)")
	@In
	public String $$vectorPARAMETER;

	@Description("Magnification: >1.0 zooms in, <1.0 zooms out (optional)")
	@In
	public String $$zoomPARAMETER = "0.75";

	@Description("Full menu (zoom + pan) & Quit menu")
	@In
	public boolean $$fFLAG = false;

	@Description("Pan mode")
	@In
	public boolean $$pFLAG = false;

	@Description("Handheld mode")
	@In
	public boolean $$hFLAG = false;

	@Description("Just redraw given maps using default colors")
	@In
	public boolean $$jFLAG = false;

	@Description("Return to previous zoom")
	@In
	public boolean $$rFLAG = false;

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
