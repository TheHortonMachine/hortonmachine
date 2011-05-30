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

@Description("Create optimally placed labels for vector map(s)")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, paint labels")
@Label("Grass Vector Modules")
@Name("v__label__sa")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__label__sa {

	@UI("infile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,area";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Name of attribute column to be used for labels")
	@In
	public String $$columnPARAMETER;

	@Description("Name for new paint-label file")
	@In
	public String $$labelsPARAMETER;

	@Description("Name of TrueType font (as listed in the fontcap)")
	@In
	public String $$fontPARAMETER;

	@Description("Label size (in map-units) (optional)")
	@In
	public String $$sizePARAMETER = "100";

	@Description("Icon size of point features (in map-units) (optional)")
	@In
	public String $$isizePARAMETER = "10";

	@Description("Character encoding (default: UTF-8) (optional)")
	@In
	public String $$charsetPARAMETER = "UTF-8";

	@Description("Text color (optional)")
	@In
	public String $$colorPARAMETER = "black";

	@Description("Highlight color for text (optional)")
	@In
	public String $$hcolorPARAMETER = "none";

	@Description("Width of highlight coloring (optional)")
	@In
	public String $$hwidthPARAMETER = "0";

	@Description("Background color (optional)")
	@In
	public String $$backgroundPARAMETER = "none";

	@Description("Opaque to vector (only relevant if background color is selected) (optional)")
	@In
	public String $$opaquePARAMETER = "yes";

	@Description("Border color (optional)")
	@In
	public String $$borderPARAMETER = "none";

	@Description("Border width (only for ps.map output) (optional)")
	@In
	public String $$widthPARAMETER = "0";

	@Description("Numeric column to give precedence in case of overlapping labels. The label with a smaller weight is hidden. (optional)")
	@In
	public String $$overlapPARAMETER = "";

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
