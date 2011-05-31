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

@Description("Explicitly create the 3D NULL-value bitmap file.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, voxel")
@Label("Grass Raster 3D Modules")
@Name("r3__null")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__null {

	@UI("infile,grassfile")
	@Description("3d raster map for which to modify null values")
	@In
	public String $$mapPARAMETER;

	@Description("List of cell values to be set to NULL (optional)")
	@In
	public String $$setnullPARAMETER;

	@Description("The value to replace the null value by (optional)")
	@In
	public String $$nullPARAMETER;

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
