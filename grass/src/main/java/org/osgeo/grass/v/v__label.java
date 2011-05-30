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

@Description("Creates paint labels for a vector map from attached attributes.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, paint labels")
@Label("Grass Vector Modules")
@Name("v__label")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__label {

	@Description("If not given the name of the input map is used (optional)")
	@In
	public String $$labelsPARAMETER;

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Name of attribute column to be used for labels")
	@In
	public String $$columnPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,boundary,centroid";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Example: income < 1000 and inhab >= 10000 (optional)")
	@In
	public String $$wherePARAMETER;

	@Description("Offset label in x-direction (optional)")
	@In
	public String $$xoffsetPARAMETER = "0";

	@Description("Offset label in y-direction (optional)")
	@In
	public String $$yoffsetPARAMETER = "0";

	@Description("Reference position (optional)")
	@In
	public String $$referencePARAMETER = "center";

	@Description("Font name (optional)")
	@In
	public String $$fontPARAMETER = "standard";

	@Description("Label size (in map-units) (optional)")
	@In
	public String $$sizePARAMETER = "100";

	@Description("Space between letters for curled labels (in map-units) (optional)")
	@In
	public String $$spacePARAMETER;

	@Description("Label size (in points) (optional)")
	@In
	public String $$fontsizePARAMETER;

	@Description("Either a standard color name or R:G:B triplet (optional)")
	@In
	public String $$colorPARAMETER = "black";

	@Description("Rotation angle (degrees counter-clockwise) (optional)")
	@In
	public String $$rotationPARAMETER = "0";

	@Description("Border width (optional)")
	@In
	public String $$widthPARAMETER = "1";

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$hcolorPARAMETER = "none";

	@Description("Width of highlight coloring (optional)")
	@In
	public String $$hwidthPARAMETER = "0";

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$backgroundPARAMETER = "none";

	@Description("Either a standard GRASS color, R:G:B triplet, or \"none\" (optional)")
	@In
	public String $$borderPARAMETER = "none";

	@Description("Opaque to vector (only relevant if background color is selected) (optional)")
	@In
	public String $$opaquePARAMETER = "yes";

	@Description("Rotate labels to align with lines")
	@In
	public boolean $$aFLAG = false;

	@Description("Curl labels along lines")
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
