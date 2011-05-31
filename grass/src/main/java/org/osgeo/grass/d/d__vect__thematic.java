package org.osgeo.grass.d;

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

@Description("Displays thematic vector map")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, vector, thematic, legend")
@Label("Grass/Display Modules")
@Name("d__vect__thematic")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__vect__thematic {

	@UI("infile,grassfile")
	@Description("Name of vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Feature type")
	@In
	public String $$typePARAMETER = "area";

	@Description("Name of attribute column to use for thematic display (must be numeric)")
	@In
	public String $$columnPARAMETER;

	@Description("Type of thematic display")
	@In
	public String $$themetypePARAMETER = "graduated_colors";

	@Description("Thematic divisions of data for display")
	@In
	public String $$themecalcPARAMETER = "interval";

	@Description("Separate values by spaces (0 10 20 30 ...) (optional)")
	@In
	public String $$breakpointsPARAMETER;

	@Description("Layer number (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Vector point icon for point data (optional)")
	@In
	public String $$iconPARAMETER = "basic/circle";

	@Description("Minimum icon size/line width for graduated points/lines) (optional)")
	@In
	public String $$sizePARAMETER = "5";

	@Description("Maximum icon size/line width for graduated points and lines (optional)")
	@In
	public String $$maxsizePARAMETER = "20";

	@Description("Number of classes for interval theme (integer) (optional)")
	@In
	public String $$nintPARAMETER = "4";

	@Description("Select 'single_color' for graduated point/line display")
	@In
	public String $$colorschemePARAMETER = "blue-red";

	@Description("GRASS named color or R:G:B triplet. Set color scheme to single color (optional)")
	@In
	public String $$pointcolorPARAMETER = "255:0:0";

	@Description("GRASS named color or R:G:B triplet. Set color scheme to single color. (optional)")
	@In
	public String $$linecolorPARAMETER = "0:0:0";

	@Description("Must be expressed as R:G:B triplet (optional)")
	@In
	public String $$startcolorPARAMETER = "255:0:0";

	@Description("Must be expressed as R:G:B triplet (optional)")
	@In
	public String $$endcolorPARAMETER = "0:0:255";

	@Description("Select x11 display monitor for legend (optional)")
	@In
	public String $$monitorPARAMETER = "x1";

	@Description("WHERE conditions of SQL statement without 'where' keyword (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("If not set, no psmap instruction files will be created) (optional)")
	@In
	public String $$psmapPARAMETER;

	@Description("Name of group file where thematic map commands will be saved (optional)")
	@In
	public String $$groupPARAMETER;

	@Description("Save thematic map commands to group file for GIS Manager")
	@In
	public boolean $$gFLAG = false;

	@Description("Create graphic legend in x11 display monitor")
	@In
	public boolean $$lFLAG = false;

	@Description("Only draw fills (no outlines) for areas and points")
	@In
	public boolean $$fFLAG = false;

	@Description("Update color values to GRASSRGB column in attribute table")
	@In
	public boolean $$uFLAG = false;

	@Description("Output legend for GIS Manager (for scripting use only)")
	@In
	public boolean $$sFLAG = false;

	@Description("Use math notation brackets in legend")
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
