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

@Description("Calculates the volume of data \"clumps\", and (optionally) produces a GRASS vector points map containing the calculated centroids of these clumps.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__volume")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__volume {

	@UI("infile,grassfile")
	@Description("Existing raster map representing data that will be summed within clumps")
	@In
	public String $$dataPARAMETER;

	@UI("infile,grassfile")
	@Description("Existing raster map, preferably the output of r.clump (optional)")
	@In
	public String $$clumpPARAMETER;

	@UI("outfile,grassfile")
	@Description("Vector points map to contain clump centroids (optional)")
	@In
	public String $$centroidsPARAMETER;

	@Description("Generate unformatted report")
	@In
	public boolean $$fFLAG = false;

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
