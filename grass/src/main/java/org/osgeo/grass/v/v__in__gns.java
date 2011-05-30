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

@Description("Imports US-NGA GEOnet Names Server (GNS) country files into a GRASS vector points map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, import, gazetteer")
@Label("Grass Vector Modules")
@Name("v__in__gns")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__gns {

	@Description("Uncompressed GNS file from NGA (with .txt extension)")
	@In
	public String $$filePARAMETER;

	@UI("outfile")
	@Description("Name for output vector map (optional)")
	@In
	public String $$vectPARAMETER;

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
