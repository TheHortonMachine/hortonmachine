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

@Description("Outputs basic information about a user-specified vector map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, metadata, history")
@Label("Grass Vector Modules")
@Name("v__info")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__info {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$mapPARAMETER;

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Print vector history instead of info")
	@In
	public boolean $$hFLAG = false;

	@Description("Print types/names of table columns for specified layer instead of info")
	@In
	public boolean $$cFLAG = false;

	@Description("Print map region only")
	@In
	public boolean $$gFLAG = false;

	@Description("Print map title only")
	@In
	public boolean $$mFLAG = false;

	@Description("Print topology information only")
	@In
	public boolean $$tFLAG = false;

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
