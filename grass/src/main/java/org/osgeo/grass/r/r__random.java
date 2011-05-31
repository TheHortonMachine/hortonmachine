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

@Description("Creates a raster map layer and vector point map containing randomly located points.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__random")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__random {

	@UI("infile,grassfile")
	@Description("Name of input raster map")
	@In
	public String $$inputPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of cover raster map (optional)")
	@In
	public String $$coverPARAMETER;

	@Description("The number of points to allocate")
	@In
	public String $$nPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output raster map (optional)")
	@In
	public String $$raster_outputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for output vector map (optional)")
	@In
	public String $$vector_outputPARAMETER;

	@Description("Generate points also for NULL category")
	@In
	public boolean $$zFLAG = false;

	@Description("Report information about input raster and exit")
	@In
	public boolean $$iFLAG = false;

	@Description("Generate vector points as 3D points")
	@In
	public boolean $$dFLAG = false;

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
