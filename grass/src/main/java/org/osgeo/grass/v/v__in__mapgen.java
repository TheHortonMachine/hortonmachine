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

@Description("Import Mapgen or Matlab vector maps into GRASS.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, import")
@Label("Grass Vector Modules")
@Name("v__in__mapgen")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__in__mapgen {

	@Description("Name of input file in Mapgen/Matlab format")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Name for output vector map (omit for display to stdout) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Input map is in Matlab format")
	@In
	public boolean $$fFLAG = false;

	@Description("Create a 3D vector points map from 3 column Matlab data")
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
