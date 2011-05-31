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

@Description("Canonical components analysis (cca) program for image processing.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery")
@Label("Grass Imagery Modules")
@Name("i__cca")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__cca {

	@UI("infile,grassfile")
	@Description("Name of input imagery group")
	@In
	public String $$groupPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input imagery subgroup")
	@In
	public String $$subgroupPARAMETER;

	@Description("Ascii file containing spectral signatures")
	@In
	public String $$signaturePARAMETER;

	@UI("outfile,grassfile")
	@Description("Output raster map prefix name")
	@In
	public String $$outputPARAMETER;

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
