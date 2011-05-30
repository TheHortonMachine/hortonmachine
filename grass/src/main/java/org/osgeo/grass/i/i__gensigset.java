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

@Description("Generates statistics for i.smap from raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery, classification, supervised, SMAP")
@Label("Grass Imagery Modules")
@Name("i__gensigset")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__gensigset {

	@UI("infile")
	@Description("Ground truth training map")
	@In
	public String $$trainingmapPARAMETER;

	@UI("infile")
	@Description("Name of input imagery group")
	@In
	public String $$groupPARAMETER;

	@UI("infile")
	@Description("Name of input imagery subgroup")
	@In
	public String $$subgroupPARAMETER;

	@Description("Name for output file containing result signatures")
	@In
	public String $$signaturefilePARAMETER;

	@Description("Maximum number of sub-signatures in any class (optional)")
	@In
	public String $$maxsigPARAMETER = "10";

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
