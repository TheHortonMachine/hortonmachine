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

@Description("Principal components analysis (pca) program for image processing.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery, image transformation, PCA")
@Label("Grass/Imagery Modules")
@Name("i__pca")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__pca {

	@UI("infile,grassfile")
	@Description("Name of two or more input raster maps")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("A numerical suffix will be added for each component map")
	@In
	public String $$outputPARAMETER;

	@Description("Rescaling range for output maps (for no rescaling use 0,0) (optional)")
	@In
	public String $$rescalePARAMETER = "0,255";

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
