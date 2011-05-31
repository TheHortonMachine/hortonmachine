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

@Description("Displays spectral response at user specified locations in group or images.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("imagery, raster, multispectral")
@Label("Grass/Imagery Modules")
@Name("i__spectral")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class i__spectral {

	@UI("infile,grassfile")
	@Description("Group input (optional)")
	@In
	public String $$groupPARAMETER;

	@UI("infile,grassfile")
	@Description("Raster input maps (optional)")
	@In
	public String $$rasterPARAMETER;

	@Description("Write output to PNG image (optional)")
	@In
	public String $$outputPARAMETER;

	@Description("Use image list and not group")
	@In
	public boolean $$iFLAG = false;

	@Description("Select multiple points")
	@In
	public boolean $$mFLAG = false;

	@Description("Label with coordinates instead of numbering")
	@In
	public boolean $$cFLAG = false;

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
