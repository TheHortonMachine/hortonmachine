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

@Description("Calculate error matrix and kappa parameter for accuracy assessment of classification result.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__kappa")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__kappa {

	@UI("infile,grassfile")
	@Description("Name of raster map containing classification result")
	@In
	public String $$classificationPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of raster map containing reference classes")
	@In
	public String $$referencePARAMETER;

	@Description("Name for output file containing error matrix and kappa (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Title for error matrix and kappa (optional)")
	@In
	public String $$titlePARAMETER = "ACCURACY ASSESSMENT";

	@Description("132 columns (default: 80)")
	@In
	public boolean $$wFLAG = false;

	@Description("Quiet")
	@In
	public boolean $$qFLAG = false;

	@Description("No header in the report")
	@In
	public boolean $$hFLAG = false;

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
