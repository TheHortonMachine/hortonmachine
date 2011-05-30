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

@Description("Converts raster maps into the VTK-Ascii format")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__out__vtk")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__out__vtk {

	@UI("infile")
	@Description("Name of input raster map(s)")
	@In
	public String $$inputPARAMETER;

	@Description("Name for VTK-ASCII output file (optional)")
	@In
	public String $$outputPARAMETER;

	@UI("infile")
	@Description("Elevation raster map (optional)")
	@In
	public String $$elevationPARAMETER;

	@Description("Value to represent no data cell (optional)")
	@In
	public String $$nullPARAMETER = "-10.0";

	@Description("Elevation (if no elevation map is specified) (optional)")
	@In
	public String $$elevation2dPARAMETER = "0.0";

	@UI("infile")
	@Description("Three (r,g,b) raster maps to create rgb values [redmap,greenmap,bluemap] (optional)")
	@In
	public String $$rgbmapsPARAMETER;

	@UI("infile")
	@Description("Three (x,y,z) raster maps to create vector values [xmap,ymap,zmap] (optional)")
	@In
	public String $$vectormapsPARAMETER;

	@Description("Scale factor for elevation (optional)")
	@In
	public String $$elevscalePARAMETER = "1.0";

	@Description("Number of significant digits (floating point only) (optional)")
	@In
	public String $$dpPARAMETER = "12";

	@Description("Create VTK point data instead of VTK cell data (if no elevation map is given)")
	@In
	public boolean $$pFLAG = false;

	@Description("Use structured grid for elevation (not recommended)")
	@In
	public boolean $$sFLAG = false;

	@Description("Use polydata-trianglestrips for elevation grid creation")
	@In
	public boolean $$tFLAG = false;

	@Description("Use polydata-vertices for elevation grid creation (to use with vtkDelauny2D)")
	@In
	public boolean $$vFLAG = false;

	@Description("Scale factor effects the origin (if no elevation map is given)")
	@In
	public boolean $$oFLAG = false;

	@Description("Correct the coordinates to fit the VTK-OpenGL precision")
	@In
	public boolean $$cFLAG = false;

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
