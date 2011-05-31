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

@Description("Tests for normality for points.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, statistics")
@Label("Grass Vector Modules")
@Name("v__normal")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__normal {

	@UI("infile,grassfile")
	@Description("point vector defining sample points")
	@In
	public String $$mapPARAMETER;

	@Description("Lists of tests (1-15): e.g. 1,3-8,13")
	@In
	public String $$testsPARAMETER;

	@Description("Attribute column")
	@In
	public String $$columnPARAMETER;

	@Description("Use only points in current region")
	@In
	public boolean $$rFLAG = false;

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

	@Description("lognormal")
	@In
	public boolean $$lFLAG = false;

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
