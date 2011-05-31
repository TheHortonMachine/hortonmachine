package org.osgeo.grass.i;

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

@Description("The resulting signature file can be used as input for i.maxlik or as a seed signature file for i.cluster.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery")
@Label("Grass Imagery Modules")
@Name("i__class")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__class {

	@UI("infile,grassfile")
	@Description("Name of raster map to be displayed")
	@In
	public String $$mapPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input imagery group")
	@In
	public String $$groupPARAMETER;

	@Description("Name of input imagery subgroup")
	@In
	public String $$subgroupPARAMETER;

	@Description("File to contain result signatures")
	@In
	public String $$outsigPARAMETER;

	@Description("File containing input signatures (seed) (optional)")
	@In
	public String $$insigPARAMETER;

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
