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

@Description("Draws arrows representing cell aspect direction for a raster map containing aspect data.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__rast__arrow")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__rast__arrow {

	@UI("infile,grassfile")
	@Description("Name of raster aspect map to be displayed (optional)")
	@In
	public String $$mapPARAMETER;

	@Description("Type of existing raster aspect map (optional)")
	@In
	public String $$typePARAMETER = "grass";

	@Description("Color for drawing arrows (optional)")
	@In
	public String $$arrow_colorPARAMETER = "green";

	@Description("Color for drawing grid or \"none\" (optional)")
	@In
	public String $$grid_colorPARAMETER = "gray";

	@Description("Color for drawing X's (Null values) (optional)")
	@In
	public String $$x_colorPARAMETER = "black";

	@Description("Color for showing unknown information (optional)")
	@In
	public String $$unknown_colorPARAMETER = "red";

	@Description("Draw arrow every Nth grid cell (optional)")
	@In
	public String $$skipPARAMETER = "1";

	@UI("infile,grassfile")
	@Description("Raster map containing values used for arrow length (optional)")
	@In
	public String $$magnitude_mapPARAMETER;

	@Description("Scale factor for arrows (magnitude map) (optional)")
	@In
	public String $$scalePARAMETER = "1.0";

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
