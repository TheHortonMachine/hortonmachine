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

@Description("Outputs a raster map layer showing the anisotropic cumulative cost of moving between different geographic locations on an input elevation raster map layer whose cell category values represent elevation combined with an input raster map layer whose cell values represent friction cost.")
@Author(name = "Grass Developers Community", contact = "http://grass.osgeo.org")
@Keywords("raster")
@Label("Grass/Raster Modules")
@Name("r__walk")
@Status(Status.CERTIFIED)
@License("General Public License Version >=2)")
public class r__walk {

	@UI("infile,grassfile")
	@Description("Name of elevation input raster map")
	@In
	public String $$elevationPARAMETER;

	@UI("infile,grassfile")
	@Description("Name of input raster map containing friction costs")
	@In
	public String $$frictionPARAMETER;

	@UI("outfile,grassfile")
	@Description("Name of raster map to contain results")
	@In
	public String $$outputPARAMETER;

	@UI("infile,grassfile")
	@Description("Starting points vector map (optional)")
	@In
	public String $$start_pointsPARAMETER;

	@UI("infile,grassfile")
	@Description("Stop points vector map (optional)")
	@In
	public String $$stop_pointsPARAMETER;

	@Description("The map E and N grid coordinates of a starting point (E,N) (optional)")
	@In
	public String $$coordinatePARAMETER;

	@Description("The map E and N grid coordinates of a stopping point (E,N) (optional)")
	@In
	public String $$stop_coordinatePARAMETER;

	@Description("An optional maximum cumulative cost (optional)")
	@In
	public String $$max_costPARAMETER = "0";

	@Description("Cost assigned to null cells. By default, null cells are excluded (optional)")
	@In
	public String $$null_costPARAMETER;

	@Description("Percent of map to keep in memory (optional)")
	@In
	public String $$percent_memoryPARAMETER = "100";

	@Description("Number of the segment to create (segment library) (optional)")
	@In
	public String $$nsegPARAMETER = "4";

	@Description("Coefficients for walking energy formula parameters a,b,c,d (optional)")
	@In
	public String $$walk_coeffPARAMETER = "0.72,6.0,1.9998,-1.9998";

	@Description("Lambda coefficients for combining walking energy and friction cost (optional)")
	@In
	public String $$lambdaPARAMETER = "1.0";

	@Description("Slope factor determines travel energy cost per height step (optional)")
	@In
	public String $$slope_factorPARAMETER = "-0.2125";

	@Description("Use the 'Knight's move'; slower, but more accurate")
	@In
	public boolean $$kFLAG = false;

	@Description("Keep null values in output map")
	@In
	public boolean $$nFLAG = false;

	@Description("Start with values in raster map")
	@In
	public boolean $$rFLAG = false;

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
