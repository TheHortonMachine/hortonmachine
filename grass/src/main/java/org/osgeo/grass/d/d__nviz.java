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

@Description("Creates fly-through script to run in NVIZ.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__nviz")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__nviz {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@Description("Name of output script")
	@In
	public String $$outputPARAMETER;

	@Description("Prefix of output images (default = NVIZ) (optional)")
	@In
	public String $$namePARAMETER;

	@Description("Route coordinates (east,north) (optional)")
	@In
	public String $$routePARAMETER;

	@Description("Camera layback distance (in map units)")
	@In
	public String $$distPARAMETER;

	@Description("Camera height above terrain")
	@In
	public String $$htPARAMETER;

	@Description("Number of frames")
	@In
	public String $$framesPARAMETER;

	@Description("Start frame number (default=0) (optional)")
	@In
	public String $$startPARAMETER;

	@Description("Interactively select route")
	@In
	public boolean $$iFLAG = false;

	@Description("Full render -- Save images")
	@In
	public boolean $$fFLAG = false;

	@Description("Fly at constant elevation (ht)")
	@In
	public boolean $$cFLAG = false;

	@Description("Include command in the script to output a KeyFrame file")
	@In
	public boolean $$kFLAG = false;

	@Description("Render images off-screen")
	@In
	public boolean $$oFLAG = false;

	@Description("Enable vector and sites drawing")
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
