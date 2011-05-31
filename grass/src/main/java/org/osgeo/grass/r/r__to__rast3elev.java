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

@Description("Creates a 3D volume map based on 2D elevation and value raster maps.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, raster3d, voxel, conversion")
@Label("Grass Raster Modules")
@Name("r__to__rast3elev")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__to__rast3elev {

	@UI("infile,grassfile")
	@Description("Name of input raster map(s)")
	@In
	public String $$inputPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of elevation raster map(s)")
	@In
	public String $$elevationPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster3d map")
	@In
	public String $$outputPARAMETER;

	@Description("The value to fill the upper cells, default is null (optional)")
	@In
	public String $$upperPARAMETER;

	@Description("The value to fill the lower cells, default is null (optional)")
	@In
	public String $$lowerPARAMETER;

	@Description("Use the input map values to fill the upper cells")
	@In
	public boolean $$uFLAG = false;

	@Description("Use the input map values to fill the lower cells")
	@In
	public boolean $$lFLAG = false;

	@Description("Use G3D mask (if exists) with input map")
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
