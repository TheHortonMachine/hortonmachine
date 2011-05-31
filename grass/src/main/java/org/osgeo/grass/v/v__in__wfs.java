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

@Description("Import GetFeature from WFS")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Label("Grass Vector Modules")
@Name("v__in__wfs")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__wfs {

	@Description("GetFeature URL starting with http (optional)")
	@In
	public String $$wfsPARAMETER;

	@UI("outfile,grassfile")
	@Description("Vector output map")
	@In
	public String $$outputPARAMETER;

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
