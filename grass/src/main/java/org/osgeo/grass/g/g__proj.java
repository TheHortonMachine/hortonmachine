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

@Description("Can also be used to create GRASS locations.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, projection")
@Label("Grass General Modules")
@Name("g__proj")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__proj {

	@Description("Georeferenced data file to read projection information from (optional)")
	@In
	public String $$georefPARAMETER;

	@Description("ASCII file containing a WKT projection description (- for stdin) (optional)")
	@In
	public String $$wktPARAMETER;

	@Description("PROJ.4 projection description (- for stdin) (optional)")
	@In
	public String $$proj4PARAMETER;

	@Description("EPSG projection code (optional)")
	@In
	public String $$epsgPARAMETER;

	@Description("\"0\" for unspecified or \"-1\" to list and exit (optional)")
	@In
	public String $$datumtransPARAMETER = "0";

	@Description("Name of new location to create (optional)")
	@In
	public String $$locationPARAMETER;

	@Description("Print projection information (in conventional GRASS format)")
	@In
	public boolean $$pFLAG = false;

	@Description("Verify datum information and print transformation parameters")
	@In
	public boolean $$dFLAG = false;

	@Description("Print projection information in PROJ.4 format")
	@In
	public boolean $$jFLAG = false;

	@Description("Print projection information in WKT format")
	@In
	public boolean $$wFLAG = false;

	@Description("Use ESRI-style format (applies to WKT output only)")
	@In
	public boolean $$eFLAG = false;

	@Description("Print 'flat' output with no linebreaks (applies to WKT and PROJ.4 output)")
	@In
	public boolean $$fFLAG = false;

	@Description("Force override of datum transformation information in input co-ordinate system")
	@In
	public boolean $$tFLAG = false;

	@Description("Create new projection files (modifies current location unless 'location' option specified)")
	@In
	public boolean $$cFLAG = false;

	@Description("Enable interactive prompting (for command-line use only)")
	@In
	public boolean $$iFLAG = false;

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
