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

@Description("Import waypoints, routes, and tracks from a GPS receiver or GPS download file into a vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, import, GPS")
@Label("Grass/Vector Modules")
@Name("v__in__gpsbabel")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__gpsbabel {

	@Description("Device or file used to import data (optional)")
	@In
	public String $$inputPARAMETER = "/dev/gps";

	@UI("outfile,grassfile")
	@Description("Name for output vector map (omit for display to stdout) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Format of GPS input data (use gpsbabel supported formats) (optional)")
	@In
	public String $$formatPARAMETER = "garmin";

	@Description("Projection of input data (PROJ.4 style), if not set Lat/Lon WGS84 is assumed (optional)")
	@In
	public String $$projPARAMETER;

	@Description("Verbose mode")
	@In
	public boolean $$vFLAG = false;

	@Description("Import waypoints")
	@In
	public boolean $$wFLAG = false;

	@Description("Import routes")
	@In
	public boolean $$rFLAG = false;

	@Description("Import track")
	@In
	public boolean $$tFLAG = false;

	@Description("Force vertices of track or route data as points")
	@In
	public boolean $$pFLAG = false;

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
