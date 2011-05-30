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

@Description("Creates a GRASS vector map of a user-defined grid.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector")
@Label("Grass Vector Modules")
@Name("v__mkgrid")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__mkgrid {

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$mapPARAMETER;

	@Description("Number of rows and columns in grid")
	@In
	public String $$gridPARAMETER;

	@Description("Where to place the grid (optional)")
	@In
	public String $$positionPARAMETER = "region";

	@Description("Lower left easting and northing coordinates of map (optional)")
	@In
	public String $$coorPARAMETER;

	@Description("Width and height of boxes in grid (optional)")
	@In
	public String $$boxPARAMETER;

	@Description("Angle of rotation (in degrees counter-clockwise) (optional)")
	@In
	public String $$anglePARAMETER = "0";

	@Description("Create grid of points instead of areas and centroids")
	@In
	public boolean $$pFLAG = false;

	@Description("Quiet; No chatter")
	@In
	public boolean $$qFLAG = false;

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
