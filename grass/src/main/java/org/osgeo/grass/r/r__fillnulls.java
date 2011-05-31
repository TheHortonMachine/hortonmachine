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

@Description("Fills no-data areas in raster maps using v.surf.rst splines interpolation")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, elevation, interpolation")
@Label("Grass/Raster Modules")
@Name("r__fillnulls")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__fillnulls {

	@UI("infile,grassfile")
	@Description("Raster map in which to fill nulls")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output raster map with nulls filled by interpolation from surrounding values")
	@In
	public String $$outputPARAMETER;

	@Description("Spline tension parameter (optional)")
	@In
	public String $$tensionPARAMETER = "40.";

	@Description("Spline smoothing parameter (optional)")
	@In
	public String $$smoothPARAMETER = "0.1";

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
