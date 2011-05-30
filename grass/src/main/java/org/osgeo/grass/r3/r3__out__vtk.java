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

@Description("Converts 3D raster maps (G3D) into the VTK-Ascii format")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster3d, voxel")
@Label("Grass Raster 3D Modules")
@Name("r3__out__vtk")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r3__out__vtk {

	@UI("infile")
	@Description("G3D map(s) to be converted to VTK-ASCII data format (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Name for VTK-ASCII output file (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Float value to represent no data cell/points (optional)")
	@In
	public String $$nullPARAMETER = "-99999.99";

	@UI("infile")
	@Description("top surface 2D raster map (optional)")
	@In
	public String $$topPARAMETER;

	@UI("infile")
	@Description("bottom surface 2D raster map (optional)")
	@In
	public String $$bottomPARAMETER;

	@UI("infile")
	@Description("Three (r,g,b) 3d raster maps to create rgb values [redmap,greenmap,bluemap] (optional)")
	@In
	public String $$rgbmapsPARAMETER;

	@UI("infile")
	@Description("Three (x,y,z) 3d raster maps to create vector values [xmap,ymap,zmap] (optional)")
	@In
	public String $$vectormapsPARAMETER;

	@Description("Scale factor for elevation (optional)")
	@In
	public String $$elevscalePARAMETER = "1.0";

	@Description("Number of significant digits (floating point only) (optional)")
	@In
	public String $$dpPARAMETER = "12";

	@Description("Create VTK pointdata instead of VTK celldata (celldata is default)")
	@In
	public boolean $$pFLAG = false;

	@Description("Create 3d elevation output with a top and a bottom surface, both raster maps are required.")
	@In
	public boolean $$sFLAG = false;

	@Description("Use g3d mask (if exists) with input maps")
	@In
	public boolean $$mFLAG = false;

	@Description("Scale factor effects the origin")
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
