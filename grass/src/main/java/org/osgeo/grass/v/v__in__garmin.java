package org.osgeo.grass.v;

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

@Description("Download waypoints, routes, and tracks from a Garmin GPS receiver into a vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, import, GPS")
@Label("Grass/Vector Modules")
@Name("v__in__garmin")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__garmin {

	@UI("outfile,grassfile")
	@Description("Name for output vector map (omit for display to stdout) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Port Garmin receiver is connected to (optional)")
	@In
	public String $$portPARAMETER = "/dev/gps";

	@Description("Verbose mode")
	@In
	public boolean $$vFLAG = false;

	@Description("Download Waypoints from GPS")
	@In
	public boolean $$wFLAG = false;

	@Description("Download Routes from GPS")
	@In
	public boolean $$rFLAG = false;

	@Description("Download Track from GPS")
	@In
	public boolean $$tFLAG = false;

	@Description("Force import of track or route data as points")
	@In
	public boolean $$pFLAG = false;

	@Description("Use gardump instead of gpstrans as the download program")
	@In
	public boolean $$uFLAG = false;

	@Description("Import track in 3D (gardump only)")
	@In
	public boolean $$zFLAG = false;

	@Description("Do not attempt projection transform from WGS84")
	@In
	public boolean $$kFLAG = false;

	@Description("Allow output files to overwrite existing files")
	@In
	public boolean $$overwriteFLAG = false;

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
