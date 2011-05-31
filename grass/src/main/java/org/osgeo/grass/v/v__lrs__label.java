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

@Description("Create stationing from input lines, and linear reference system")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, LRS, networking")
@Label("Grass Vector Modules")
@Name("v__lrs__label")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__lrs__label {

	@UI("infile,grassfile")
	@Description("Input vector map containing lines")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output vector map where stationing will be written")
	@In
	public String $$outputPARAMETER;

	@Description("Line layer (optional)")
	@In
	public String $$llayerPARAMETER = "1";

	@Description("Driver name for reference system table (optional)")
	@In
	public String $$rsdriverPARAMETER = "dbf";

	@Description("Database name for reference system table (optional)")
	@In
	public String $$rsdatabasePARAMETER = "$GISDBASE/$LOCATION_NAME/$MAPSET/dbf/";

	@Description("Name of the reference system table")
	@In
	public String $$rstablePARAMETER;

	@UI("outfile,grassfile")
	@Description("Label file (optional)")
	@In
	public String $$labelsPARAMETER;

	@Description("PM left, MP right, stationing left, stationing right offset (optional)")
	@In
	public String $$offsetPARAMETER = "50,100,25,25";

	@Description("Offset label in label x-direction in map units (optional)")
	@In
	public String $$xoffsetPARAMETER = "25";

	@Description("Offset label in label y-direction in map units (optional)")
	@In
	public String $$yoffsetPARAMETER = "5";

	@Description("Reference position (optional)")
	@In
	public String $$referencePARAMETER = "center";

	@Description("Font (optional)")
	@In
	public String $$fontPARAMETER = "standard";

	@Description("Label size (in map-units) (optional)")
	@In
	public String $$sizePARAMETER = "100";

	@Description("Text color (optional)")
	@In
	public String $$colorPARAMETER = "black";

	@Description("Only for d.label output (optional)")
	@In
	public String $$widthPARAMETER = "1";

	@Description("Only for d.label output (optional)")
	@In
	public String $$hcolorPARAMETER = "none";

	@Description("Only for d.label output (optional)")
	@In
	public String $$hwidthPARAMETER = "0";

	@Description("Background color (optional)")
	@In
	public String $$backgroundPARAMETER = "none";

	@Description("Border color (optional)")
	@In
	public String $$borderPARAMETER = "none";

	@Description("Only relevant if background color is selected (optional)")
	@In
	public String $$opaquePARAMETER = "yes";

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
