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

@Description("Displays the boundary of each r.le patch and shows how the boundary is traced, displays the attribute, size, perimeter and shape indices for each patch and saves the data in an output file.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__le__trace")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__le__trace {

	@UI("infile")
	@Description("Raster map to be analyzed")
	@In
	public String $$mapPARAMETER;

	@Description("Name of output file to store patch data (optional)")
	@In
	public String $$outPARAMETER;

	@Description("Include sampling area boundary as perimeter")
	@In
	public boolean $$pFLAG = false;

	@Description("Use 4 neighbor tracing instead of 8 neighbor")
	@In
	public boolean $$tFLAG = false;

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
