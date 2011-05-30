package org.osgeo.grass.d;

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

@Description("Interactive profile plotting utility with optional output.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__profile")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__profile {

	@UI("infile")
	@Description("Raster map to be profiled")
	@In
	public String $$rastPARAMETER;

	@UI("infile")
	@Description("Optional display raster (optional)")
	@In
	public String $$drastPARAMETER;

	@Description("Output profile data to file(s) with prefix 'name' (optional)")
	@In
	public String $$plotfilePARAMETER;

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
