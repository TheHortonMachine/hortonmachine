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

@Description("Creates raster plane map given dip (inclination), aspect (azimuth) and one point.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, elevation")
@Label("Grass Raster Modules")
@Name("r__plane")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__plane {

	@UI("outfile,grassfile")
	@Description("Name of raster plane to be created")
	@In
	public String $$namePARAMETER;

	@Description("Dip of plane. Value must be between -90 and 90 degrees")
	@In
	public String $$dipPARAMETER = "0.0";

	@Description("Azimuth of the plane. Value must be between 0 and 360 degrees")
	@In
	public String $$azimuthPARAMETER = "0.0";

	@Description("Easting coordinate of a point on the plane")
	@In
	public String $$eastingPARAMETER;

	@Description("Northing coordinate of a point on the plane")
	@In
	public String $$northingPARAMETER;

	@Description("Elevation coordinate of a point on the plane")
	@In
	public String $$elevationPARAMETER;

	@Description("Type of the raster map to be created")
	@In
	public String $$typePARAMETER;

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
