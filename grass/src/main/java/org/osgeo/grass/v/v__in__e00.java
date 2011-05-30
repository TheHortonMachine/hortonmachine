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

@Description("Import E00 file into a vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, import")
@Label("Grass Vector Modules")
@Name("v__in__e00")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__e00 {

	@Description("E00 file")
	@In
	public String $$filePARAMETER;

	@Description("Input type point, line or area")
	@In
	public String $$typePARAMETER;

	@UI("outfile")
	@Description("Name for output vector map (optional)")
	@In
	public String $$vectPARAMETER;

	@Description("Verbose mode")
	@In
	public boolean $$vFLAG = false;

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
