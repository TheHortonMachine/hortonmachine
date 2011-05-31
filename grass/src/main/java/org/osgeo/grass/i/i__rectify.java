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

@Description("Rectifies an image by computing a coordinate transformation for each pixel in the image based on the control points.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery, rectify")
@Label("Grass/Imagery Modules")
@Name("i__rectify")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__rectify {

	@UI("infile,grassfile")
	@Description("Name of input imagery group")
	@In
	public String $$groupPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input raster map(s) (optional)")
	@In
	public String $$inputPARAMETER;

	@Description("Output raster map(s) suffix")
	@In
	public String $$extensionPARAMETER;

	@Description("Rectification polynom order (1-3)")
	@In
	public String $$orderPARAMETER;

	@Description("Use current region settings in target location (def.=calculate smallest area)")
	@In
	public boolean $$cFLAG = false;

	@Description("Rectify all raster maps in group")
	@In
	public boolean $$aFLAG = false;

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
