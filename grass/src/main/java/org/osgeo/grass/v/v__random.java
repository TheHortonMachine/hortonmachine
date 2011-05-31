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

@Description("Randomly generate a 2D/3D vector points map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, statistics")
@Label("Grass/Vector Modules")
@Name("v__random")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__random {

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Number of points to be created")
	@In
	public String $$nPARAMETER;

	@Description("Minimum z height (needs -z flag or column name) (optional)")
	@In
	public String $$zminPARAMETER = "0.0";

	@Description("Maximum z height (needs -z flag or column name) (optional)")
	@In
	public String $$zmaxPARAMETER = "0.0";

	@Description("If type is not given then DOUBLE PRECISION is used. Writes Z data to column instead of 3D vector. (optional)")
	@In
	public String $$columnPARAMETER;

	@Description("Create 3D output")
	@In
	public boolean $$zFLAG = false;

	@Description("Use drand48() function instead of rand()")
	@In
	public boolean $$dFLAG = false;

	@Description("Do not build topology")
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
