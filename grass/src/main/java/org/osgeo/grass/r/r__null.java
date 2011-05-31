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

@Description("Manages NULL-values of given raster map.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster, null data")
@Label("Grass Raster Modules")
@Name("r__null")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__null {

	@UI("infile,grassfile")
	@Description("Raster map for which to edit null file")
	@In
	public String $$mapPARAMETER;

	@Description("List of cell values to be set to NULL (optional)")
	@In
	public String $$setnullPARAMETER;

	@Description("The value to replace the null value by (optional)")
	@In
	public String $$nullPARAMETER;

	@Description("Only do the work if the map is floating-point")
	@In
	public boolean $$fFLAG = false;

	@Description("Only do the work if the map is integer")
	@In
	public boolean $$iFLAG = false;

	@Description("Only do the work if the map doesn't have a NULL-value bitmap file")
	@In
	public boolean $$nFLAG = false;

	@Description("Create NULL-value bitmap file validating all data cells")
	@In
	public boolean $$cFLAG = false;

	@Description("Remove NULL-value bitmap file")
	@In
	public boolean $$rFLAG = false;

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
