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

@Description("Generates three, or four raster map layers showing 1) the base (perpendicular) rate of spread (ROS), 2) the maximum (forward) ROS, 3) the direction of the maximum ROS, and optionally 4) the maximum potential spotting distance.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__ros")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__ros {

	@UI("infile,grassfile")
	@Description("Name of raster map containing fuel MODELs")
	@In
	public String $$modelPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing the 1-HOUR fuel MOISTURE (%) (optional)")
	@In
	public String $$moisture_1hPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing the 10-HOUR fuel MOISTURE (%) (optional)")
	@In
	public String $$moisture_10hPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing the 100-HOUR fuel MOISTURE (%) (optional)")
	@In
	public String $$moisture_100hPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing LIVE fuel MOISTURE (%)")
	@In
	public String $$moisture_livePARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing midflame wind VELOCITYs (ft/min) (optional)")
	@In
	public String $$velocityPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing wind DIRECTIONs (degree) (optional)")
	@In
	public String $$directionPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing SLOPE (degree) (optional)")
	@In
	public String $$slopePARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing ASPECT (degree, anti-clockwise from E) (optional)")
	@In
	public String $$aspectPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing ELEVATION (m) (required w/ -s) (optional)")
	@In
	public String $$elevationPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name of raster map to contain results (several new layers)")
	@In
	public String $$outputPARAMETER;

	@Description("Run verbosely")
	@In
	public boolean $$vFLAG = false;

	@Description("Also produce maximum SPOTTING distance")
	@In
	public boolean $$sFLAG = false;

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
