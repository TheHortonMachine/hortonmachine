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

@Description("Creates parallel line to input vector lines.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, geometry")
@Label("Grass Vector Modules")
@Name("v__parallel")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__parallel {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Offset along major axis in map units")
	@In
	public String $$distancePARAMETER;

	@Description("Offset along minor axis in map units (optional)")
	@In
	public String $$minordistancePARAMETER;

	@Description("Angle of major axis in degrees (optional)")
	@In
	public String $$anglePARAMETER = "0";

	@Description("Side")
	@In
	public String $$sidePARAMETER = "right";

	@Description("Tolerance of arc polylines in map units (optional)")
	@In
	public String $$tolerancePARAMETER;

	@Description("Make outside corners round")
	@In
	public boolean $$rFLAG = false;

	@Description("Create buffer-like parallel lines")
	@In
	public boolean $$bFLAG = false;

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
