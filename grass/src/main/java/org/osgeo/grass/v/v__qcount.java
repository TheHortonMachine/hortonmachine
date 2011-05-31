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

@Description("Indices for quadrat counts of sites lists.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, statistics")
@Label("Grass Vector Modules")
@Name("v__qcount")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__qcount {

	@UI("infile,grassfile")
	@Description("Vector of points defining sample points")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Output quadrant centres, number of points is written as category (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Number of quadrats")
	@In
	public String $$nPARAMETER;

	@Description("Quadrat radius")
	@In
	public String $$rPARAMETER;

	@Description("Print results in shell script style")
	@In
	public boolean $$gFLAG = false;

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

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
