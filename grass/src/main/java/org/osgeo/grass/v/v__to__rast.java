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

@Description("Converts a binary GRASS vector map into a GRASS raster map .")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("vector, raster, conversion")
@Label("Grass/Vector Modules")
@Name("v__to__rast")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class v__to__rast {

	@UI("infile,grassfile")
	@Description("Name of input vector map")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Source of raster values (optional)")
	@In
	public String $$usePARAMETER = "attr";

	@Description("Feature type (optional)")
	@In
	public String $$typePARAMETER = "point,line,area";

	@Description("A single vector map can be connected to multiple database tables. This number determines which table to use. (optional)")
	@In
	public String $$layerPARAMETER = "1";

	@Description("Name of column for attr parameter (data type must be numeric) (optional)")
	@In
	public String $$columnPARAMETER;

	@Description("Raster value (optional)")
	@In
	public String $$valuePARAMETER = "1";

	@Description("Number of rows to hold in memory (optional)")
	@In
	public String $$rowsPARAMETER = "4096";

	@Description("Name of color definition column (with RRR:GGG:BBB entries) (optional)")
	@In
	public String $$rgbcolumnPARAMETER;

	@Description("Name of column used as raster category labels (optional)")
	@In
	public String $$labelcolumnPARAMETER;

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
