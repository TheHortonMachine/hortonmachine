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

@Description("Surface generation program from rasterized contours.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__surf__contour")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__surf__contour {

	@UI("infile")
	@Description("Name of existing raster map containing contours")
	@In
	public String $$inputPARAMETER;

	@UI("outfile")
	@Description("Output elevation raster map")
	@In
	public String $$outputPARAMETER;

	@Description("Unused; retained for compatibility purposes, will be removed in future")
	@In
	public boolean $$fFLAG = false;

	@Description("Invoke slow, but memory frugal operation (generally not needed, will be removed in future)")
	@In
	public boolean $$sFLAG = false;

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
