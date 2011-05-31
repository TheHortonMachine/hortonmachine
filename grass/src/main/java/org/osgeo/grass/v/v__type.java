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

@Description("Change the type of geometry elements.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, geometry")
@Label("Grass Vector Modules")
@Name("v__type")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__type {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("<input_type1>,<output_type1>,<input_type2>,<output_type2>,... 		Example1: line,boundary 		Example2: line,boundary,point,centroid (optional)")
	@In
	public String $$typePARAMETER = "line,boundary,point,centroid";

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
