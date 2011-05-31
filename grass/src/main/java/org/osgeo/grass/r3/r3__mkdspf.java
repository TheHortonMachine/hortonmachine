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

@Description("Creates a display file from an existing grid3 file according to specified threshold levels.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, voxel")
@Label("Grass Raster 3D Modules")
@Name("r3__mkdspf")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__mkdspf {

	@UI("infile,grassfile")
	@Description("Name of an existing 3d raster map")
	@In
	public String $$inputPARAMETER;

	@Description("Name of output display file")
	@In
	public String $$dspfPARAMETER;

	@Description("List of thresholds for isosurfaces (optional)")
	@In
	public String $$levelsPARAMETER;

	@Description("Minimum isosurface level (optional)")
	@In
	public String $$minPARAMETER;

	@Description("Maximum isosurface level (optional)")
	@In
	public String $$maxPARAMETER;

	@Description("Positive increment between isosurface levels (optional)")
	@In
	public String $$stepPARAMETER;

	@Description("Number of isosurface threshold levels (optional)")
	@In
	public String $$tnumPARAMETER = "7";

	@Description("Suppress progress report & min/max information")
	@In
	public boolean $$qFLAG = false;

	@Description("Use flat shading rather than gradient")
	@In
	public boolean $$fFLAG = false;

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
