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

@Description("Calculates Optimum-Index-Factor table for LANDSAT TM bands 1-5, & 7")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, imagery, statistics")
@Label("Grass/Imagery Modules")
@Name("i__oif")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__oif {

	@UI("infile,grassfile")
	@Description("LANDSAT TM band 1.")
	@In
	public String $$image1PARAMETER;

	@UI("infile,grassfile")
	@Description("LANDSAT TM band 2.")
	@In
	public String $$image2PARAMETER;

	@UI("infile,grassfile")
	@Description("LANDSAT TM band 3.")
	@In
	public String $$image3PARAMETER;

	@UI("infile,grassfile")
	@Description("LANDSAT TM band 4.")
	@In
	public String $$image4PARAMETER;

	@UI("infile,grassfile")
	@Description("LANDSAT TM band 5.")
	@In
	public String $$image5PARAMETER;

	@UI("infile,grassfile")
	@Description("LANDSAT TM band 7.")
	@In
	public String $$image7PARAMETER;

	@Description("Print in shell script style")
	@In
	public boolean $$gFLAG = false;

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
