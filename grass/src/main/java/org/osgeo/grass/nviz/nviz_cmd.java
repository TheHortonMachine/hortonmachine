package org.osgeo.grass.nviz;

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

@Description("Experimental NVIZ CLI prototype.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("visualization, raster, vector, raster3d")
@Label("Grass")
@Name("nviz_cmd")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class nviz_cmd {

	@UI("infile,grassfile")
	@Description("Name of raster map(s) for elevation (optional)")
	@In
	public String $$elevation_mapPARAMETER;

	@Description("Elevation value(s) (optional)")
	@In
	public String $$elevation_valuePARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map(s) for color (optional)")
	@In
	public String $$color_mapPARAMETER;

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$color_valuePARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map(s) for mask (optional)")
	@In
	public String $$mask_mapPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map(s) for transparency (optional)")
	@In
	public String $$transparency_mapPARAMETER;

	@Description("Transparency value(s) (optional)")
	@In
	public String $$transparency_valuePARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map(s) for shininess (optional)")
	@In
	public String $$shininess_mapPARAMETER;

	@Description("Shininess value(s) (optional)")
	@In
	public String $$shininess_valuePARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map(s) for emission (optional)")
	@In
	public String $$emission_mapPARAMETER;

	@Description("Emission value(s) (optional)")
	@In
	public String $$emission_valuePARAMETER;

	@Description("Draw mode")
	@In
	public String $$modePARAMETER = "fine";

	@Description("Fine resolution")
	@In
	public String $$resolution_finePARAMETER = "6";

	@Description("Coarse resolution")
	@In
	public String $$resolution_coarsePARAMETER = "9";

	@Description("Draw style")
	@In
	public String $$stylePARAMETER = "surface";

	@Description("Shading")
	@In
	public String $$shadingPARAMETER = "gouraud";

	@Description("Either a standard color name or R:G:B triplet")
	@In
	public String $$wire_colorPARAMETER = "136:136:136";

	@Description("Position")
	@In
	public String $$positionPARAMETER = "0,0,0";

	@UI("infile,grassfile")
	@Description("Name of line vector overlay map(s) (optional)")
	@In
	public String $$vlinePARAMETER;

	@Description("Vector line width (optional)")
	@In
	public String $$vline_widthPARAMETER = "2";

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$vline_colorPARAMETER = "blue";

	@Description("Vector line display mode")
	@In
	public String $$vline_modePARAMETER = "surface";

	@Description("Vector line height (optional)")
	@In
	public String $$vline_heightPARAMETER = "0";

	@Description("Position")
	@In
	public String $$vline_positionPARAMETER = "0,0,0";

	@UI("infile,grassfile")
	@Description("Name of point vector overlay map(s) (optional)")
	@In
	public String $$vpointPARAMETER;

	@Description("Icon size (optional)")
	@In
	public String $$vpoint_sizePARAMETER = "100";

	@Description("Icon width (optional)")
	@In
	public String $$vpoint_widthPARAMETER = "2";

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$vpoint_colorPARAMETER = "blue";

	@Description("Icon marker")
	@In
	public String $$vpoint_markerPARAMETER = "sphere";

	@Description("Position")
	@In
	public String $$vpoint_positionPARAMETER = "0,0,0";

	@UI("infile,grassfile")
	@Description("Name of input raster3d map(s) (optional)")
	@In
	public String $$volumePARAMETER;

	@Description("Volume draw mode")
	@In
	public String $$volume_modePARAMETER = "isosurface";

	@Description("Volume shading")
	@In
	public String $$volume_shadingPARAMETER = "gouraud";

	@Description("Volume position")
	@In
	public String $$volume_positionPARAMETER = "0,0,0";

	@Description("Volume resolution")
	@In
	public String $$volume_resolutionPARAMETER = "3";

	@Description("Isosurface level (optional)")
	@In
	public String $$isosurf_levelPARAMETER;

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$bgcolorPARAMETER = "white";

	@Description("Viewpoint height (in map units) (optional)")
	@In
	public String $$heightPARAMETER;

	@Description("Viewpoint field of view (in degrees) (optional)")
	@In
	public String $$perspectivePARAMETER = "40";

	@Description("Viewpoint twist angle (in degrees) (optional)")
	@In
	public String $$twistPARAMETER = "0";

	@Description("Vertical exaggeration (optional)")
	@In
	public String $$zexagPARAMETER;

	@Description("Name for output file (do not add extension)")
	@In
	public String $$outputPARAMETER;

	@Description("Graphics file format")
	@In
	public String $$formatPARAMETER = "ppm";

	@Description("Width and height of output image")
	@In
	public String $$sizePARAMETER = "640,480";

	@Description("Use draw mode for all loaded surfaces")
	@In
	public boolean $$aFLAG = false;

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
