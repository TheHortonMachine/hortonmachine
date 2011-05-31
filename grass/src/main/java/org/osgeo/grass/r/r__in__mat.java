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

@Description("Imports a binary MAT-File(v4) to a GRASS raster.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__in__mat")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__in__mat {

	@Description("Name of an existing MAT-File(v4)")
	@In
	public String $$inputPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name for the output raster map (override) (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Verbose mode")
	@In
	public boolean $$vFLAG = false;

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
