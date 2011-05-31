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

@Description("Recursively traces the least cost path backwards to cells from which the cumulative cost was determined.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__spreadpath")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__spreadpath {

	@UI("infile,grassfile")
	@Description("Name of raster map containing back-path easting information")
	@In
	public String $$x_inputPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing back-path northing information")
	@In
	public String $$y_inputPARAMETER;

	@Description("The map E and N grid coordinates of starting points (optional)")
	@In
	public String $$coordinatePARAMETER;

	@UI("outfile,grassfile")
	@Description("Name of spread path raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Run verbosely")
	@In
	public boolean $$vFLAG = false;

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
