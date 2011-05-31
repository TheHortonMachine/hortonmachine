package org.osgeo.grass.r;

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

@Description("Create a MASK for limiting raster operation")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, mask")
@Label("Grass Raster Modules")
@Name("r__mask")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__mask {

	@UI("infile,grassfile")
	@Description("Raster map to use as MASK (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Category values to use for MASK (format: 1 2 3 thru 7 *) (optional)")
	@In
	public String $$maskcatsPARAMETER = "*";

	@Description("Create inverse MASK from specified 'maskcats' list")
	@In
	public boolean $$iFLAG = false;

	@Description("Overwrite existing MASK")
	@In
	public boolean $$oFLAG = false;

	@Description("Remove existing MASK (overrides other options)")
	@In
	public boolean $$rFLAG = false;

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
