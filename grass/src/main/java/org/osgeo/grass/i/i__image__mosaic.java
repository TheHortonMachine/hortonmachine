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

@Description("Mosaics up to 4 images and extends colormap; creates map *.mosaic")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, imagery, mosaicking")
@Label("Grass Imagery Modules")
@Name("i__image__mosaic")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__image__mosaic {

	@UI("infile,grassfile")
	@Description("1st map for mosaic (top of image stack).")
	@In
	public String $$image1PARAMETER;

	@UI("infile,grassfile")
	@Description("2nd map for mosaic.")
	@In
	public String $$image2PARAMETER;

	@UI("infile,grassfile")
	@Description("3rd map for mosaic. (optional)")
	@In
	public String $$image3PARAMETER;

	@UI("infile,grassfile")
	@Description("4th map for mosaic. (optional)")
	@In
	public String $$image4PARAMETER;

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
