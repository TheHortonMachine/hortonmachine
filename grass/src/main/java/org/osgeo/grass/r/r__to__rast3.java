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

@Description("Converts 2D raster map slices to one 3D raster volume map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, volume, conversion")
@Label("Grass Raster Modules")
@Name("r__to__rast3")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__to__rast3 {

	@UI("infile,grassfile")
	@Description("2d raster maps which represent the slices")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster3d map")
	@In
	public String $$outputPARAMETER;

	@Description("Use G3D mask (if exists) with output map")
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
