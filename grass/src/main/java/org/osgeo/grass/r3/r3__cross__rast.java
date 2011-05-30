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

@Description("Creates cross section 2D raster map from 3d raster map based on 2D elevation map")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, voxel")
@Label("Grass Raster 3D Modules")
@Name("r3__cross__rast")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__cross__rast {

	@UI("infile")
	@Description("Input 3D raster map for cross section.")
	@In
	public String $$inputPARAMETER;

	@UI("infile")
	@Description("2D elevation map used to create the cross section map")
	@In
	public String $$elevationPARAMETER;

	@UI("outfile")
	@Description("Resulting cross section 2D raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Use g3d mask (if exists) with input map")
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
