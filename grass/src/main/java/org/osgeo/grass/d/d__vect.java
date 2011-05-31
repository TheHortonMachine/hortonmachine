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

@Description("Displays vector data in the active frame on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display, vector")
@Name("d__vect")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__vect {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Display")
	@In
	public String $$displayPARAMETER = "shape";

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid,area,face";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Example: 1,3,7-9,13 (optional)")
	@In
	public String $$catsPARAMETER;

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$colorPARAMETER = "black";

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$fcolorPARAMETER = "200:200:200";

	@Description("Name of color definition column (for use with -a flag) (optional)")
	@In
	public String $$rgb_columnPARAMETER = "GRASSRGB";

	@Description("Type of color table (for use with -z flag) (optional)")
	@In
	public String $$zcolorPARAMETER = "terrain";

	@Description("Line width (optional)")
	@In
	public String $$widthPARAMETER = "0";

	@Description("Name of column for line widths (these values will be scaled by wscale) (optional)")
	@In
	public String $$wcolumnPARAMETER;

	@Description("Scale factor for wcolumn (optional)")
	@In
	public String $$wscalePARAMETER = "1";

	@Description("Point and centroid symbol (optional)")
	@In
	public String $$iconPARAMETER = "basic/x";

	@Description("Symbol size (optional)")
	@In
	public String $$sizePARAMETER = "5";

	@Description("Layer number for labels (default: the given layer number) (optional)")
	@In
	public String $$llayerPARAMETER = "1";

	@Description("Name of column to be displayed (optional)")
	@In
	public String $$attrcolPARAMETER;

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$lcolorPARAMETER = "red";

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$bgcolorPARAMETER = "none";

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$bcolorPARAMETER = "none";

	@Description("Label size (pixels) (optional)")
	@In
	public String $$lsizePARAMETER = "8";

	@Description("Font name (optional)")
	@In
	public String $$fontPARAMETER;

	@Description("Label horizontal justification (optional)")
	@In
	public String $$xrefPARAMETER = "left";

	@Description("Label vertical justification (optional)")
	@In
	public String $$yrefPARAMETER = "center";

	@Description("Minimum region size (average from height and width) when map is displayed (optional)")
	@In
	public String $$minregPARAMETER;

	@Description("Maximum region size (average from height and width) when map is displayed (optional)")
	@In
	public String $$maxregPARAMETER;

	@Description("Rendering method for filled polygons (optional)")
	@In
	public String $$renderPARAMETER = "c";

	@Description("Run verbosely")
	@In
	public boolean $$vFLAG = false;

	@Description("Get colors from map table column (of form RRR:GGG:BBB)")
	@In
	public boolean $$aFLAG = false;

	@Description("Random colors according to category number (or layer number if 'layer=-1' is given)")
	@In
	public boolean $$cFLAG = false;

	@Description("Use values from 'cats' option as feature id")
	@In
	public boolean $$iFLAG = false;

	@Description("Don't add to list of vectors and commands in monitor (it won't be drawn if the monitor is refreshed)")
	@In
	public boolean $$xFLAG = false;

	@Description("Colorize polygons according to z height")
	@In
	public boolean $$zFLAG = false;

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
