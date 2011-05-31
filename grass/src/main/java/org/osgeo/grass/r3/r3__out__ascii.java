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

@Description("Converts a 3D raster map layer into an ASCII text file")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, voxel, export")
@Label("Grass/Raster 3D Modules")
@Name("r3__out__ascii")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__out__ascii {

	@UI("infile,grassfile")
	@Description("3d raster map to be converted to ASCII")
	@In
	public String $$inputPARAMETER;

	@Description("Name for ASCII output file (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Number of decimal places for floats (optional)")
	@In
	public String $$dpPARAMETER = "8";

	@Description("Char string to represent no data cell (optional)")
	@In
	public String $$nullPARAMETER = "*";

	@Description("Suppress printing of header information")
	@In
	public boolean $$hFLAG = false;

	@Description("Use G3D mask (if exists) with input map")
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
