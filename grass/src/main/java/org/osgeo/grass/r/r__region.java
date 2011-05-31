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

@Description("Sets the boundary definitions for a raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__region")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__region {

	@UI("infile,grassfile")
	@Description("Raster map to change")
	@In
	public String $$mapPARAMETER;

	@UI("infile,grassfile")
	@Description("Set region from named region (optional)")
	@In
	public String $$regionPARAMETER;

	@UI("infile,grassfile")
	@Description("Set region to match this raster map (optional)")
	@In
	public String $$rasterPARAMETER;

	@UI("infile,grassfile")
	@Description("Set region to match this vector map (optional)")
	@In
	public String $$vectorPARAMETER;

	@UI("infile,grassfile")
	@Description("Set region to match this 3dview file (optional)")
	@In
	public String $$3dviewPARAMETER;

	@Description("Value for the northern edge (optional)")
	@In
	public String $$nPARAMETER;

	@Description("Value for the southern edge (optional)")
	@In
	public String $$sPARAMETER;

	@Description("Value for the eastern edge (optional)")
	@In
	public String $$ePARAMETER;

	@Description("Value for the western edge (optional)")
	@In
	public String $$wPARAMETER;

	@UI("infile,grassfile")
	@Description("Raster map to align to (optional)")
	@In
	public String $$alignPARAMETER;

	@Description("Set from current region")
	@In
	public boolean $$cFLAG = false;

	@Description("Set from default region")
	@In
	public boolean $$dFLAG = false;

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
