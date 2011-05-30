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

@Description("Calculates category or object oriented statistics.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, statistics")
@Label("Grass Raster Modules")
@Name("r__statistics")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__statistics {

	@UI("infile")
	@Description("Name of base raster map")
	@In
	public String $$basePARAMETER;

	@UI("infile")
	@Description("Name of cover raster map")
	@In
	public String $$coverPARAMETER;

	@Description("Method of object-based statistic")
	@In
	public String $$methodPARAMETER;

	@UI("outfile")
	@Description("Resultant raster map (not used with 'distribution') (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Cover values extracted from the category labels of the cover map")
	@In
	public boolean $$cFLAG = false;

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
