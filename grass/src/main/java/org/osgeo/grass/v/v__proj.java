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

@Description("Allows projection conversion of vector maps.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, projection")
@Label("Grass Vector Modules")
@Name("v__proj")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__proj {

	@Description("Name of input vector map (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Location containing input vector map")
	@In
	public String $$locationPARAMETER;

	@Description("Mapset containing input vector map (optional)")
	@In
	public String $$mapsetPARAMETER;

	@Description("Path to GRASS database of input location (optional)")
	@In
	public String $$dbasePARAMETER;

	@UI("outfile")
	@Description("Name for output vector map (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("List vector maps in input location and exit")
	@In
	public boolean $$lFLAG = false;

	@Description("3D vector maps only")
	@In
	public boolean $$zFLAG = false;

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
