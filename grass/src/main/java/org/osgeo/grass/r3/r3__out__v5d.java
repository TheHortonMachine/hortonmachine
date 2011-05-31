package org.osgeo.grass.r3;

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

@Description("Export of GRASS 3D raster map to 3-dimensional Vis5D file.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, voxel")
@Label("Grass Raster 3D Modules")
@Name("r3__out__v5d")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__out__v5d {

	@UI("infile,grassfile")
	@Description("3d raster map to be converted to Vis5d (v5d) file")
	@In
	public String $$inputPARAMETER;

	@Description("Name for v5d output file")
	@In
	public String $$outputPARAMETER;

	@Description("Use map coordinates instead of xyz coordinates")
	@In
	public boolean $$mFLAG = false;

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
