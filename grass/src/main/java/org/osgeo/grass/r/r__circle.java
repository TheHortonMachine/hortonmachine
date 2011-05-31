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

@Description("Creates a raster map containing concentric rings around a given point.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass Raster Modules")
@Name("r__circle")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__circle {

	@UI("outfile,grassfile")
	@Description("Name for output raster map")
	@In
	public String $$outputPARAMETER;

	@Description("The coordinate of the center (east,north)")
	@In
	public String $$coordinatePARAMETER;

	@Description("Minimum radius for ring/circle map (in meters) (optional)")
	@In
	public String $$minPARAMETER;

	@Description("Maximum radius for ring/circle map (in meters) (optional)")
	@In
	public String $$maxPARAMETER;

	@Description("Data value multiplier (optional)")
	@In
	public String $$multPARAMETER;

	@Description("Generate binary raster map")
	@In
	public boolean $$bFLAG = false;

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
