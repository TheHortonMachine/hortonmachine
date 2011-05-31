package org.osgeo.grass.r;

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

@Description("Computes direct (beam), diffuse and reflected solar irradiation raster maps for given day, latitude, surface and atmospheric conditions. Solar parameters (e.g. sunrise, sunset times, declination, extraterrestrial irradiance, daylight length) are saved in the map history file. Alternatively, a local time can be specified to compute solar incidence angle and/or irradiance raster maps. The shadowing effect of the topography is optionally incorporated.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__sun")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__sun {

	@UI("infile,grassfile")
	@Description("Name of the input elevation raster map [meters]")
	@In
	public String $$elevinPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the input aspect map (terrain aspect or azimuth of the solar panel) [decimal degrees] (optional)")
	@In
	public String $$aspinPARAMETER;

	@Description("A single value of the orientation (aspect), 270 is south (optional)")
	@In
	public String $$aspectPARAMETER = "270";

	@UI("infile,grassfile")
	@Description("Name of the input slope raster map (terrain slope or solar panel inclination) [decimal degrees] (optional)")
	@In
	public String $$slopeinPARAMETER;

	@Description("A single value of inclination (slope) (optional)")
	@In
	public String $$slopePARAMETER = "0.0";

	@UI("infile,grassfile")
	@Description("Name of the Linke atmospheric turbidity coefficient input raster map [-] (optional)")
	@In
	public String $$linkeinPARAMETER;

	@Description("A single value of the Linke atmospheric turbidity coefficient [-] (optional)")
	@In
	public String $$linPARAMETER = "3.0";

	@UI("infile,grassfile")
	@Description("Name of the ground albedo coefficient input raster map [-] (optional)")
	@In
	public String $$albedoPARAMETER;

	@Description("A single value of the ground albedo coefficient [-] (optional)")
	@In
	public String $$albPARAMETER = "0.2";

	@UI("infile,grassfile")
	@Description("Name of the latitudes input raster map [decimal degrees] (optional)")
	@In
	public String $$latinPARAMETER;

	@Description("A single value of latitude [decimal degrees] (optional)")
	@In
	public String $$latPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of the longitude input raster map [decimal degrees] (optional)")
	@In
	public String $$longinPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of real-sky beam radiation coefficient input raster map [-] (optional)")
	@In
	public String $$coefbhPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of real-sky diffuse radiation coefficient input raster map [-] (optional)")
	@In
	public String $$coefdhPARAMETER;

	@UI("infile,grassfile")
	@Description("The horizon information input map prefix (optional)")
	@In
	public String $$horizonPARAMETER;

	@Description("Angle step size for multidirectional horizon [degrees] (optional)")
	@In
	public String $$horizonstepPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output incidence angle raster map (mode 1 only) (optional)")
	@In
	public String $$incidoutPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output beam irradiance [W.m-2] (mode 1) or irradiation raster map [Wh.m-2.day-1] (mode 2) (optional)")
	@In
	public String $$beam_radPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output insolation time raster map [h] (mode 2 only) (optional)")
	@In
	public String $$insol_timePARAMETER;

	@UI("outfile,grassfile")
	@Description("Output diffuse irradiance [W.m-2] (mode 1) or irradiation raster map [Wh.m-2.day-1] (mode 2) (optional)")
	@In
	public String $$diff_radPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output ground reflected irradiance [W.m-2] (mode 1) or irradiation raster map [Wh.m-2.day-1] (mode 2) (optional)")
	@In
	public String $$refl_radPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output global (total) irradiance/irradiation [W.m-2] (mode 1) or irradiance/irradiation raster map [Wh.m-2.day-1] (mode 2) (optional)")
	@In
	public String $$glob_radPARAMETER;

	@Description("No. of day of the year (1-365)")
	@In
	public String $$dayPARAMETER;

	@Description("Time step when computing all-day radiation sums [decimal hours] (optional)")
	@In
	public String $$stepPARAMETER = "0.5";

	@Description("Declination value (overriding the internally computed value) [radians] (optional)")
	@In
	public String $$declinPARAMETER;

	@Description("Local (solar) time (to be set for mode 1 only) [decimal hours] (optional)")
	@In
	public String $$timePARAMETER;

	@Description("Sampling distance step coefficient (0.5-1.5) (optional)")
	@In
	public String $$distPARAMETER = "1.0";

	@Description("Read the input files in this number of chunks (optional)")
	@In
	public String $$numpartitionsPARAMETER = "1";

	@Description("Civil time zone value, if none, the time will be local solar time (optional)")
	@In
	public String $$civiltimePARAMETER;

	@Description("Incorporate the shadowing effect of terrain")
	@In
	public boolean $$sFLAG = false;

	@Description("Use the low-memory version of the program")
	@In
	public boolean $$mFLAG = false;

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
