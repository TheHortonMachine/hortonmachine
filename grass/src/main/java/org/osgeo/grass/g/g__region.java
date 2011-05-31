package org.osgeo.grass.g;

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

@Description("Manages the boundary definitions for the geographic region.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general")
@Label("Grass General Modules")
@Name("g__region")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__region {

	@UI("infile,grassfile")
	@Description("Set current region from named region (optional)")
	@In
	public String $$regionPARAMETER;

	@UI("infile,grassfile")
	@Description("Set region to match this raster map (optional)")
	@In
	public String $$rastPARAMETER;

	@UI("infile,grassfile")
	@Description("Set region to match this 3D raster map (both 2D and 3D values) (optional)")
	@In
	public String $$rast3dPARAMETER;

	@UI("infile,grassfile")
	@Description("Set region to match this vector map (optional)")
	@In
	public String $$vectPARAMETER;

	@UI("infile,grassfile")
	@Description("Set region to match this 3dview file (optional)")
	@In
	public String $$3dviewPARAMETER;

	@Description("Value for the northern edge (optional)")
	@In
	public String $$nPARAMETER;

	@Description("Value for the southern edge (optional)")
	@In
	public String $$sPARAMETER;

	@Description("Value for the eastern edge (optional)")
	@In
	public String $$ePARAMETER;

	@Description("Value for the western edge (optional)")
	@In
	public String $$wPARAMETER;

	@Description("Value for the top edge (optional)")
	@In
	public String $$tPARAMETER;

	@Description("Value for the bottom edge (optional)")
	@In
	public String $$bPARAMETER;

	@Description("Number of rows in the new region (optional)")
	@In
	public String $$rowsPARAMETER;

	@Description("Number of columns in the new region (optional)")
	@In
	public String $$colsPARAMETER;

	@Description("Grid resolution 2D (both north-south and east-west) (optional)")
	@In
	public String $$resPARAMETER;

	@Description("3D grid resolution (north-south, east-west and top-bottom) (optional)")
	@In
	public String $$res3PARAMETER;

	@Description("North-south grid resolution 2D (optional)")
	@In
	public String $$nsresPARAMETER;

	@Description("East-west grid resolution 2D (optional)")
	@In
	public String $$ewresPARAMETER;

	@Description("Top-bottom grid resolution 3D (optional)")
	@In
	public String $$tbresPARAMETER;

	@UI("infile,grassfile")
	@Description("Shrink region until it meets non-NULL data from this raster map (optional)")
	@In
	public String $$zoomPARAMETER;

	@UI("infile,grassfile")
	@Description("Adjust region cells to cleanly align with this raster map (optional)")
	@In
	public String $$alignPARAMETER;

	@UI("outfile,grassfile")
	@Description("Save current region settings in named region file (optional)")
	@In
	public String $$savePARAMETER;

	@Description("Set from default region")
	@In
	public boolean $$dFLAG = false;

	@Description("Save as default region")
	@In
	public boolean $$sFLAG = false;

	@Description("Print the current region")
	@In
	public boolean $$pFLAG = false;

	@Description("Print the current region in lat/long using the current ellipsoid/datum")
	@In
	public boolean $$lFLAG = false;

	@Description("Print the current region extent")
	@In
	public boolean $$eFLAG = false;

	@Description("Print the current region map center coordinates")
	@In
	public boolean $$cFLAG = false;

	@Description("Print region resolution in meters (geodesic)")
	@In
	public boolean $$mFLAG = false;

	@Description("The difference between the projection's grid north and true north, measured at the center coordinates of the current region.")
	@In
	public boolean $$nFLAG = false;

	@Description("Print also 3D settings")
	@In
	public boolean $$3FLAG = false;

	@Description("Print the maximum bounding box in lat/long on WGS84")
	@In
	public boolean $$bFLAG = false;

	@Description("Print in shell script style")
	@In
	public boolean $$gFLAG = false;

	@Description("Align region to resolution (default = align to bounds, works only for 2D resolution)")
	@In
	public boolean $$aFLAG = false;

	@Description("Do not update the current region")
	@In
	public boolean $$uFLAG = false;

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
