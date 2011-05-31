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

@Description("Calculates cast shadow areas from sun position and DEM. Either A: exact sun position is specified, or B: date/time to calculate the sun position by r.sunmask itself.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__sunmask")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__sunmask {

	@UI("infile,grassfile")
	@Description("Name of elevation raster map")
	@In
	public String $$elevPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output raster map having shadows")
	@In
	public String $$outputPARAMETER;

	@Description("A: altitude of the sun above horizon, degrees (optional)")
	@In
	public String $$altitudePARAMETER;

	@Description("A: azimuth of the sun from the north, degrees (optional)")
	@In
	public String $$azimuthPARAMETER;

	@Description("B: year (1950..2050) (optional)")
	@In
	public String $$yearPARAMETER;

	@Description("B: month (0..12) (optional)")
	@In
	public String $$monthPARAMETER;

	@Description("B: day (0..31) (optional)")
	@In
	public String $$dayPARAMETER;

	@Description("B: hour (0..24) (optional)")
	@In
	public String $$hourPARAMETER;

	@Description("B: minutes (0..60) (optional)")
	@In
	public String $$minutePARAMETER;

	@Description("B: seconds (0..60) (optional)")
	@In
	public String $$secondPARAMETER;

	@Description("B: timezone (east positive, offset from GMT, also use to adjust daylight savings) (optional)")
	@In
	public String $$timezonePARAMETER;

	@Description("East coordinate (point of interest, default: map center) (optional)")
	@In
	public String $$eastPARAMETER;

	@Description("North coordinate (point of interest, default: map center) (optional)")
	@In
	public String $$northPARAMETER;

	@Description("Zero is a real elevation")
	@In
	public boolean $$zFLAG = false;

	@Description("Verbose output (also print out sun position etc.)")
	@In
	public boolean $$vFLAG = false;

	@Description("Calculate sun position only and exit")
	@In
	public boolean $$sFLAG = false;

	@Description("Print the sun position output in shell script style")
	@In
	public boolean $$gFLAG = false;

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
