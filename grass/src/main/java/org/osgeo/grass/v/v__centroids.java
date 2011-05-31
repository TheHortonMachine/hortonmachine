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

@Description("Adds missing centroids to closed boundaries.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, centroid, area")
@Label("Grass Vector Modules")
@Name("v__centroids")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__centroids {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map")
	@In
	public String $$outputPARAMETER;

	@Description("Action to be taken (optional)")
	@In
	public String $$optionPARAMETER = "add";

	@Description("Layer number (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Category number starting value (optional)")
	@In
	public String $$catPARAMETER = "1";

	@Description("Category increment (optional)")
	@In
	public String $$stepPARAMETER = "1";

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
