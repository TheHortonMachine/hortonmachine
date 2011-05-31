package org.osgeo.grass.g;

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

@Description("Computes a coordinate transformation based on the control points.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("general, transformation, GCP")
@Label("Grass General Modules")
@Name("g__transform")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class g__transform {

	@UI("infile,grassfile")
	@Description("Name of input imagery group")
	@In
	public String $$groupPARAMETER;

	@Description("Rectification polynomial order")
	@In
	public String $$orderPARAMETER;

	@Description("Output format (optional)")
	@In
	public String $$formatPARAMETER = "fd,rd";

	@Description("Display summary information")
	@In
	public boolean $$sFLAG = false;

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
