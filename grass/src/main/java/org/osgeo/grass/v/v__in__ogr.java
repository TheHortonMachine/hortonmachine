package org.osgeo.grass.v;

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

@Description("Convert OGR vector layers to GRASS vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, import")
@Label("Grass Vector Modules")
@Name("v__in__ogr")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__ogr {

	@Description("Examples: 		ESRI Shapefile: directory containing shapefiles 		MapInfo File: directory containing mapinfo files (optional)")
	@In
	public String $$dsnPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Examples: 		ESRI Shapefile: shapefile name 		MapInfo File: mapinfo file name (optional)")
	@In
	public String $$layerPARAMETER;

	@Description("Format: xmin,ymin,xmax,ymax - usually W,S,E,N (optional)")
	@In
	public String $$spatialPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Smaller areas and islands are ignored. Should be greater than snap^2 (optional)")
	@In
	public String $$min_areaPARAMETER = "0.0001";

	@Description("Optionally change default input type (optional)")
	@In
	public String $$typePARAMETER = "";

	@Description("'-1' for no snap (optional)")
	@In
	public String $$snapPARAMETER = "-1";

	@Description("Name for new location to create (optional)")
	@In
	public String $$locationPARAMETER;

	@Description("List of column names to be used instead of original names, first is used for category column (optional)")
	@In
	public String $$cnamesPARAMETER;

	@Description("List available layers in data source and exit")
	@In
	public boolean $$lFLAG = false;

	@Description("List supported formats and exit")
	@In
	public boolean $$fFLAG = false;

	@Description("Do not clean polygons (not recommended)")
	@In
	public boolean $$cFLAG = false;

	@Description("Create 3D output")
	@In
	public boolean $$zFLAG = false;

	@Description("Do not create attribute table")
	@In
	public boolean $$tFLAG = false;

	@Description("Override dataset projection (use location's projection)")
	@In
	public boolean $$oFLAG = false;

	@Description("Limit import to the current region")
	@In
	public boolean $$rFLAG = false;

	@Description("Extend location extents based on new dataset")
	@In
	public boolean $$eFLAG = false;

	@Description("Change column names to lowercase characters")
	@In
	public boolean $$wFLAG = false;

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
