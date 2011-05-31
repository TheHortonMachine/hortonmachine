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

@Description("Allows the user to interactively query the category contents of multiple raster map layers at user specified locations within the current geographic region.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("display")
@Name("d__what__rast")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class d__what__rast {

	@UI("infile,grassfile")
	@Description("Name of existing raster map(s)")
	@In
	public String $$mapPARAMETER;

	@Description("Field separator (terse mode only) (optional)")
	@In
	public String $$fsPARAMETER = ":";

	@Description("Identify just one location")
	@In
	public boolean $$1FLAG = false;

	@Description("Terse output. For parsing by programs")
	@In
	public boolean $$tFLAG = false;

	@Description("Print out col/row for the entire map in grid resolution of the region")
	@In
	public boolean $$cFLAG = false;

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
