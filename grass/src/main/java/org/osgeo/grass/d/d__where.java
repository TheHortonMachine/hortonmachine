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

@Description("Identifies the geographic coordinates associated with point locations in the active frame on the graphics monitor.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Label("Grass Display Modules")
@Name("d__where")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__where {

	@Description("One mouse click only")
	@In
	public boolean $$1FLAG = false;

	@Description("Output lat/long in decimal degree")
	@In
	public boolean $$dFLAG = false;

	@Description("Output lat/long referenced to current ellipsoid")
	@In
	public boolean $$lFLAG = false;

	@Description("Output lat/long referenced to WGS84 ellipsoid using datum transformation parameters defined in current location (if available)")
	@In
	public boolean $$wFLAG = false;

	@Description("Output frame coordinates of current display monitor (percentage)")
	@In
	public boolean $$fFLAG = false;

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
