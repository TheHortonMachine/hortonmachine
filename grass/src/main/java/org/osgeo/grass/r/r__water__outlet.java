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

@Description("Watershed basin creation program.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__water__outlet")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__water__outlet {

	@UI("infile")
	@Description("Name of input raster map")
	@In
	public String $$drainagePARAMETER;

	@UI("outfile")
	@Description("Name of raster map to contain results")
	@In
	public String $$basinPARAMETER;

	@Description("The map E grid coordinates")
	@In
	public String $$eastingPARAMETER;

	@Description("The map N grid coordinates")
	@In
	public String $$northingPARAMETER;

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
