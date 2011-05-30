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

@Description("Create a new vector map layer by combining other vector map layers.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector")
@Label("Grass Vector Modules")
@Name("v__patch")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__patch {

	@UI("infile")
	@Description("Name of input vector map(s)")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map where bounding boxes of input vector maps are written to (optional)")
	@In
	public String $$bboxPARAMETER;

	@Description("Append files to existing file (overwriting existing files must be activated)")
	@In
	public boolean $$aFLAG = false;

	@Description("Only the table of layer 1 is currently supported")
	@In
	public boolean $$eFLAG = false;

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
