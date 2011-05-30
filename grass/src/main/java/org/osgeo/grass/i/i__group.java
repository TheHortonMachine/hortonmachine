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

@Description("Creates, edits, and lists groups and subgroups of imagery files.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery")
@Label("Grass Imagery Modules")
@Name("i__group")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__group {

	@UI("infile")
	@Description("Name of imagery group")
	@In
	public String $$groupPARAMETER;

	@Description("Name of imagery sub-group (optional)")
	@In
	public String $$subgroupPARAMETER;

	@UI("infile")
	@Description("Name of raster map(s) to include in group (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Remove selected files from specified group")
	@In
	public boolean $$rFLAG = false;

	@Description("List files from specified (sub)group (fancy)")
	@In
	public boolean $$lFLAG = false;

	@Description("List files from specified (sub)group (shell script style)")
	@In
	public boolean $$gFLAG = false;

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
