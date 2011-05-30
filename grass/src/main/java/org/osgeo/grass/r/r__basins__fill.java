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

@Description("Generates a raster map layer showing watershed subbasins.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__basins__fill")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__basins__fill {

	@UI("infile")
	@Description("Number of passes through the dataset")
	@In
	public String $$numberPARAMETER;

	@UI("infile")
	@Description("Coded stream network file name")
	@In
	public String $$c_mapPARAMETER;

	@UI("infile")
	@Description("Thinned ridge network file name")
	@In
	public String $$t_mapPARAMETER;

	@UI("outfile")
	@Description("Name for the resultant watershed partition file")
	@In
	public String $$resultPARAMETER;

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
